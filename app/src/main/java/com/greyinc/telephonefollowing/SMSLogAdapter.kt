package com.greyinc.telephonefollowing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CallLogAdapter(private val callLogs: List<CallLogItem>) : RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder>() {

    class CallLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val phoneNumberTextView: TextView = itemView.findViewById(R.id.phone_number)
        val countMessageTextView: TextView = itemView.findViewById(R.id.message_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.send_message_details_recycler_view, parent, false)
        return CallLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallLogViewHolder, position: Int) {
        val callLog = callLogs[position]
        holder.phoneNumberTextView.text = callLog.phoneNumber
        holder.countMessageTextView.text = callLog.smsCount
    }

    override fun getItemCount(): Int {
        return callLogs.size
    }
}

data class CallLogItem(val phoneNumber: String, val smsCount: String)
