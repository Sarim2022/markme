package com.example.markmyattendence.teacher.Adatper

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.markmyattendence.data.ClassModel
import com.example.markmyattendence.databinding.ItemClassSettingCardBinding

class ClassSettingAdapter(
    private val onClassClick: (ClassModel) -> Unit
) : RecyclerView.Adapter<ClassSettingAdapter.ClassSettingViewHolder>() {

    private var classList: List<ClassModel> = emptyList()

    inner class ClassSettingViewHolder(private val binding: ItemClassSettingCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(classModel: ClassModel) {
            binding.tvClassName.text = classModel.className
            binding.tvClassTime.text = "${classModel.startTime} - ${classModel.endTime}"
            binding.tvClassCode.text = "Code: ${classModel.classCodeUid}"

            val approveStatusText = if (classModel.autoApprove) {
                "Auto Approve: Yes"
            } else {
                "Auto Approve: Manual"
            }


            val daysString = classModel.repeatDays.joinToString(", ")


            val maxStudentsText = classModel.maxStudents?.let { "Max Students: $it" } ?: "Max Students: Unlimited"

            binding.root.setOnClickListener {
                onClassClick(classModel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassSettingViewHolder {
        val binding = ItemClassSettingCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClassSettingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClassSettingViewHolder, position: Int) {
        holder.bind(classList[position])
    }

    override fun getItemCount(): Int = classList.size

    fun updateClasses(newClasses: List<ClassModel>) {
        classList = newClasses
        notifyDataSetChanged()
    }
}
