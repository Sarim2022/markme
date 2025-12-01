package com.example.markmyattendence.data

data class ActiveAttendanceSession(
    // Document ID will be the Class ID (e.g., "wlxfjXyRYOe576UPo6wT")
    val classId: String = "",
    val qrCodeToken: String = "",
    val startTime: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val expiryTime: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val teacherUid: String = ""
)