package com.example.markmyattendence.teacher.Adatper

// ... (existing imports)

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.markmyattendence.R
import com.example.markmyattendence.data.ClassModel
import com.example.markmyattendence.databinding.ClassCardLayoutBinding
import com.example.markmyattendence.teacher.TeacherHomeNavFragment

interface OnClassItemClickListener {
    fun onClassClicked(classItem: ClassModel) // <-- ADD THIS METHOD
    fun onDeleteClicked(classId: String)
}
interface OnClassClickListener {
    fun onClassClicked(classItem: ClassModel)
}

// 2. ADAPTER CONSTRUCTOR: Accept the listener
class ClassAdapter(
    private var classList: List<ClassModel>,
    private val listener: TeacherHomeNavFragment
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    // IMPORTANT: Make sure classList is a MutableList if you use removeItem()
    init {
        this.classList = classList.toMutableList()
    }

    inner class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Use the binding object for views, not the itemView reference
        private val binding = ClassCardLayoutBinding.bind(itemView)

        // Pass the ClassModel object directly
        fun bind(classModel: ClassModel) {

            // 1. BIND DATA TO VIEWS
            binding.tvClassName.text = classModel.className
            binding.tvClassroom.text = classModel.classroom
            binding.tvRepeatDays.text = "Repeat: ${classModel.repeatDays.joinToString(", ")}"
            binding.tvClassTime.text = "${classModel.startTime} - ${classModel.endTime}"
            binding.tvClassCode.text = "Code: ${classModel.classCodeUid}"

            val approveStatusText = if (classModel.autoApprove) {
                "Auto Approve: Yes"
            } else {
                "Auto Approve: No"
            }
            binding.tvAutoApprove.text = approveStatusText

            // 2. SET CLICK LISTENER CORRECTLY
            // Set the click listener on the entire card/root view of the item (itemView)
            itemView.setOnClickListener {
                // This calls the method we just implemented in the Fragment
                listener.onClassClicked(classModel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.class_card_layout,
            parent,
            false
        )
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        holder.bind(classList[position])
    }

    override fun getItemCount(): Int = classList.size

    // ... (updateList and removeItem functions remain the same)
    /**
     * Updates the adapter's data list and notifies the RecyclerView to refresh.
     * We'll update this slightly to use a mutable list in the adapter for better deletion performance.
     */
    fun updateList(newList: List<ClassModel>) {
        this.classList = newList.toMutableList() // Ensure the adapter holds a mutable list internally
        notifyDataSetChanged()
    }



    // Optional: Function to remove the item from the list instantly after successful deletion
    fun removeItem(classId: String) {
        val index = classList.indexOfFirst { it.classId == classId }
        if (index != -1) {
            (classList as MutableList).removeAt(index)
            notifyItemRemoved(index)
        }
    }
}