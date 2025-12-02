package com.example.markmyattendence.student.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.markmyattendence.student.AttendanceSession

class AttendanceSessionAdapter(private val sessions: List<AttendanceSession>) :
    RecyclerView.Adapter<AttendanceSessionAdapter.SessionViewHolder>() {

    // Simple view holder using a two-line list item layout
    class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSessionTime: TextView = view.findViewById(android.R.id.text1)
        val tvStatus: TextView = view.findViewById(android.R.id.text2)
        val cardView: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        // Use a standard Android layout for simplicity
        val view = LayoutInflater.from(parent.context).inflate(
            android.R.layout.simple_list_item_2,
            parent,
            false
        )
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        val context = holder.itemView.context

        if (session.documentId.isEmpty()) {
            // Placeholder for "No sessions" or error message
            holder.tvSessionTime.text = session.sessionTime
            holder.tvStatus.text = ""
            holder.tvSessionTime.setTextColor(context.resources.getColor(android.R.color.darker_gray, null))
            holder.tvStatus.visibility = View.GONE
            // Set background back to white/transparent
            holder.cardView.setBackgroundColor(context.resources.getColor(android.R.color.white, null))
            return
        }

        holder.tvSessionTime.text = "Session: ${session.sessionTime}"
        holder.tvStatus.visibility = View.VISIBLE


        if (session.isPresent) {
            holder.tvStatus.text = "PRESENT"
            holder.tvStatus.setTextColor(context.resources.getColor(android.R.color.holo_green_dark, null))

            holder.cardView.setBackgroundColor(0xFFE8F5E9.toInt()) // Light Green
        } else {
            holder.tvStatus.text = "ABSENT"
            holder.tvStatus.setTextColor(context.resources.getColor(android.R.color.holo_red_dark, null))

            holder.cardView.setBackgroundColor(0xFFFFEBEE.toInt()) // Light Red
        }
    }

    override fun getItemCount() = sessions.size
}