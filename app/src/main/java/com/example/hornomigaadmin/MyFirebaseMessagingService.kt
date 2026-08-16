package com.example.hornomigaadmin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val titulo = message.notification?.title ?: "Nuevo pedido"
        val cuerpo = message.notification?.body ?: "Llegó una reserva nueva"
        mostrarNotificacion(titulo, cuerpo)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // No hace falta hacer nada aquí porque usamos "topics" (suscripción
        // por tema) en vez de tokens individuales. Se deja el override por
        // si en el futuro se quiere mandar notificaciones a un admin puntual.
    }

    private fun mostrarNotificacion(titulo: String, cuerpo: String) {
        val channelId = "pedidos_nuevos"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                channelId,
                "Pedidos nuevos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos cuando llega un pedido nuevo desde el sitio web"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, ReservasActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        getSystemService(NotificationManager::class.java).notify(notificationId, notif)
    }
}