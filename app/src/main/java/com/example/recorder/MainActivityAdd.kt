package com.example.recorder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.recorder.databinding.ActivityMainAddBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class MainActivityAdd : AppCompatActivity() {

    private lateinit var binding: ActivityMainAddBinding
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var progressBar: ProgressBar
    private var imageUri: Uri? = null
    private val SPEECH_REQUEST_CODE = 100

    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        progressBar = binding.progressBar

        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Silakan berbicara")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("id", "ID"))
        }

        binding.buttonRecord.setOnClickListener {
            startActivityForResult(speechRecognizerIntent, SPEECH_REQUEST_CODE)
        }

        binding.buttonImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.buttonSubmit.setOnClickListener {
            saveNoteToDatabase()
        }

        binding.buttonBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (result != null && result.isNotEmpty()) {
                val recognizedText = result[0]
                val currentText = binding.editTextNote.text.toString()
                val newText = if (currentText.isEmpty()) {
                    recognizedText
                } else {
                    "$currentText $recognizedText"
                }
                binding.editTextNote.setText(newText)
                binding.editTextNote.setSelection(newText.length)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.imageView.setImageURI(it)
            imageUri = it
        }
    }

    private fun saveNoteToDatabase() {
        val noteText = binding.editTextNote.text.toString().trim()
        val imageUri = this.imageUri

        if (noteText.isEmpty()) {
            Toast.makeText(this, "Catatan tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        toggleUiInteraction(false)
        progressBar.visibility = View.VISIBLE

        if (imageUri != null) {
            val fileName = "${System.currentTimeMillis()}.jpg"
            val storageRef = storage.reference.child("images/$fileName")

            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    Log.d("FirebaseStorage", "Upload successful")
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        saveNoteToFirebase(noteText, imageUrl)
                    }.addOnFailureListener { e ->
                        handleFailure(e, "Gagal mendapatkan URL gambar")
                    }
                }
                .addOnFailureListener { e ->
                    handleFailure(e, "Gagal mengunggah gambar")
                }
        } else {
            saveNoteToFirebase(noteText, null)
        }
    }

    private fun saveNoteToFirebase(noteText: String, imageUrl: String?) {
        val noteId = database.push().key ?: return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val authorName = document.getString("name") ?: "Anonim"
                val note = Note(
                    id = noteId,
                    text = noteText,
                    imageUrl = imageUrl,
                    timestamp = System.currentTimeMillis(),
                    author = authorName,
                    userId = userId
                )

                database.child("notes").child(noteId).setValue(note)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Catatan disimpan", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        toggleUiInteraction(true)
                        val intent = Intent(this, UserNotesActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        handleFailure(e, "Gagal menyimpan catatan")
                    }
            }
            .addOnFailureListener { e ->
                handleFailure(e, "Gagal mendapatkan nama penulis")
            }
    }

    private fun handleFailure(e: Exception, message: String) {
        Log.e("Firebase", "Error: ${e.message}")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        progressBar.visibility = View.GONE
        toggleUiInteraction(true)
    }

    private fun toggleUiInteraction(enable: Boolean) {
        binding.editTextNote.isEnabled = enable
        binding.buttonRecord.isEnabled = enable
        binding.buttonImage.isEnabled = enable
        binding.buttonSubmit.isEnabled = enable
        binding.buttonBack.isEnabled = enable
    }

    data class Note(
        var id: String = "",
        var text: String = "",
        var imageUrl: String? = null,
        var author: String = "",
        var userId: String = "",
        val timestamp: Long = System.currentTimeMillis()
    )
}
