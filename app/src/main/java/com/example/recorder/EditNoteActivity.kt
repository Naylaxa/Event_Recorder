package com.example.recorder

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.recorder.databinding.ActivityEditNoteBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

class EditNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditNoteBinding
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private var noteId: String = ""
    private var originalText: String = ""
    private var originalImageUrl: String = ""
    private var author: String = ""
    private var selectedImageUri: Uri? = null
    private var originalTimestamp: Long = 0

    private val SPEECH_REQUEST_CODE = 101
    private val REQUEST_CODE_SELECT_IMAGE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Firebase
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        // Ambil data dari Intent
        noteId = intent.getStringExtra("NOTE_ID") ?: ""
        originalText = intent.getStringExtra("NOTE_TEXT") ?: ""
        originalImageUrl = intent.getStringExtra("NOTE_IMAGE_URL") ?: ""
        originalTimestamp = intent.getLongExtra("NOTE_TIMESTAMP", System.currentTimeMillis())
        author = intent.getStringExtra("NOTE_AUTHOR") ?: "Tidak diketahui"

        // Set nilai awal teks dan gambar
        binding.editTextNote.setText(originalText)
        binding.textViewAuthor.text = "Penulis : $author"
        if (originalImageUrl.isNotEmpty()) {
            Glide.with(this).load(originalImageUrl).into(binding.imageViewNote)
        }

        // Set listener pada tombol
        binding.buttonChooseImage.setOnClickListener { selectImageFromGallery() }
        binding.buttonSave.setOnClickListener { saveNote() }
        binding.buttonRecord.setOnClickListener { startSpeechToText() }
        binding.buttonCancel.setOnClickListener { finish() }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE)
    }

    private fun saveNote() {
        val updatedText = binding.editTextNote.text.toString().trim()

        if (updatedText.isEmpty()) {
            Toast.makeText(this, "Catatan tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Periksa apakah gambar baru dipilih
        if (selectedImageUri != null) {
            uploadImageToFirebase(updatedText)
        } else {
            updateNoteInDatabase(updatedText, originalImageUrl)
        }
    }

    private fun uploadImageToFirebase(updatedText: String) {
        val storageRef = storage.reference.child("images/$noteId.jpg")
        selectedImageUri?.let { uri ->
            toggleUiInteraction(false)
            binding.progressBar.visibility = View.VISIBLE
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        updateNoteInDatabase(updatedText, downloadUrl.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal mengunggah gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                    toggleUiInteraction(true)
                    binding.progressBar.visibility = View.GONE
                }
        }
    }

    private fun updateNoteInDatabase(updatedText: String, imageUrl: String?) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val note = Note(
            id = noteId,
            text = updatedText,
            imageUrl = imageUrl,
            timestamp = originalTimestamp,
            author = author,
            userId = userId
        )

        toggleUiInteraction(false)
        binding.progressBar.visibility = View.VISIBLE
        database.child("notes").child(noteId).setValue(note)
            .addOnSuccessListener {
                Toast.makeText(this, "Catatan diperbarui", Toast.LENGTH_SHORT).show()
                navigateToMyNotesActivity()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memperbarui catatan: ${e.message}", Toast.LENGTH_SHORT).show()
                toggleUiInteraction(true)
            }
            .addOnCompleteListener {
                binding.progressBar.visibility = View.GONE
                toggleUiInteraction(true)
            }
    }

    private fun updateNoteInFirebase(noteId: String, updatedText: String, updatedImageUrl: String?) {
        val database = FirebaseDatabase.getInstance().reference.child("notes").child(noteId)
        val updatedNote = mapOf(
            "text" to updatedText,
            "imageUrl" to updatedImageUrl,
            "timestamp" to System.currentTimeMillis()
        )

        database.updateChildren(updatedNote).addOnSuccessListener {
            Toast.makeText(this, "Catatan berhasil diperbarui", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)  // Memberikan tanda ke activity sebelumnya
            finish()               // Kembali ke UserNotesActivity setelah update
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Gagal memperbarui catatan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun navigateToMyNotesActivity() {
        val intent = Intent(this, UserNotesActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startSpeechToText() {
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Silakan berbicara")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("id", "ID"))
        }
        startActivityForResult(speechRecognizerIntent, SPEECH_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let { binding.imageViewNote.setImageURI(it) }
        }

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (result != null && result.isNotEmpty()) {
                val recognizedText = result[0]
                val currentText = binding.editTextNote.text.toString()
                val newText = if (currentText.isEmpty()) recognizedText else "$currentText $recognizedText"
                binding.editTextNote.setText(newText)
                binding.editTextNote.setSelection(newText.length)
            }
        }
    }

    private fun toggleUiInteraction(enable: Boolean) {
        binding.buttonSave.isEnabled = enable
        binding.buttonChooseImage.isEnabled = enable
        binding.buttonCancel.isEnabled = enable
        binding.editTextNote.isEnabled = enable
        binding.buttonRecord.isEnabled = enable
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









//package com.example.recorder
//
//import android.app.Activity
//import android.content.Intent
//import android.net.Uri
//import android.os.Bundle
//import android.speech.RecognizerIntent
//import android.view.View
//import android.widget.Button
//import android.widget.EditText
//import android.widget.ImageView
//import android.widget.ProgressBar
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.bumptech.glide.Glide
//import com.google.firebase.database.DatabaseReference
//import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.storage.FirebaseStorage
//import java.util.Locale
//
//class EditNoteActivity : AppCompatActivity() {
//
//    private lateinit var database: DatabaseReference
//    private lateinit var storage: FirebaseStorage
//    private lateinit var editTextNote: EditText
//    private lateinit var textViewAuthor: TextView
//    private lateinit var imageViewNote: ImageView
//    private lateinit var buttonSave: Button
//    private lateinit var buttonChooseImage: Button
//    private lateinit var buttonCancel: Button
//    private lateinit var progressBar: ProgressBar
//    private lateinit var buttonRecord: Button
//    private lateinit var noteId: String
//    private lateinit var originalText: String
//    private lateinit var originalImageUrl: String
//    private lateinit var author: String
//    private var selectedImageUri: Uri? = null
//    private var originalTimestamp: Long = 0
//    private val SPEECH_REQUEST_CODE = 101
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_edit_note)
//
//        // Inisialisasi Firebase
//        database = FirebaseDatabase.getInstance().reference
//        storage = FirebaseStorage.getInstance()
//
//        // Ambil data dari Intent
//        noteId = intent.getStringExtra("NOTE_ID") ?: ""
//        originalText = intent.getStringExtra("NOTE_TEXT") ?: ""
//        originalImageUrl = intent.getStringExtra("NOTE_IMAGE_URL") ?: ""
//        originalTimestamp = intent.getLongExtra("NOTE_TIMESTAMP", System.currentTimeMillis())
//        author = intent.getStringExtra("NOTE_AUTHOR") ?: "Tidak diketahui" // Pastikan author diterima
//
//        // Inisialisasi view
//        editTextNote = findViewById(R.id.editTextNote)
//        textViewAuthor = findViewById(R.id.textViewAuthor) // Inisialisasi TextView author
//        imageViewNote = findViewById(R.id.imageViewNote)
//        buttonSave = findViewById(R.id.buttonSave)
//        buttonChooseImage = findViewById(R.id.buttonChooseImage)
//        buttonCancel = findViewById(R.id.buttonCancel)
//        progressBar = findViewById(R.id.progressBar)
//        buttonRecord = findViewById(R.id.buttonRecord)
//
//        // Set nilai awal teks dan gambar
//        editTextNote.setText(originalText)
//        textViewAuthor.text = "Author : $author" // Set teks author
//        if (originalImageUrl.isNotEmpty()) {
//            Glide.with(this).load(originalImageUrl).into(imageViewNote)
//        }
//
//        // Set listener pada tombol
//        buttonChooseImage.setOnClickListener { selectImageFromGallery() }
//        buttonSave.setOnClickListener { saveNote() }
//        buttonRecord.setOnClickListener { startSpeechToText() }
//        buttonCancel.setOnClickListener { finish() }  // Menambahkan tombol cancel untuk kembali
//    }
//
//    private fun selectImageFromGallery() {
//        val intent = Intent(Intent.ACTION_PICK)
//        intent.type = "image/*"
//        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE)
//    }
//
//    private fun saveNote() {
//        val updatedText = editTextNote.text.toString().trim()
//
//        if (updatedText.isEmpty()) {
//            Toast.makeText(this, "Catatan tidak boleh kosong", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        // Periksa apakah gambar baru dipilih
//        if (selectedImageUri != null) {
//            uploadImageToFirebase(updatedText)
//        } else {
//            updateNoteInDatabase(updatedText, originalImageUrl)
//        }
//    }
//
//
//
//    private fun uploadImageToFirebase(updatedText: String) {
//        val storageRef = storage.reference.child("images/$noteId.jpg")
//        selectedImageUri?.let { uri ->
//            toggleUiInteraction(false)
//            progressBar.visibility = View.VISIBLE
//            storageRef.putFile(uri)
//                .addOnSuccessListener {
//                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
//                        updateNoteInDatabase(updatedText, downloadUrl.toString())
//                    }
//                }
//                .addOnFailureListener { e ->
//                    Toast.makeText(this, "Gagal mengunggah gambar: ${e.message}", Toast.LENGTH_SHORT).show()
//                    toggleUiInteraction(true)
//                    progressBar.visibility = View.GONE
//                }
//        }
//    }
//
//    private fun updateNoteInDatabase(updatedText: String, imageUrl: String?) {
//        // Perbarui data catatan dengan informasi baru
//        val note = Note(
//            id = noteId,
//            text = updatedText,
//            imageUrl = imageUrl,
//            timestamp = originalTimestamp,
//            author = author
//        )
//
//        // Simpan data ke Firebase
//        noteId.let {
//            toggleUiInteraction(false)
//            progressBar.visibility = View.VISIBLE
//            database.child("notes").child(it).setValue(note)
//                .addOnSuccessListener {
//                    Toast.makeText(this, "Catatan diperbarui", Toast.LENGTH_SHORT).show()
//                    navigateToMyNotesActivity()  // Pindah ke halaman Catatan Saya setelah sukses
//                }
//                .addOnFailureListener { e ->
//                    Toast.makeText(this, "Gagal memperbarui catatan: ${e.message}", Toast.LENGTH_SHORT).show()
//                    toggleUiInteraction(true)
//                }
//                .addOnCompleteListener {
//                    progressBar.visibility = View.GONE
//                    toggleUiInteraction(true)
//                }
//        }
//    }
//
//    private fun navigateToMyNotesActivity() {
//        val intent = Intent(this, UserNotesActivity::class.java)
//        startActivity(intent)  // Mengarahkan pengguna ke halaman Catatan Saya
//        finish()  // Menutup EditNoteActivity
//    }
//
//    private fun startSpeechToText() {
//        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
//            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//            putExtra(RecognizerIntent.EXTRA_PROMPT, "Silakan berbicara")
//            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("id", "ID"))
//        }
//        startActivityForResult(speechRecognizerIntent, SPEECH_REQUEST_CODE)
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
//            selectedImageUri = data?.data
//            selectedImageUri?.let { imageViewNote.setImageURI(it) }
//        }
//
//        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
//            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
//            if (result != null && result.isNotEmpty()) {
//                val recognizedText = result[0]
//                val currentText = editTextNote.text.toString()
//                val newText = if (currentText.isEmpty()) recognizedText else "$currentText $recognizedText"
//                editTextNote.setText(newText)
//                editTextNote.setSelection(newText.length)
//            }
//        }
//    }
//
//    private fun toggleUiInteraction(enable: Boolean) {
//        buttonSave.isEnabled = enable
//        buttonChooseImage.isEnabled = enable
//        buttonCancel.isEnabled = enable
//        editTextNote.isEnabled = enable
//        buttonRecord.isEnabled = enable
//    }
//
//    companion object {
//        private const val REQUEST_CODE_SELECT_IMAGE = 100
//    }
//}
//
//// Data class Note untuk mencakup seluruh informasi catatan
//data class Note(
//    var id: String = "",
//    var text: String = "",
//    var imageUrl: String? = null,
//    val timestamp: Long = System.currentTimeMillis(),
//    val author: String = ""
//)
