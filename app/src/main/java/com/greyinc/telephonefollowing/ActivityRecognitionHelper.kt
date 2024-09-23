package com.greyinc.telephonefollowing

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient

class ActivityRecognitionHelper(private val context: Context) {

    private var activityRecognitionClient: ActivityRecognitionClient = ActivityRecognition.getClient(context)
    private var pendingIntent: PendingIntent = getPendingIntent()

    // Aktivite tanıma işlemini başlatma fonksiyonu
    fun startActivityRecognition() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("ActivityRecognition", "Permission not granted")
            return
        }

        // Aktivite güncellemelerini iste
        activityRecognitionClient.requestActivityUpdates(
            3000,  // Güncellemeler arasındaki süre (milisaniye cinsinden)
            pendingIntent
        ).addOnSuccessListener {
            Log.d("ActivityRecognition", "Activity updates started")
        }.addOnFailureListener {
            Log.e("ActivityRecognition", "Failed to start activity updates")
        }
    }

    // Aktivite tanıma işlemini durdurma fonksiyonu
    fun stopActivityRecognition() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("ActivityRecognition", "Permission not granted")
            return
        }

        activityRecognitionClient.removeActivityUpdates(pendingIntent)
            .addOnSuccessListener {
                Log.d("ActivityRecognition", "Activity updates stopped")
            }
            .addOnFailureListener {
                Log.e("ActivityRecognition", "Activity updates failed to stop")
            }
    }

    // PendingIntent oluşturan yardımcı fonksiyon
    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(context, ActivityRecognitionReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}
