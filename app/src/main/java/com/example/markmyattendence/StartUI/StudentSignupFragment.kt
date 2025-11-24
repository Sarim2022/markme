package com.example.markmyattendence.StartUI

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.Toast
import com.example.markmyattendence.data.OnSignupDataSubmit
import com.example.markmyattendence.databinding.FragmentStudentSignupBinding

class StudentSignupFragment : Fragment() {

    private var _binding: FragmentStudentSignupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parent = activity as OnSignupDataSubmit

        (activity as SignupActivity).binding.btnSignup.setOnClickListener {

            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val collegeName = binding.etCollegeName.text.toString().trim()
            val department = binding.etDepartment.text.toString().trim()
            val studentId = binding.etStudentID.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()


            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            parent.onStudentSignupSubmit(
                name, email, collegeName, department, studentId, password ,
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
