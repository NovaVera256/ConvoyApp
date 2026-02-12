package edu.temple.convoy

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class AuthActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionStore.isLoggedIn(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            MaterialTheme {
                AuthScreen(
                    onLoginSuccess = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthScreen(onLoginSuccess: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf("LOGIN") } // "LOGIN" or "REGISTER"

    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    var regFirst by remember { mutableStateOf("") }
    var regLast by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { mode = "LOGIN" }, modifier = Modifier.weight(1f)) { Text("Log in") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { mode = "REGISTER" }, modifier = Modifier.weight(1f)) { Text("Create account") }
        }

        Spacer(Modifier.height(16.dp))

        if (mode == "LOGIN") {

            OutlinedTextField(
                value = loginUsername,
                onValueChange = { loginUsername = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username") }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = loginPassword,
                onValueChange = { loginPassword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val u = loginUsername.trim()
                    val p = loginPassword.trim()
                    if (u.isEmpty() || p.isEmpty()) {
                        Toast.makeText(ctx, "Missing fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        try {
                            val json = Api.login(u, p)
                            if (json.optString("status") == "SUCCESS") {
                                val sessionKey = json.optString("session_key")
                                SessionStore.saveUser(ctx, u, null, null)
                                SessionStore.saveSessionKey(ctx, sessionKey)
                                onLoginSuccess()
                            } else {
                                Toast.makeText(ctx, json.optString("message"), Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: Exception) {
                            Toast.makeText(ctx, "Network error", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Log in") }

        } else {

            OutlinedTextField(
                value = regFirst,
                onValueChange = { regFirst = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("First name") }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = regLast,
                onValueChange = { regLast = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Last name") }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = regUsername,
                onValueChange = { regUsername = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username") }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = regPassword,
                onValueChange = { regPassword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val f = regFirst.trim()
                    val l = regLast.trim()
                    val u = regUsername.trim()
                    val p = regPassword.trim()

                    if (f.isEmpty() || l.isEmpty() || u.isEmpty() || p.isEmpty()) {
                        Toast.makeText(ctx, "Missing fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        try {
                            val json = Api.register(u, f, l, p)
                            if (json.optString("status") == "SUCCESS") {
                                val sessionKey = json.optString("session_key")
                                SessionStore.saveUser(ctx, u, f, l)
                                SessionStore.saveSessionKey(ctx, sessionKey)
                                onLoginSuccess()
                            } else {
                                Toast.makeText(ctx, json.optString("message"), Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: Exception) {
                            Toast.makeText(ctx, "Network error", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create account") }
        }
    }
}
