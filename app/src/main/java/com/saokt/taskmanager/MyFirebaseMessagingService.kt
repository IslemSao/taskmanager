package com.saokt.taskmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.saokt.taskmanager.notification.FCMTokenManager
import com.saokt.taskmanager.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "MyFirebaseMessagingService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Received FCM message: ${remoteMessage.messageId}")

        // Handle FCM messages here.
        val title = remoteMessage.notification?.title ?: "Task Manager"
        val body = remoteMessage.notification?.body ?: "You have a new notification."

        // You can add custom handling based on message data
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: $data")
            // Handle custom data payload here if needed
        }

        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received: ${token.take(20)}...")

        // Update the FCM token using our token manager
        serviceScope.launch {
            try {
                val result = fcmTokenManager.refreshAndUpdateToken()
                result.onSuccess {
                    Log.d(TAG, "FCM token updated successfully via service")
                }.onFailure { exception ->
                    Log.e(TAG, "Failed to update FCM token via service", exception)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating FCM token via service", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Note: CoroutineScope doesn't have a direct cancel() method in this context
        // The scope will be cancelled when the service is destroyed
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "taskmanager_channel"
        val notificationId = System.currentTimeMillis().toInt()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open the app (Dashboard) when notification is tapped
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Manager Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
} 