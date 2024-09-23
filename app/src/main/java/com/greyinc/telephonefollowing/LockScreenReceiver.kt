package com.greyinc.telephonefollowing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LockScreenReceiver : BroadcastReceiver() {

    private lateinit var database: DatabaseReference

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_USER_PRESENT -> {
                Log.d("LockScreenReceiver", "Cihazın kilidi açıldı")

                database = FirebaseDatabase.getInstance("https://telephonefollow-default-rtdb.europe-west1.firebasedatabase.app").reference
                val unlockCountRef = database.child("unlockCounts").child("count")

                // Mevcut açılma sayısını al ve artır
                unlockCountRef.get().addOnSuccessListener { dataSnapshot ->
                    val currentCount = dataSnapshot.getValue(Int::class.java) ?: 0
                    val newCount = currentCount + 1

                    // Yeni sayıyı Firebase'e kaydet
                    unlockCountRef.setValue(newCount)
                        .addOnSuccessListener {
                            Log.d("LockScreenReceiver", "Yeni açılma sayısı: $newCount")
                        }
                        .addOnFailureListener { e ->
                            Log.e("LockScreenReceiver", "Kaydedilemedi", e)
                        }
                }.addOnFailureListener { e ->
                    Log.e("LockScreenReceiver", "Veri alınamadı", e)
                }

                // UnlockTrackingService'i başlatma
                startUnlockTrackingService(context)
            }
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("LockScreenReceiver", "Cihaz kilitlendi")

                // UnlockTrackingService'i başlatma (ekran kapandığında)
                startUnlockTrackingService(context)
            }
        }
    }

    // Servisi başlatan fonksiyon
    private fun startUnlockTrackingService(context: Context) {
        val serviceIntent = Intent(context, UnlockTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        Log.d("LockScreenReceiver", "UnlockTrackingService başlatıldı.")
    }
}
