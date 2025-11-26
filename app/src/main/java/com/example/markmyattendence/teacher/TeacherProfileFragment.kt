package com.example.markmyattendence.teacher

import android.content.Intent
import android.os.Bundle
import android.util.Log // <--- Add this import for Log.d/Log.w
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.markmyattendence.StartUI.LoginActivity
import com.example.markmyattendence.data.TeacherModel
import com.example.markmyattendence.databinding.FragmentTeacherProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeacherProfileFragment : Fragment() {
// Inside TeacherProfileFragment.kt (after the class declaration)

    // Import the necessary dependency: androidx.fragment.app.activityViewModels
    private val viewModel: TeacherViewModel by activityViewModels()
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

        fetchUserProfileData()
        viewModel.teacherProfile.observe(viewLifecycleOwner) { teacherData ->

            if (teacherData != null) {

                // Display User Name
                binding.tvUserName.text = teacherData.name ?: "N/A"

                // Display User Email
                binding.tvEmailUser.text = teacherData.email ?: "N/A"

                // Log to confirm update
                Log.d("ProfileFragment", "UI updated with data for: ${teacherData.name}")
            }
        }
        initUi()
    }
    private fun initUi(){

        binding.llMyInfo.setOnClickListener {
            val intent = Intent(requireContext(), MyInfoActivity::class.java)
            startActivity(intent)
        }
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
            // Optional: Navigate to login screen
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)

        userRef.get()
            .addOnSuccessListener { document ->
                if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                    if (document.exists()) {
                        // 🔥 Crucial Step: Map the document to your data class
                        val teacherData: TeacherModel? = document.toObject(TeacherModel::class.java)

                        if (teacherData != null) {
                            // 1. Store the data in the ViewModel/Repository for global access
                            // This replaces the need to pass it via Intent later.
                            viewModel.saveTeacherProfile(teacherData)

                            // 2. Use the data class fields directly for UI updates
                            binding.tvUserName.text = teacherData.name
                            binding.tvEmailUser.text = teacherData.email

                            // You can log the data to verify
                            Log.d("ProfileFragment", "Profile loaded for: ${teacherData.name}")
                        } else {
                            Log.w(
                                "ProfileFragment",
                                "Document exists but failed to parse into TeacherModel."
                            )
                        }

                    } else {
                        Log.d("ProfileFragment", "No such document for UID: $userId")
                    }
                } else {
                    Log.d(
                        "ProfileFragment",
                        "Data arrived but view was destroyed. Skipping UI update."
                    )
                }
            }
            .addOnFailureListener { exception ->
                Log.w("ProfileFragment", "Error getting documents: ", exception)
                Toast.makeText(requireContext(), "Failed to load profile data.", Toast.LENGTH_SHORT)
                    .show()
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