package com.example.markmyattendence.data

import com.google.firebase.firestore.PropertyName

data class ClassModel(

    @get:PropertyName("classId")
    var classId: String = "",

    @get:PropertyName("className")
    val className: String = "",

    @get:PropertyName("classroom")
    val classroom: String = "",

    @get:PropertyName("startDate")
    val date: String = "",

    @get:PropertyName("startTime")
    val startTime: String = "",

    @get:PropertyName("endTime")
    val endTime: String = "",

    @get:PropertyName("maxStudents")
    val maxStudents: Int? = null, // Note: maxStudents is optional/nullable

    @get:PropertyName("autoApprove")
    val autoApprove: Boolean = true,

    @get:PropertyName("repeatDays")
    val repeatDays: List<String> = emptyList(),

    @get:PropertyName("teacherUid")
    val teacherUid: String = "",

    @get:PropertyName("classCodeUid")
    val classCodeUid: String = "",

    val studentJoined: List<String> = emptyList(), // Stores UIDs of joined students
    val requestStudent: List<String> = emptyList() // Stores UIDs of students who requested to join

)