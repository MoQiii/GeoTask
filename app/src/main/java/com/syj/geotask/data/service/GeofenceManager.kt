package com.syj.geotask.data.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.syj.geotask.data.receiver.GeofenceBroadcastReceiver
import com.syj.geotask.domain.model.Task
import kotlinx.coroutines.tasks.await

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    companion object {
        private const val DEFAULT_GEOFENCE_RADIUS_IN_METERS = 200f
        private const val MIN_GEOFENCE_RADIUS_IN_METERS = 100f
        private const val MAX_GEOFENCE_RADIUS_IN_METERS = 500f
        private const val GEOFENCE_EXPIRATION = Geofence.NEVER_EXPIRE
        private const val GEOFENCE_TRANSITION = Geofence.GEOFENCE_TRANSITION_ENTER
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun addGeofenceForTask(task: Task): Boolean {
        if (task.latitude == null || task.longitude == null || task.location == null) {
            return false
        }

        return try {
            // 使用任务中配置的地理围栏半径，确保在合理范围内
            val radius = task.geofenceRadius.coerceIn(MIN_GEOFENCE_RADIUS_IN_METERS, MAX_GEOFENCE_RADIUS_IN_METERS)
            
            val geofence = Geofence.Builder()
                .setRequestId(task.id.toString())
                .setCircularRegion(
                    task.latitude,
                    task.longitude,
                    radius
                )
                .setExpirationDuration(GEOFENCE_EXPIRATION)
                .setTransitionTypes(GEOFENCE_TRANSITION)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            geofencingClient.addGeofences(geofencingRequest, pendingIntent).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeGeofenceForTask(taskId: Long): Boolean {
        return try {
            geofencingClient.removeGeofences(listOf(taskId.toString())).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeAllGeofences(): Boolean {
        return try {
            val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            geofencingClient.removeGeofences(pendingIntent).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
