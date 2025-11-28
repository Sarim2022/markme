package com.example.markmyattendence.data

data class StudentDisplayModel(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val joinedClasses: List<String> = emptyList(),
    val classNames: List<String> = emptyList()
)
