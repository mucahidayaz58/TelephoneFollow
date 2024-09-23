package com.greyinc.telephonefollowing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class UnlockTrackingService : Service() {

    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "UnlockTrackingServiceChannel"

    override fun onCreate() {
        super.onCreate()
        Log.d("UnlockTrackingService", "Servis başlatıldı.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground servisi başlat
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Servisin asıl işlevini burada yapabilirsiniz.
        Log.d("UnlockTrackingService", "Servis çalışıyor...")

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Bound servis değil, null döndürüyoruz
        return null
    }

    // Bildirim (Notification) oluştur
    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Notification Channel oluştur (Android 8.0 ve sonrası için gerekli)
            val channelName = "Unlock Tracking Service"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(CHANNEL_ID, channelName, importance)

            // Notification Manager ile kanalı kaydet
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(notificationChannel)
        }

        // Bildirim (Notification) oluştur
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Unlock Tracking Service")
            .setContentText("Service is running...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Küçük simge
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("UnlockTrackingService", "Servis durduruldu.")
    }
}
