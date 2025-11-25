package com.example.markmyattendence.StartUI

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.markmyattendence.R // Make sure this is your correct R path
import com.example.markmyattendence.data.OnSignupDataSubmit
import com.example.markmyattendence.data.PasswordPolicy // Import the Policy utility
import com.example.markmyattendence.data.PolicyResult // Import the Policy data class
import com.example.markmyattendence.databinding.FragmentTeacherSignupBinding
import com.google.android.material.textfield.TextInputLayout

class TeacherSignupFragment : Fragment() {

    private var _binding: FragmentTeacherSignupBinding? = null
    private val binding get() = _binding!!
    private val passwordPolicy = PasswordPolicy()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        setupTextWatchers()
    }

    private fun initUI() {
        // Show password policy when the password field gains focus
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            binding.mvPasswordPolicy.visibility = if (hasFocus) View.VISIBLE else View.GONE
        }

        // Setup signup button listener (assuming SignupActivity has the button)
        val parent = activity as? OnSignupDataSubmit
        (activity as? SignupActivity)?.binding?.btnSignup?.setOnClickListener {
            // Validate all inputs before submission
            if (validateInputs() && parent != null) {
                val name = binding.etName.text.toString().trim()
                val email = binding.etEmail.text.toString().trim()
                val phone = binding.etPhone.text.toString().trim()
                val department = binding.etDepartment.text.toString().trim()
                val subjectName = binding.etSubject.text.toString().trim()
                val subjectCode = binding.etSubjectCode.text.toString().trim()
                val collegeId = binding.etCollegeID.text.toString().trim()
                val collegeName = binding.etCollegeName.text.toString().trim()
                val teacherId = binding.etTeacherID.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()

                parent.onTeacherSignupSubmit(
                    name, email, phone, department, subjectName, subjectCode,
                    collegeId, collegeName, teacherId, password
                )
            }
        }
    }

    private fun setupTextWatchers() {
        // Continuous password policy checking
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updatePasswordPolicyUI(s.toString())
                checkPasswordMatch() // Check match simultaneously
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Check password matching on confirm password change
        binding.etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                checkPasswordMatch()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    /**
     * Updates the UI icons and colors based on the current password input.
     */
    private fun updatePasswordPolicyUI(password: String) {
        val policyResults = passwordPolicy.checkPolicy(password)

        // Resources (assuming you have defined these colors/drawables)
        val passIcon = ContextCompat.getDrawable(requireContext(), R.drawable.markgreen)
        val failIcon = ContextCompat.getDrawable(requireContext(), R.drawable.tickgreen)
        val passColor = ContextCompat.getColor(requireContext(), R.color.green)
        val failColor = ContextCompat.getColor(requireContext(), R.color.red)

        // Helper function to update a rule view
        val updateRule = { iconView: View, rule: Boolean, textViewId: Int ->
            val icon = (iconView as? android.widget.ImageView)
            icon?.setImageDrawable(if (rule) passIcon else failIcon)
            icon?.setColorFilter(if (rule) passColor else failColor)
            (binding.root.findViewById<android.widget.TextView>(textViewId))?.setTextColor(if (rule) passColor else failColor)
        }

        // Apply updates
        updateRule(binding.ivLengthIcon, policyResults.isLengthValid, R.id.tvLengthRule)
        updateRule(binding.ivSymbolIcon, policyResults.isSymbolValid, R.id.tvSymbolRule)
        updateRule(binding.ivAlphaIcon, policyResults.isMixedCaseValid, R.id.tvAlphaRule)
        updateRule(binding.ivDigitIcon, policyResults.isDigitValid, R.id.tvDigitRule)
    }

    /**
     * Checks if the passwords match and updates the Confirm Password field error state.
     */
    private fun checkPasswordMatch() {
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        val confirmPasswordLayout = (binding.etConfirmPassword.parent.parent as? TextInputLayout)

        if (confirmPassword.isEmpty() || password == confirmPassword) {
            confirmPasswordLayout?.error = null
        } else {
            confirmPasswordLayout?.error = "Passwords do not match"
        }
    }


    /**
     * Performs final validation before submitting the form.
     */
    private fun validateInputs(): Boolean {
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        var isValid = true

        // 1. Check for empty fields and standard validation (Example for Name)
        if (binding.etName.text.isNullOrEmpty()) {
            (binding.etName.parent.parent as? TextInputLayout)?.error = "Name is required"
            isValid = false
        } else {
            (binding.etName.parent.parent as? TextInputLayout)?.error = null
        }
        // ... (Add checks for all other required fields like email format, phone, IDs, etc.) ...

        // 2. Check Password Policy
        if (!passwordPolicy.checkPolicy(password).isPolicyMet) {
            (binding.etPassword.parent.parent as? TextInputLayout)?.error = "Password does not meet requirements"
            isValid = false
        } else {
            (binding.etPassword.parent.parent as? TextInputLayout)?.error = null
        }

        // 3. Check Password Match
        if (password != confirmPassword) {
            (binding.etConfirmPassword.parent.parent as? TextInputLayout)?.error = "Passwords do not match"
            isValid = false
        } else {
            (binding.etConfirmPassword.parent.parent as? TextInputLayout)?.error = null
        }

        return isValid
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}