package com.example.markmyattendence.teacher.Adatper

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.markmyattendence.data.StudentDisplayModel
import com.example.markmyattendence.databinding.ItemStudentListCardBinding

class StudentListAdapter : RecyclerView.Adapter<StudentListAdapter.StudentViewHolder>() {

    private var studentList: List<StudentDisplayModel> = emptyList()

    inner class StudentViewHolder(private val binding: ItemStudentListCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(student: StudentDisplayModel) {
            binding.tvStudentName.text = student.name
            binding.tvStudentEmail.text = student.email

            val classesText = if (student.classNames.isNotEmpty()) {
                "Classes: ${student.classNames.joinToString(", ")}"
            } else {
                "Classes: None"
            }
            binding.tvStudentClasses.text = classesText
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = ItemStudentListCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        holder.bind(studentList[position])
    }

    override fun getItemCount(): Int = studentList.size

    fun updateStudents(newStudents: List<StudentDisplayModel>) {
        studentList = newStudents
        notifyDataSetChanged()
    }
}
