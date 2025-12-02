package com.example.markmyattendence.data

data class StudentAttendance(
    val studentUid: String,
    val studentName: String,
    var isPresent: Boolean = false
)