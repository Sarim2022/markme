package com.example.markmyattendence.data


data class StudentData(
    val collegeName: String = "",
    val department: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "",
    val studentId: String = "",
    val uid: String = "",
    val joinedClasses: List<String> = emptyList(), // Array of class UIDs
    val requestedClasses: List<String> = emptyList() // Array of class UIDs
)


object AppCache {

    var studentProfile: StudentData? = null
        private set

    fun setStudentProfile(data: StudentData) {
        this.studentProfile = data
    }
    fun clear() {
        studentProfile = null
    }

}