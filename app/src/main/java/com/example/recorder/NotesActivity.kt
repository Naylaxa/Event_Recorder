package com.example.recorder

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.DatePicker
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recorder.databinding.ActivityNotesBinding
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class NotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotesBinding
    private lateinit var database: DatabaseReference
    private lateinit var noteList: MutableList<Note>
    private lateinit var filteredNoteList: MutableList<Note>
    private lateinit var notesAdapter: NotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi RecyclerView
        binding.recyclerViewAllNotes.layoutManager = LinearLayoutManager(this)
        noteList = mutableListOf()
        filteredNoteList = mutableListOf()
        setupRecyclerView()

        // Menghubungkan ke Firebase Database
        database = FirebaseDatabase.getInstance().getReference("notes")

        // Mengambil data dari Firebase
        fetchNotesFromFirebase()

        // Mengatur aksi untuk EditText pencarian teks
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val searchText = s.toString().trim()
                searchNotesByText(searchText) // Langsung mencari setelah teks berubah
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Mengatur aksi untuk EditText tanggal untuk membuka DatePickerDialog
        binding.editTextSearchDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            filteredNoteList,
            onNoteClick = { note -> /* Aksi ketika catatan diklik (jika perlu) */ },
            onEditClick = { note ->
                // Pindah ke EditNoteActivity dengan data catatan
                val intent = Intent(this, EditNoteActivity::class.java)
                intent.putExtra("NOTE_ID", note.id)
                intent.putExtra("NOTE_TEXT", note.text)
                intent.putExtra("NOTE_IMAGE_URL", note.imageUrl)
                intent.putExtra("NOTE_TIMESTAMP", note.timestamp)
                intent.putExtra("NOTE_AUTHOR", note.author) // Pastikan author dikirim
                startActivity(intent)
            },
            onDeleteClick = { note -> deleteNote(note) }
        )
        binding.recyclerViewAllNotes.adapter = notesAdapter
    }

    private fun fetchNotesFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                noteList.clear()
                for (noteSnapshot in snapshot.children) {
                    val note = noteSnapshot.getValue(Note::class.java)
                    note?.let {
                        it.id = noteSnapshot.key ?: ""
                        noteList.add(it)
                    }
                }

                // Urutkan noteList berdasarkan timestamp secara descending
                noteList.sortByDescending { it.timestamp }

                // Menampilkan semua catatan
                filteredNoteList.clear()
                filteredNoteList.addAll(noteList)

                notesAdapter.notifyDataSetChanged()

                // Tampilkan "Belum Ada Catatan" jika daftar catatan kosong
                binding.textViewNoNotes.visibility = if (noteList.isEmpty()) {
                    binding.textViewNoNotes.text = "Belum Ada Catatan"
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotesActivity", "Gagal mengambil data: ${error.message}")
                Toast.makeText(this@NotesActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Membuat DatePickerDialog
        val datePickerDialog = DatePickerDialog(this, { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            // Mengatur EditText dengan tanggal yang dipilih
            val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            binding.editTextSearchDate.setText(formattedDate) // Mengatur tanggal dalam format yyyy-MM-dd

            // Panggil searchNotesByDate secara otomatis setelah memilih tanggal
            searchNotesByDate(formattedDate)
        }, year, month, day)

        datePickerDialog.show() // Tampilkan dialog
    }

    // Fungsi pencarian catatan berdasarkan teks
    private fun searchNotesByText(query: String) {
        filteredNoteList.clear()
        if (query.isEmpty()) {
            filteredNoteList.addAll(noteList)
        } else {
            for (note in noteList) {
                if (note.text.contains(query, ignoreCase = true)) {
                    filteredNoteList.add(note)
                }
            }
        }

        binding.textViewNoNotes.visibility = if (filteredNoteList.isEmpty()) {
            View.VISIBLE // Tampilkan pesan tidak ditemukan
        } else {
            View.GONE // Sembunyikan pesan jika ada catatan
        }

        notesAdapter.notifyDataSetChanged()
    }

    private fun searchNotesByDate(date: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate: Date

        try {
            formattedDate = sdf.parse(date) ?: return
        } catch (e: Exception) {
            Toast.makeText(this, "Format tanggal salah", Toast.LENGTH_SHORT).show()
            return
        }

        val timestamp = formattedDate.time
        filteredNoteList.clear()

        for (note in noteList) {
            if (note.timestamp >= timestamp && note.timestamp < timestamp + 86400000) {
                filteredNoteList.add(note)
            }
        }

        binding.textViewNoNotes.visibility = if (filteredNoteList.isEmpty()) {
            View.VISIBLE // Tampilkan pesan tidak ditemukan
        } else {
            View.GONE // Sembunyikan pesan jika ada catatan
        }

        notesAdapter.notifyDataSetChanged()
    }

    private fun deleteNote(note: Note) {
        note.id?.let { noteId ->
            database.child(noteId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Catatan dihapus", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal menghapus catatan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

}
