package com.example.markmyattendence.teacher

import android.content.Intent
import android.os.Bundle
import android.util.Log // <--- Add this import for Log.d/Log.w
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.markmyattendence.StartUI.LoginActivity
import com.example.markmyattendence.databinding.FragmentTeacherProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeacherProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentTeacherProfileBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Fetch and Display Data (CORRECT LOCATION)
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

    /**
     * Fetches user data from Firestore and updates the UI.
     */
    // In your TeacherProfileFragment.kt

    private fun fetchUserProfileData() {
        val firebaseUser = auth.currentUser
        val userId = firebaseUser?.uid

        if (userId == null) {
            Log.e("ProfileFragment", "User not logged in or UID is null")
            return
        }

        val db = FirebaseFirestore.getInstance()
        // Using "users" collection as per your screenshot
        val userRef = db.collection("users").document(userId)

        userRef.get()
            .addOnSuccessListener { document ->

                // 🚨 Crucial Fix: Check if the view is still active before accessing binding
                if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                    if (document.exists()) {
                        val name = document.getString("name")
                        val email = document.getString("email")

                        // Update the UI only if the view is active
                        binding.tvUserName.text = name
                        binding.tvEmailUser.text = email
                    } else {
                        Log.d("ProfileFragment", "No such document for UID: $userId")
                    }
                } else {
                    // Log if the data arrived but the view was already destroyed
                    Log.d("ProfileFragment", "Data arrived but view was destroyed. Skipping UI update.")
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
        fun newInstance(): TeacherProfileFragment {
            return TeacherProfileFragment()
        }
    }
}