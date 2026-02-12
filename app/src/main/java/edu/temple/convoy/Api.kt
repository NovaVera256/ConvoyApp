package edu.temple.convoy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Api {
    private suspend fun postForm(urlStr: String, fields: Map<String, String>): JSONObject {
    return withContext(Dispatchers.IO) {

        val postData = fields.map { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }.joinToString("&")

        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }

        BufferedWriter(OutputStreamWriter(conn.outputStream)).use { writer ->
            writer.write(postData)
            writer.flush()
        }

        val response = conn.inputStream.bufferedReader().use { it.readText() }
        JSONObject(response)
    }
}

    suspend fun register(username: String, first: String, last: String, password: String): JSONObject {
        return postForm("https://kamorris.com/lab/convoy/account.php",
            mapOf(
                "action" to "REGISTER",
                "username" to username,
                "firstname" to first,
                "lastname" to last,
                "password" to password
            )
        )
    }

    suspend fun login(username: String, password: String): JSONObject {
        return postForm(
            "https://kamorris.com/lab/convoy/account.php",
            mapOf(
                "action" to "LOGIN",
                "username" to username,
                "password" to password
            )
        )
    }

    suspend fun logout(username: String, sessionKey: String): JSONObject {
        return postForm(
            "https://kamorris.com/lab/convoy/account.php",
            mapOf(
                "action" to "LOGOUT",
                "username" to username,
                "session_key" to sessionKey
            )
        )
    }

    suspend fun createConvoy(username: String, sessionKey: String): JSONObject {
        return postForm(
            "https://kamorris.com/lab/convoy/convoy.php",
            mapOf(
                "action" to "CREATE",
                "username" to username,
                "session_key" to sessionKey
            )
        )
    }

    suspend fun endConvoy(username: String, sessionKey: String, convoyId: String): JSONObject {
        return postForm(
            "https://kamorris.com/lab/convoy/convoy.php",
            mapOf(
                "action" to "END",
                "username" to username,
                "session_key" to sessionKey,
                "convoy_id" to convoyId
            )
        )
    }

    suspend fun queryConvoy(username: String, sessionKey: String): JSONObject {
        return postForm(
            "https://kamorris.com/lab/convoy/convoy.php",
            mapOf(
                "action" to "QUERY",
                "username" to username,
                "session_key" to sessionKey
            )
        )
    }
}