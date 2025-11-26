package com.example.markmyattendence.teacher.Adatper


import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.markmyattendence.R
import com.example.markmyattendence.data.ClassModel
import com.example.markmyattendence.databinding.ClassCardLayoutBinding
import com.example.markmyattendence.teacher.TeacherHomeNavFragment

interface OnClassItemClickListener {
    fun onClassClicked(classItem: ClassModel)
    fun onDeleteClicked(classId: String)
}

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

            binding.tvClassTime.text = "${classModel.startTime} - ${classModel.endTime}"
            binding.tvClassCode.text = "Code: ${classModel.classCodeUid}"


            binding.tvDate.text = "On ${classModel.startDate}"

            val approveStatusText = if (classModel.autoApprove) {
                "Auto Approve: Yes"
            } else {
                "Auto Approve: Manual"
            }

            binding.ivOptions.setOnClickListener {

                val shareText = """
        📘 Class Details
        
        Class Name: ${classModel.className}
        Classroom: ${classModel.classroom}
        Time: ${classModel.startTime} - ${classModel.endTime}
        Date: On ${classModel.startDate}
        Repeat Days: ${classModel.repeatDays.joinToString(", ")}
        Students Allowed: ${classModel.maxStudents}
        Auto Approve: ${if (classModel.autoApprove) "Yes" else "Manual"}
        
        Class Code: ${classModel.classCodeUid}
        Join the class using this code!
    """.trimIndent()

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }

                val chooser = Intent.createChooser(intent, "Share Class Details")
                itemView.context.startActivity(chooser)
            }

            itemView.setOnClickListener {
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