package com.example.markmyattendence.teacher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.markmyattendence.data.ClassModel
import com.example.markmyattendence.databinding.ActivityMyClassSettingBinding
import com.example.markmyattendence.teacher.Adatper.ClassSettingAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MyClassSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyClassSettingBinding
    private lateinit var adapter: ClassSettingAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val TAG = "MyClassSettingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyClassSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadTeacherClasses()
    }

    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Classes"

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Setup RecyclerView
        adapter = ClassSettingAdapter { classModel ->
            openClassDetails(classModel)
        }
        binding.rvClasses.layoutManager = LinearLayoutManager(this)
        binding.rvClasses.adapter = adapter
    }

    private fun loadTeacherClasses() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val teacherUid = currentUser.uid

        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        binding.rvClasses.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // First, get the teacher's myClasses array
                val teacherDoc = db.collection("users").document(teacherUid).get().await()
                val myClasses = teacherDoc.get("myClasses") as? List<String> ?: emptyList()

                Log.d(TAG, "Found ${myClasses.size} classes for teacher: $teacherUid")

                if (myClasses.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        showEmptyState()
                    }
                    return@launch
                }

                // Fetch class details for each class ID
                val classList = mutableListOf<ClassModel>()
                for (classId in myClasses) {
                    try {
                        val classDoc = db.collection("classes").document(classId).get().await()
                        val classModel = classDoc.toObject(ClassModel::class.java)
                        if (classModel != null) {
                            classModel.classId = classDoc.id // Ensure classId is set
                            classList.add(classModel)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching class $classId: ${e.message}")
                    }
                }

                Log.d(TAG, "Successfully loaded ${classList.size} classes")

                withContext(Dispatchers.Main) {
                    if (classList.isNotEmpty()) {
                        adapter.updateClasses(classList)
                        showClasses()
                    } else {
                        showEmptyState()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading classes: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MyClassSettingActivity, "Error loading classes", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
            }
        }
    }

    private fun showClasses() {
        binding.progressBar.visibility = View.GONE
        binding.rvClasses.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.progressBar.visibility = View.GONE
        binding.rvClasses.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
    }

    private fun openClassDetails(classModel: ClassModel) {
        val intent = Intent(this, ClassDetailsActivity::class.java).apply {
            putExtra("CLASS_ID", classModel.classId)
            putExtra("CLASS_NAME", classModel.className)
            putExtra("CLASS_ROOM", classModel.classroom)
            putExtra("START_TIME", classModel.startTime)
            putExtra("END_TIME", classModel.endTime)
            putExtra("CLASS_CODE", classModel.classCodeUid)
            putExtra("AUTO_APPROVE", classModel.autoApprove)
            putExtra("START_DATE", classModel.startDate)
            putExtra("MAX_STUDENTS", classModel.maxStudents)
            putStringArrayListExtra("REPEAT_DAYS", ArrayList(classModel.repeatDays))
        }
        startActivity(intent)
    }
}
