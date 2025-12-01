package com.example.markmyattendence.data

data class AttendanceRecord(
    val classId: String = "",
    val date: String = "",
    val sessionStart: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),

    val sessionEnd: com.google.firebase.Timestamp? = null,
    val teacherUid: String = "",

    val attendedStudents: Map<String, com.google.firebase.Timestamp> = emptyMap()
)