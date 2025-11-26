package com.example.markmyattendence.teacher

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.markmyattendence.data.TeacherModel

// TeacherViewModel.kt

class TeacherViewModel : ViewModel() {

    // MutableLiveData is used internally to update the data
    private val _teacherProfile = MutableLiveData<TeacherModel?>()

    // LiveData is used externally (in Fragments) to observe the data safely
    val teacherProfile: LiveData<TeacherModel?> = _teacherProfile

    // Function called by your TeacherProfileFragment to save the fetched data
    fun saveTeacherProfile(model: TeacherModel) {
        _teacherProfile.value = model
    }

    // Function to retrieve the current profile, if needed (returns null if not yet loaded)
    fun getCurrentProfile(): TeacherModel? {
        return _teacherProfile.value
    }
}