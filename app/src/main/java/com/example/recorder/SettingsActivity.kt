package com.example.recorder

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.recorder.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Firebase Auth dan Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.progressBar.visibility = View.GONE // Sembunyikan ProgressBar di awal

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
            navigateToLogin()
        }

        binding.buttonLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        binding.buttonDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }

        binding.buttonBack.setOnClickListener {
            // Kembali ke MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Hapus aktivitas ini dari stack agar tidak bisa kembali ke halaman ini
        }
    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Konfirmasi Keluar")
        builder.setMessage("Apakah anda yakin ingin keluar?")
        builder.setPositiveButton("Ya") { _, _ -> logout() }
        builder.setNegativeButton("Tidak") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun logout() {
        setButtonsEnabled(false)
        binding.progressBar.visibility = View.VISIBLE

        auth.signOut()

        // Periksa jika pengguna berhasil keluar
        if (auth.currentUser == null) {
            Toast.makeText(this, "Berhasil keluar", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        } else {
            Toast.makeText(this, "Gagal keluar", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            setButtonsEnabled(true)
        }
    }

    private fun showDeleteAccountConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Konfirmasi Hapus Akun")
        builder.setMessage("Apakah anda yakin ingin menghapus akun ini?")
        builder.setPositiveButton("Ya") { _, _ -> deleteAccount() }
        builder.setNegativeButton("Tidak") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun deleteAccount() {
        setButtonsEnabled(false)
        binding.progressBar.visibility = View.VISIBLE
        val currentUser: FirebaseUser? = auth.currentUser

        currentUser?.delete()?.addOnCompleteListener { task ->
            binding.progressBar.visibility = View.GONE
            setButtonsEnabled(true)

            if (task.isSuccessful) {
                Toast.makeText(this, "Akun berhasil dihapus", Toast.LENGTH_SHORT).show()
                navigateToLogin()
            } else {
                Toast.makeText(this, "Gagal menghapus akun", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.buttonLogout.isEnabled = enabled
        binding.buttonDeleteAccount.isEnabled = enabled
        binding.buttonBack.isEnabled = enabled
    }
}
