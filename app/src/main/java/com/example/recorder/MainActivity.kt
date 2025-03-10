package com.example.recorder

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.recorder.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Firebase Auth dan Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Periksa pengguna yang sedang login
        val currentUser: FirebaseUser? = auth.currentUser


        if (currentUser != null) {
            val userId = currentUser.uid
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val username = document.getString("name") ?: "Pengguna"
                    binding.textViewWelcome.text = "Selamat datang, $username!"
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal mengambil data pengguna", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Set tombol untuk menambahkan dan melihat catatan
        binding.buttonAddNotes.setOnClickListener {
            val intent = Intent(this, MainActivityAdd::class.java)
            startActivity(intent)
        }

        binding.buttonViewAllNotes.setOnClickListener {
            val intent = Intent(this, NotesActivity::class.java)
            startActivity(intent)
        }

        binding.buttonViewNotesUsers.setOnClickListener {
            startActivity(Intent(this, UserNotesActivity::class.java))
        }

        binding.buttonSetting.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

    }
}