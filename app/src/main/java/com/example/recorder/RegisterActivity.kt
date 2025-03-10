package com.example.recorder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val buttonRegister = findViewById<Button>(R.id.buttonRegister)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val editTextName = findViewById<EditText>(R.id.editTextName)
        val editTextEmail = findViewById<EditText>(R.id.editTextEmail)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)

        buttonRegister.setOnClickListener {
            val name = editTextName.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("RegisterActivity", "Registrasi berhasil")
                            val user = auth.currentUser
                            val userId = user?.uid

                            // Simpan data pengguna ke Firestore
                            if (userId != null) {
                                val userData = hashMapOf("name" to name)
                                db.collection("users").document(userId).set(userData)
                                    .addOnSuccessListener {
                                        Log.d("RegisterActivity", "User data saved successfully")
                                        Toast.makeText(
                                            this,
                                            "Registrasi berhasil",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // Lakukan pembaruan profil asinkron
                                        val profileUpdates = UserProfileChangeRequest.Builder()
                                            .setDisplayName(name)
                                            .build()
                                        user.updateProfile(profileUpdates)
                                            .addOnCompleteListener { updateTask ->
                                                if (updateTask.isSuccessful) {
                                                    Log.d(
                                                        "RegisterActivity",
                                                        "Profile updated successfully"
                                                    )
                                                } else {
                                                    Log.e(
                                                        "RegisterActivity",
                                                        "Profile update failed"
                                                    )
                                                }
                                            }

                                        // Pindah ke LoginActivity setelah data disimpan
                                        val intent = Intent(this, LoginActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(
                                            this,
                                            "Gagal menyimpan data pengguna: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        Log.e("RegisterActivity", "Error saving user data", e)
                                    }
                            }
                        } else {
                            // Tangani kesalahan jika registrasi gagal
                            handleRegistrationError(task.exception)
                        }
                    }
            } else {
                Toast.makeText(this, "Lengkapi semua kolom", Toast.LENGTH_SHORT).show()
            }
        }

        buttonLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun handleRegistrationError(exception: Exception?) {
        val errorMessage = when (exception) {
            is FirebaseAuthUserCollisionException -> {
                Toast.makeText(this, "Email sudah terdaftar. Silakan login.", Toast.LENGTH_SHORT).show()
                // Arahkan ke LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                return // Hentikan eksekusi lebih lanjut
            }
            else -> "Registrasi gagal: ${exception?.message}"
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }
}






//package com.example.recorder
//
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log
//import android.widget.Button
//import android.widget.EditText
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseAuthUserCollisionException
//import com.google.firebase.auth.UserProfileChangeRequest
//import com.google.firebase.firestore.FirebaseFirestore
//
//class RegisterActivity : AppCompatActivity() {
//
//    private lateinit var auth: FirebaseAuth
//    private lateinit var db: FirebaseFirestore
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_register)
//
//        auth = FirebaseAuth.getInstance()
//        db = FirebaseFirestore.getInstance()
//
//        val buttonRegister = findViewById<Button>(R.id.buttonRegister)
//        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
//        val editTextName = findViewById<EditText>(R.id.editTextName)
//        val editTextEmail = findViewById<EditText>(R.id.editTextEmail)
//        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
//
//        buttonRegister.setOnClickListener {
//            val name = editTextName.text.toString()
//            val email = editTextEmail.text.toString()
//            val password = editTextPassword.text.toString()
//
//            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
//                auth.createUserWithEmailAndPassword(email, password)
//                    .addOnCompleteListener { task ->
//                        if (task.isSuccessful) {
//                            Log.d("RegisterActivity", "Registrasi berhasil")
//                            val user = auth.currentUser
//                            val profileUpdates = UserProfileChangeRequest.Builder()
//                                .setDisplayName(name)
//                                .build()
//
//                            // Memperbarui profil pengguna dan menyimpan data pengguna ke Firestore
//                            user?.updateProfile(profileUpdates)?.addOnCompleteListener { updateTask ->
//                                if (updateTask.isSuccessful) {
//                                    val userId = user.uid
//                                    val userData = hashMapOf("name" to name)
//
//                                    db.collection("users").document(userId).set(userData)
//                                        .addOnSuccessListener {
//                                            Toast.makeText(this, "Registrasi berhasil", Toast.LENGTH_SHORT).show()
//                                            Log.d("RegisterActivity", "User data saved successfully")
//                                            // Pindah ke LoginActivity setelah data pengguna disimpan
//                                            val intent = Intent(this, LoginActivity::class.java)
//                                            startActivity(intent)
//                                            finish()
//                                        }
//                                        .addOnFailureListener { e ->
//                                            Toast.makeText(this, "Gagal menyimpan data pengguna: ${e.message}", Toast.LENGTH_SHORT).show()
//                                            Log.e("RegisterActivity", "Error saving user data", e)
//                                        }
//                                } else {
//                                    Toast.makeText(this, "Gagal memperbarui profil pengguna", Toast.LENGTH_SHORT).show()
//                                }
//                            }
//                        } else {
//                            // Tangani kesalahan jika registrasi gagal
//                            handleRegistrationError(task.exception)
//                        }
//                    }
//            } else {
//                Toast.makeText(this, "Lengkapi semua kolom", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        buttonLogin.setOnClickListener {
//            val intent = Intent(this, LoginActivity::class.java)
//            startActivity(intent)
//        }
//    }
//
//    private fun handleRegistrationError(exception: Exception?) {
//        val errorMessage = when (exception) {
//            is FirebaseAuthUserCollisionException -> {
//                Toast.makeText(this, "Email sudah terdaftar. Silakan login.", Toast.LENGTH_SHORT).show()
//                // Arahkan ke LoginActivity
//                val intent = Intent(this, LoginActivity::class.java)
//                startActivity(intent)
//                finish()
//                return // Hentikan eksekusi lebih lanjut
//            }
//            else -> "Registrasi gagal: ${exception?.message}"
//        }
//        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
//    }
//}
