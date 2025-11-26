package com.example.markmyattendence.data

import com.google.firebase.firestore.PropertyName

data class TeacherModel(
    val collegeId: String? = null,
    val collegeName: String? = null,
    val department: String? = null,
    val email: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val role: String? = null,

    val subject_Name: String? = null,

    val subject_code: String? = null,
    val teacherId: String? = null,

)