package com.example.recorder

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class DetailNoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_note)

        // Mengambil data dari intent
        val noteText = intent.getStringExtra("NOTE_TEXT") ?: "No Text" // Default jika null
        val noteDate = intent.getStringExtra("NOTE_DATE") ?: "No Date" // Default jika null
        val noteImage = intent.getStringExtra("NOTE_IMAGE")
        val noteAuthor = intent.getStringExtra("NOTE_AUTHOR") ?: "Tidak diketahui" // Ambil penulis

        // Debugging untuk melihat nilai penulis
        Log.d("DetailNoteActivity", "Note Author: $noteAuthor")

        // Menampilkan data pada UI
        findViewById<TextView>(R.id.textViewNote).text = "Catatan : $noteText"  // Menampilkan teks catatan
        findViewById<TextView>(R.id.textViewDate).text = "$noteDate"  // Menampilkan tanggal catatan
        findViewById<TextView>(R.id.authorTextView).text = "Penulis : $noteAuthor" // Menampilkan penulis

        // Load image menggunakan Glide
        val imageViewNote = findViewById<ImageView>(R.id.imageViewNote)
        if (!noteImage.isNullOrEmpty()) {
            Glide.with(this).load(noteImage).into(imageViewNote)  // Menampilkan gambar jika ada
        } else {
            imageViewNote.setImageResource(R.drawable.image_background) // Placeholder jika tidak ada gambar
        }

        // Menangani klik tombol Back
        val buttonBack = findViewById<Button>(R.id.buttonBack)
        buttonBack.setOnClickListener {
            onBackPressed()  // Kembali ke halaman sebelumnya
        }
    }
}
