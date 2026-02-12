package edu.temple.convoy

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val currentLatLng = mutableStateOf(LatLng(39.9526, -75.1652))
    private val convoyIdState = mutableStateOf<String?>(null)

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val lat = intent.getDoubleExtra(LocationForegroundService.EXTRA_LAT, Double.NaN)
            val lng = intent.getDoubleExtra(LocationForegroundService.EXTRA_LNG, Double.NaN)
            if (!lat.isNaN() && !lng.isNaN()) {
                currentLatLng.value = LatLng(lat, lng)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SessionStore.isLoggedIn(this)) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        registerReceiver(
            locationReceiver,
            IntentFilter(LocationForegroundService.ACTION_LOCATION),
            Context.RECEIVER_NOT_EXPORTED
        )

        requestNeededPermissions()

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(locationReceiver)
        super.onDestroy()
    }

    private fun requestNeededPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        permLauncher.launch(perms.toTypedArray())
    }

    private fun startLocationService() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, LocationForegroundService::class.java))
    }

    private fun stopLocationService() {
        stopService(Intent(this, LocationForegroundService::class.java))
    }

    private fun logout() {
        val username = SessionStore.getUsername(this)
        val sessionKey = SessionStore.getSessionKey(this)

        stopLocationService()
        convoyIdState.value = null

        if (username == null || sessionKey == null) {
            SessionStore.clearAll(this)
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            try { Api.logout(username, sessionKey) } catch (_: Exception) { }
            SessionStore.clearAll(this@MainActivity)
            startActivity(Intent(this@MainActivity, AuthActivity::class.java))
            finish()
        }
    }

    @Composable
    private fun MainScreen() {
        val ctx = LocalContext.current
        val scope = rememberCoroutineScope()

        var showEndConfirm by remember { mutableStateOf(false) }
        var showJoinDialog by remember { mutableStateOf(false) }
        var showLeaveConfirm by remember { mutableStateOf(false) }
        var joinInput by remember { mutableStateOf("") }

        val convoyId = convoyIdState.value
        val latLng = currentLatLng.value

        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

            Text(text = if (convoyId == null) "Convoy ID: (none)" else "Convoy ID: $convoyId")

            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                BasicMapView(latLng = latLng, modifier = Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {

                Button(
                    onClick = {
                        val username = SessionStore.getUsername(ctx) ?: return@Button
                        val sessionKey = SessionStore.getSessionKey(ctx) ?: return@Button

                        scope.launch {
                            try {
                                val json = Api.createConvoy(username, sessionKey)
                                if (json.optString("status") == "SUCCESS") {
                                    convoyIdState.value = json.optString("convoy_id")
                                    startLocationService()
                                } else {
                                    Toast.makeText(ctx, json.optString("message"), Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {
                                Toast.makeText(ctx, "Network error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Start") }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = { showJoinDialog = true },
                    modifier = Modifier.weight(1f)
                ) { Text("Join") }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = { showLeaveConfirm = true },
                    modifier = Modifier.weight(1f)
                ) { Text("Leave") }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showEndConfirm = true },
                enabled = convoyId != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("End Convoy") }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { logout() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Logout") }
        }

        if (showEndConfirm) {
            AlertDialog(
                onDismissRequest = { showEndConfirm = false },
                confirmButton = {
                    TextButton(onClick = {
                        showEndConfirm = false
                        val username = SessionStore.getUsername(ctx) ?: return@TextButton
                        val sessionKey = SessionStore.getSessionKey(ctx) ?: return@TextButton
                        val id = convoyIdState.value ?: return@TextButton

                        scope.launch {
                            try {
                                val json = Api.endConvoy(username, sessionKey, id)
                                if (json.optString("status") == "SUCCESS") {
                                    convoyIdState.value = null
                                    stopLocationService()
                                } else {
                                    Toast.makeText(ctx, json.optString("message"), Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {
                                Toast.makeText(ctx, "Network error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Text("End") }
                },
                dismissButton = {
                    TextButton(onClick = { showEndConfirm = false }) { Text("Cancel") }
                },
                title = { Text("End Convoy") },
                text = { Text("End the convoy?") }
            )
        }

        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = { showJoinDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        showJoinDialog = false
                        Toast.makeText(ctx, "Join Convoy incomplete", Toast.LENGTH_SHORT).show()
                    }) { Text("Join") }
                },
                dismissButton = {
                    TextButton(onClick = { showJoinDialog = false }) { Text("Cancel") }
                },
                title = { Text("Join Convoy") },
                text = {
                    OutlinedTextField(
                        value = joinInput,
                        onValueChange = { joinInput = it },
                        label = { Text("Convoy ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }

        if (showLeaveConfirm) {
            AlertDialog(
                onDismissRequest = { showLeaveConfirm = false },
                confirmButton = {
                    TextButton(onClick = {
                        showLeaveConfirm = false
                        Toast.makeText(ctx, "Leave Convoy incomplete", Toast.LENGTH_SHORT).show()
                    }) { Text("Leave") }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") }
                },
                title = { Text("Leave Convoy") },
                text = { Text("Leave the convoy?") }
            )
        }
    }
}

@Composable
private fun BasicMapView(latLng: LatLng, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current

    val mapView = remember { MapView(ctx).apply { onCreate(Bundle()) } }
    var marker by remember { mutableStateOf<Marker?>(null) }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )

    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            map.uiSettings.isZoomControlsEnabled = true
            marker = map.addMarker(MarkerOptions().position(latLng).title("You"))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        }
    }

    LaunchedEffect(latLng) {
        mapView.getMapAsync { map ->
            if (marker == null) {
                marker = map.addMarker(MarkerOptions().position(latLng).title("You"))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            } else {
                marker?.position = latLng
            }
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }
}

