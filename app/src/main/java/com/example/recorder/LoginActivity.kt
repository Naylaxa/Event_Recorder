package com.example.recorder

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonLogin: Button
    private lateinit var buttonRegister: Button
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonRegister = findViewById(R.id.buttonRegister)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        progressBar = findViewById(R.id.progressBar)

        progressBar.visibility = View.GONE // Sembunyikan ProgressBar di awal

        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            // Validasi format email
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Masukkan email dan kata sandi", Toast.LENGTH_SHORT).show()
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email tidak valid", Toast.LENGTH_SHORT).show()
            } else {
                showLoading(true)

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        showLoading(false)
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                // Ambil nama pengguna dari Firebase Database berdasarkan userId
                                val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                                userRef.child("userName").get().addOnSuccessListener { dataSnapshot ->
                                    val userNameFromFirebase = dataSnapshot.value as? String ?: "Unknown User"

                                    // Simpan nama pengguna di SharedPreferences
                                    saveUserNameToPreferences(userNameFromFirebase)

                                    // Arahkan ke MainActivity setelah login berhasil
                                    Toast.makeText(this, "Berhasil masuk", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }.addOnFailureListener {
                                    Toast.makeText(this, "Gagal mengambil nama pengguna.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            // Tangani kesalahan lebih spesifik
                            handleLoginError(task.exception)
                        }
                    }
            }
        }

        buttonRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun handleLoginError(exception: Exception?) {
        when (exception) {
            is FirebaseAuthInvalidCredentialsException -> {
                Toast.makeText(this, "Email atau password salah", Toast.LENGTH_SHORT).show()
            }
            is FirebaseAuthInvalidUserException -> {
                Toast.makeText(this, "Akun tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Login gagal: ${exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserNameToPreferences(userName: String) {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("userName", userName)
        editor.apply()
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            // Tampilkan ProgressBar dan nonaktifkan input
            progressBar.visibility = View.VISIBLE
            buttonLogin.isEnabled = false
            buttonRegister.isEnabled = false
            editTextEmail.isEnabled = false
            editTextPassword.isEnabled = false
        } else {
            // Sembunyikan ProgressBar dan aktifkan kembali input
            progressBar.visibility = View.GONE
            buttonLogin.isEnabled = true
            buttonRegister.isEnabled = true
            editTextEmail.isEnabled = true
            editTextPassword.isEnabled = true
        }
    }
}
