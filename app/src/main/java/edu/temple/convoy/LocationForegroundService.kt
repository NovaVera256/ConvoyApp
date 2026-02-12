package edu.temple.convoy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

class LocationForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "convoy_location"
        const val NOTIF_ID = 1
        const val ACTION_LOCATION = "edu.temple.convoy.LOCATION_UPDATE"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
    }

    private lateinit var fused: FusedLocationProviderClient
    private var cb: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        startUpdates()
    }

    override fun onDestroy() {
        stopUpdates()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Convoy")
            .setContentText("Location active")
            .setOngoing(true).build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Convoy", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun startUpdates() {
        val fine = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            stopSelf()
            return
        }

        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateDistanceMeters(10f)
            .build()

        cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                sendBroadcast(Intent(ACTION_LOCATION).apply {
                    putExtra(EXTRA_LAT, loc.latitude)
                    putExtra(EXTRA_LNG, loc.longitude)
                }
                )
            }
        }

        fused.requestLocationUpdates(req, cb!!, mainLooper)
    }

    private fun stopUpdates() {
        cb?.let { fused.removeLocationUpdates(it) }
        cb = null
    }
}