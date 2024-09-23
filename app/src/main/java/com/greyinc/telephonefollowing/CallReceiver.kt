package com.greyinc.telephonefollowing

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import android.telephony.TelephonyManager
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

class CallReceiver : BroadcastReceiver() {

    private val database = FirebaseDatabase.getInstance("https://telephonefollow-default-rtdb.europe-west1.firebasedatabase.app")// Realtime Database'i başlat
    private var incomingCallsCount = 0 // Gelen çağrı sayısı
    private var outgoingCallsCount = 0 // Giden çağrı sayısı
    private var answeredCallsCount = 0 // Cevaplanan çağrı sayısı
    private var missedCallsCount = 0 // Cevaplanmayan çağrı sayısı

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Gelen çağrı
                Log.d("CallReceiver", "Gelen çağrı: $number")
                incomingCallsCount++ // Gelen çağrı sayısını artır
                missedCallsCount++ // Kaçırılan çağrı sayısını artır
                updateCallCount("incoming")
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Çağrı aktif
                Log.d("CallReceiver", "Çağrı cevaplandı: $number")
                answeredCallsCount++ // Cevaplanan çağrı sayısını artır
                missedCallsCount-- // Eğer cevaplandıysa, kaçırılan çağrı sayısını azalt
                updateCallCount("answered")
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Çağrı sona erdi
                Log.d("CallReceiver", "Çağrı sona erdi")
                saveCallCountsToDatabase()
            }
        }
    }

    // Firebase'e çağrı sayılarını kaydet
    private fun saveCallCountsToDatabase() {
        val callCountRef = database.getReference("callCounts")

        callCountRef.setValue(mapOf(
            "incoming" to incomingCallsCount,
            "outgoing" to outgoingCallsCount,
            "answered" to answeredCallsCount,
            "missed" to missedCallsCount
        ))
    }

    // Çağrı sayılarını almak için fonksiyon
    private fun getCallCounts(contentResolver: ContentResolver): Pair<Int, Int> {
        var incomingCalls = 0
        var outgoingCalls = 0

        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null, // Seçilecek sütunlar
            null, // Filtreleme
            null, // Filtreleme parametreleri
            CallLog.Calls.DATE + " DESC" // Son çağrıdan itibaren sıralama
        )

        cursor?.use {
            val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
            while (it.moveToNext()) {
                val callType = it.getInt(typeIndex)
                when (callType) {
                    CallLog.Calls.INCOMING_TYPE -> incomingCalls++
                    CallLog.Calls.OUTGOING_TYPE -> outgoingCalls++
                }
            }
        }
        return Pair(incomingCalls, outgoingCalls)
    }

    // Firebase'e çağrı sayısını güncelle
    private fun updateCallCount(type: String) {
        val callCountRef = database.getReference("callCounts")

        callCountRef.child(type).runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                var count = mutableData.getValue(Int::class.java) ?: 0
                count++
                mutableData.value = count
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?,
            ) {
                // İşlem tamamlandığında yapılacaklar
            }
        })
    }
}
