package com.example.markmyattendence.data



data class ClassItem(
    val autoApprove: Boolean = false,
    val classCodeUid: String = "",
    val classID: String = "",
    val className: String = "",
    val classroom: String = "",
    val endTime: String = "", // e.g., "9:25 pm"
    val maxStudents: Int? = null, // Corrected to nullable Int?
    val repeatDays: List<String> = emptyList(),
    val startDate: String = "",
    val startTime: String = "",
    val teacherUid: String = ""
) {
}