package com.example.markmyattendence.teacher.Adatper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.markmyattendence.R
import com.example.markmyattendence.data.ClassItem

class ClassScheduleAdapter(private var classList: List<ClassItem>) :
    RecyclerView.Adapter<ClassScheduleAdapter.ClassViewHolder>() {

    // Inner class for ViewHolder
    class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textStartTime: TextView = itemView.findViewById(R.id.textStartTime)
        val textEndTime: TextView = itemView.findViewById(R.id.textEndTime)
        val textClassName: TextView = itemView.findViewById(R.id.textClassName)
        val textClassroom: TextView = itemView.findViewById(R.id.textClassroom)
        val textMaxStudents: TextView = itemView.findViewById(R.id.textMaxStudents)

        fun bind(classItem: ClassItem) {
            textStartTime.text = classItem.startTime
            textEndTime.text = classItem.endTime
            textClassName.text = classItem.className
            textClassroom.text = "Room: ${classItem.classroom}"
            textMaxStudents.text = "Max Students: ${classItem.maxStudents}"
            // Note: You can add more logic here, e.g., coloring the item based on time.
        }
    }

    // Called when a new ViewHolder is needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class_schedule, parent, false)
        return ClassViewHolder(view)
    }

    // Called to bind data to an existing ViewHolder
    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        holder.bind(classList[position])
    }

    // Returns the total number of items
    override fun getItemCount(): Int = classList.size

    // Public method to update the list and refresh the RecyclerView
    fun updateList(newList: List<ClassItem>) {
        classList = newList
        notifyDataSetChanged()
    }
}