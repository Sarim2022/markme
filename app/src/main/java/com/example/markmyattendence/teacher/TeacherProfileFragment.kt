package com.example.markmyattendence.teacher

import android.R
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.markmyattendence.StartUI.LoginActivity
import com.example.markmyattendence.data.TeacherModel
import com.example.markmyattendence.databinding.FragmentTeacherProfileBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TeacherProfileFragment : Fragment() {

    private val viewModel: TeacherViewModel by activityViewModels()
    private lateinit var auth: FirebaseAuth

    // UI binding
    private var _binding: FragmentTeacherProfileBinding? = null
    private val binding get() = _binding!!

    // SharedPreferences and Gallery picker
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

        // Initialize gallery launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    saveProfileImageUri(uri)
                    loadProfileImage()
                }
            }
        }
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
        loadProfileImage() // Load profile image when view is created
        loadNotificationPreference() // Load notification preference when view is created

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

        // Profile image click listener
        binding.ivProfile.setOnClickListener {
            openGalleryPicker()
        }



        binding.optMyClasses.setOnClickListener {
            val intent = Intent(requireContext(), MyClassSettingActivity::class.java)
            startActivity(intent)
        }

        binding.optAllStudents.setOnClickListener {
            val intent = Intent(requireContext(), MyStudentListActivity::class.java)
            startActivity(intent)
        }

        binding.optChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.optDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }

        // Notification toggle listener
        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationPreference(isChecked)
        }

        binding.ivLogout.setOnClickListener {
            // Set isActive = false before signing out
            val user = auth.currentUser
            if (user != null) {
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(user.uid)
                    .update("isActive", false)
                    .addOnSuccessListener {
                        Log.d("TeacherProfileFragment", "isActive set to false for user: ${user.uid}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("TeacherProfileFragment", "Failed to set isActive to false: ${e.message}")
                    }
            }

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

    private fun showChangePasswordDialog() {
        val editText = EditText(requireContext())
        editText.hint = "Enter new password"
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setView(editText)
            .setPositiveButton("Change Password") { dialog, _ ->
                val newPassword = editText.text.toString().trim()

                if (newPassword.length < 6) {
                    Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                changePassword(newPassword)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changePassword(newPassword: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        user.updatePassword(newPassword)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to change password: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteAccount()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val teacherUid = user.uid
        val db = FirebaseFirestore.getInstance()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get teacher's classes to delete them
                val teacherDoc = db.collection("users").document(teacherUid).get().await()
                val myClasses = teacherDoc.get("myClasses") as? List<String> ?: emptyList()

                // Delete all classes created by this teacher
                for (classId in myClasses) {
                    try {
                        db.collection("classes").document(classId).delete().await()
                        Log.d("DeleteAccount", "Deleted class: $classId")
                    } catch (e: Exception) {
                        Log.e("DeleteAccount", "Error deleting class $classId: ${e.message}")
                    }
                }

                // Delete teacher document from users collection
                db.collection("users").document(teacherUid).delete().await()
                Log.d("DeleteAccount", "Deleted teacher document")

                // Delete Firebase Auth user
                user.delete().await()
                Log.d("DeleteAccount", "Deleted Firebase Auth user")

                // Redirect to login
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity?.finish()
                }

            } catch (e: Exception) {
                Log.e("DeleteAccount", "Error deleting account: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to delete account: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openGalleryPicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        galleryLauncher.launch(intent)
    }

    private fun saveProfileImageUri(uri: Uri) {
        sharedPreferences.edit()
            .putString("profile_image_uri", uri.toString())
            .apply()
    }

    private fun loadProfileImage() {
        val uriString = sharedPreferences.getString("profile_image_uri", null)
        if (uriString != null) {
            try {
                val uri = Uri.parse(uriString)
                binding.ivProfile.setImageURI(uri)
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error loading profile image: ${e.message}")
                // Reset to default if there's an error
                binding.ivProfile.setImageResource(R.drawable.gallery_thumb)
            }
        } else {
            // Show default placeholder
            binding.ivProfile.setImageResource(R.drawable.gallery_thumb)
        }
    }

    private fun loadNotificationPreference() {
        val user = auth.currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val notificationEnabled = document.getBoolean("notification") ?: false
                binding.switchNotification.isChecked = notificationEnabled
            }
            .addOnFailureListener { e ->
                Log.e("TeacherProfileFragment", "Error loading notification preference: ${e.message}")
            }

    }

    private fun updateNotificationPreference(enabled: Boolean) {
        val user = auth.currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .update("notification", enabled)
            .addOnSuccessListener {
                Log.d("TeacherProfileFragment", "Notification preference updated: $enabled")
            }
            .addOnFailureListener { e ->
                Log.e("TeacherProfileFragment", "Error updating notification preference: ${e.message}")
                // Revert the switch state on failure
                binding.switchNotification.isChecked = !enabled
                Toast.makeText(requireContext(), "Failed to update notification preference", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        @JvmStatic
        fun newInstance(): TeacherProfileFragment {
            return TeacherProfileFragment()
        }
    }
}
