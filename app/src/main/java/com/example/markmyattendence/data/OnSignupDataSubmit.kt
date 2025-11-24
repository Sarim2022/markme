package com.example.markmyattendence.data


interface OnSignupDataSubmit {
    fun onTeacherSignupSubmit(
        name: String,
        email: String,
        phone: String,
        department: String,
        subjectName: String,
        subjectCode: String,
        collegeId: String,
        collegeName: String,
        teacherId: String,
        password: String
    )

    fun onStudentSignupSubmit(
        name: String,
        email: String,
        collegeName: String,
        department: String,
        studentId: String,
        password: String
    )
}
