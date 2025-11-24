package com.example.markmyattendence.student.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.markmyattendence.R
import com.example.markmyattendence.data.ClassModel
import com.example.markmyattendence.databinding.ClassListItemBinding

class StudentClassAdapter(
    private var classList: List<ClassModel>,
    private val onItemClick: (ClassModel) -> Unit
) : RecyclerView.Adapter<StudentClassAdapter.ClassViewHolder>() {

    inner class ClassViewHolder(private val binding: ClassListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(classModel: ClassModel) {
            val ctx = binding.root.context

            binding.tvClassName.text = classModel.className
            binding.tvSheduleDay.text = classModel.date
            binding.tvClassCode.text = classModel.classCodeUid
            binding.tvClassroom.text = ctx.getString(
                R.string.classroom_format,
                classModel.classroom
            )

            binding.tvClassTime.text = ctx.getString(
                R.string.time_range_format,
                classModel.startTime,
                classModel.endTime
            )
            binding.tvClassDays.text = classModel.repeatDays.joinToString(", ")
            binding.tvTeacher.text = ctx.getString(
                R.string.teacher_uid_format,
                classModel.teacherUid
            )

            binding.root.setOnClickListener {
                onItemClick(classModel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val binding = ClassListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClassViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        holder.bind(classList[position])
    }

    override fun getItemCount(): Int = classList.size

    fun updateList(newList: List<ClassModel>) {
        classList = newList
        notifyDataSetChanged()
    }
}
