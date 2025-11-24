package com.example.markmyattendence.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue


fun main(){

}



/**
 * Stores the newly created classId into the current teacher's myClasses array.
 */
fun linkClassToTeacherProfile(
    teacherUid: String,
    classId: String,
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    onComplete: (isSuccess: Boolean) -> Unit
) {
    // 1. Reference the teacher's document
    val teacherRef = db.collection("users").document(teacherUid)

    // 2. Atomically update the myClasses array
    teacherRef.update("myClasses", FieldValue.arrayUnion(classId))
        .addOnSuccessListener {
            onComplete(true)
        }
        .addOnFailureListener { e ->
            // Handle error, e.g., if the teacher document doesn't exist
            println("Error linking class to teacher: $e")
            onComplete(false)
        }
}