package com.example.markmyattendence.teacher.Adapter

// StudentAttendanceAdapter.kt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.markmyattendence.R
import com.example.markmyattendence.data.StudentAttendance
import org.w3c.dom.Text

class StudentAttendanceAdapter(private var studentList: List<StudentAttendance>) :
    RecyclerView.Adapter<StudentAttendanceAdapter.StudentViewHolder>() {

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView) // attendence_item

        val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)  // attendence_item
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.attendance_item, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = studentList[position]
        holder.nameTextView.text = student.studentName

        if (student.isPresent) {
            holder.statusTextView.text = "Present"
            holder.statusTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.green))
        } else {
            holder.statusTextView.text = "Absent"
            holder.statusTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.red))
        }

    }

    override fun getItemCount() = studentList.size

    fun updateList(newList: List<StudentAttendance>) {
        // Optional: Sort the list by name before displaying
        studentList = newList.sortedBy { it.studentName }
        notifyDataSetChanged()
    }




    fun getStudentList(): List<StudentAttendance> {
        return studentList
    }
}