package com.example.markmyattendence.teacher.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.markmyattendence.data.EnrolledStudent
import com.example.markmyattendence.databinding.ItemEnrolledStudentBinding

class EnrolledStudentsAdapter : RecyclerView.Adapter<EnrolledStudentsAdapter.EnrolledStudentViewHolder>() {

    private var studentsList: List<EnrolledStudent> = emptyList()

    inner class EnrolledStudentViewHolder(private val binding: ItemEnrolledStudentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(student: EnrolledStudent) {
            binding.tvStudentName.text = student.name
            binding.tvStudentEmail.text = student.email
            binding.tvStudentId.text = "Student ID: ${student.studentId}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnrolledStudentViewHolder {
        val binding = ItemEnrolledStudentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EnrolledStudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EnrolledStudentViewHolder, position: Int) {
        holder.bind(studentsList[position])
    }

    override fun getItemCount(): Int = studentsList.size

    fun updateStudents(newStudents: List<EnrolledStudent>) {
        studentsList = newStudents
        notifyDataSetChanged()
    }
}