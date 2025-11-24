package com.example.markmyattendence.student

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.markmyattendence.R
import com.example.markmyattendence.StartUI.LoginActivity
import com.example.markmyattendence.databinding.FragmentStudentProfileBinding
import com.example.markmyattendence.databinding.FragmentTeacherProfileBinding
import com.example.markmyattendence.teacher.TeacherProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentProfile : Fragment() {
    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentStudentProfileBinding? = null
    private val binding get() = _binding!!



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStudentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchUserProfileData()

        // 2. Setup Logout Listener
        binding.ivLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(activity, LoginActivity::class.java)
            Toast.makeText(requireContext(), "Logout Successfully!", Toast.LENGTH_SHORT).show()
            startActivity(intent)
            activity?.finish()
        }
    }
    private fun fetchUserProfileData() {
        val firebaseUser = auth.currentUser
        val userId = firebaseUser?.uid

        if (userId == null) {
            Log.e("ProfileFragment", "User not logged in or UID is null")
            return
        }

        val db = FirebaseFirestore.getInstance()

        val userRef = db.collection("users").document(userId)

        userRef.get()
            .addOnSuccessListener { document ->
                // ⚠️ FIX: Check if the Fragment's view is still available (i.e., not null)
                if (view == null) {
                    // The view has been destroyed. Safely exit without touching the UI.
                    Log.d("ProfileFragment", "Data arrived but view was destroyed. Skipping UI update.")
                    return@addOnSuccessListener
                }

                // Now, it is safe to access viewLifecycleOwner and the binding
                if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {

                    if (document.exists()) {
                        val name = document.getString("name")
                        val email = document.getString("email")

                        // Assuming 'binding' is accessed safely (as previously discussed)
                        binding.tvUserName.text = name
                        binding.tvEmailUser.text = email
                    } else {
                        Log.d("ProfileFragment", "No such document for UID: $userId")
                    }
                }
            }
            .addOnFailureListener { exception ->
                // No need for lifecycle check here, as it's not a UI update crash
                Log.w("ProfileFragment", "Error getting documents: ", exception)
                Toast.makeText(requireContext(), "Failed to load profile data.", Toast.LENGTH_SHORT).show()
            }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    companion object {
        @JvmStatic
        fun newInstance(): StudentProfile {
            return StudentProfile()
        }
    }
}