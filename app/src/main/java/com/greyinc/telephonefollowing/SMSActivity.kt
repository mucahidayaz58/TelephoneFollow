package com.greyinc.telephonefollowing

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SMSActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smsactivity)

        // RecyclerView'i bul
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        // Test için örnek veriler oluştur
        val callLogs = listOf(
            CallLogItem(phoneNumber = "+39 331 765 4321", smsCount = "15"),
            CallLogItem(phoneNumber = "+39 340 112 2334", smsCount = "20"),
            CallLogItem(phoneNumber = "+39 320 654 3210", smsCount = "10")
        )


        // RecyclerView ayarlarını yap
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = CallLogAdapter(callLogs)
    }
}
