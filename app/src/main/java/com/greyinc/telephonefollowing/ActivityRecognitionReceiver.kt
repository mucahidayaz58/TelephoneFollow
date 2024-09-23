package com.greyinc.telephonefollowing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ActivityRecognitionReceiver : BroadcastReceiver() {

    private val database: DatabaseReference = FirebaseDatabase.getInstance("https://telephonefollow-default-rtdb.europe-west1.firebasedatabase.app").reference

    override fun onReceive(context: Context?, intent: Intent?) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent!!)
            val activity = result!!.mostProbableActivity
            val activityType = activity.type
            val confidence = activity.confidence

            // Aktivite adını belirle
            val activityName = when (activityType) {
                DetectedActivity.IN_VEHICLE -> "In Vehicle"
                DetectedActivity.ON_FOOT -> "On Foot"
                DetectedActivity.STILL -> "Still"
                DetectedActivity.RUNNING -> "Running"
                DetectedActivity.WALKING -> "Walking"
                DetectedActivity.TILTING -> "Tilting"
                DetectedActivity.UNKNOWN -> "Unknown"
                else -> "Unknown"
            }

            // Firebase'e kaydet
            updateActivityInFirebase(activityName, confidence)

            // MyAccessibilityService'e mevcut aktiviteyi güncelle
            (context as? MyAccessibilityService)?.updateCurrentActivity(activityName)

            Log.d("ActivityRecognition", "$activityName ($confidence%)")
        }
    }

    private fun updateActivityInFirebase(activityName: String, confidence: Int) {
        // Firebase'de aktivite verisini kaydet
        val activityData = hashMapOf(
            "confidence" to confidence
        )

        database.child("activity_data").child(activityName).setValue(activityData)
            .addOnSuccessListener {
                Log.d("ActivityRecognition", "Firebase'e kaydedildi: $activityName ($confidence%)")
            }
            .addOnFailureListener { exception ->
                Log.e("ActivityRecognition", "Firebase'e kaydedilemedi: ${exception.message}")
            }
    }
}
