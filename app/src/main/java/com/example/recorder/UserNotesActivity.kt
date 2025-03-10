package com.example.recorder

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recorder.databinding.ActivityUserNotesBinding
import android.text.Editable
import android.text.TextWatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class UserNotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserNotesBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var userNotesAdapter: UserNotesAdapter
    private val notesList = mutableListOf<Note>()
    private val filteredNotesList = mutableListOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("notes")

        binding.recyclerViewUserNotes.layoutManager = LinearLayoutManager(this)
        setupRecyclerView()

        // Tambahkan TextWatcher untuk pencarian langsung
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Tidak diperlukan
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchNotesByText(s.toString().trim())
            }

            override fun afterTextChanged(s: Editable?) {
                // Tidak diperlukan
            }
        })

        binding.editTextSearchDate.setOnClickListener {
            showDatePickerDialog()
        }

        loadUserNotes()
    }


    private val REQUEST_CODE_EDIT_NOTE = 1001

    // Di setupRecyclerView(), panggil EditNoteActivity dengan request code
    private fun setupRecyclerView() {
        userNotesAdapter = UserNotesAdapter(
            filteredNotesList,
            onNoteClick = { /* Aksi saat catatan diklik */ },
            onEditClick = { note ->
                val intent = Intent(this, EditNoteActivity::class.java).apply {
                    putExtra("NOTE_ID", note.id)
                    putExtra("NOTE_TEXT", note.text)
                    putExtra("NOTE_IMAGE_URL", note.imageUrl)
                    putExtra("NOTE_TIMESTAMP", note.timestamp)
                    putExtra("NOTE_AUTHOR", note.author)
                }
                startActivityForResult(intent, REQUEST_CODE_EDIT_NOTE)
            },
            onDeleteClick = { note -> deleteNote(note) }
        )
        binding.recyclerViewUserNotes.adapter = userNotesAdapter
    }

    // Tambahkan onActivityResult untuk memuat ulang catatan setelah edit
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_EDIT_NOTE && resultCode == RESULT_OK) {
            loadUserNotes()  // Refresh daftar catatan setelah kembali dari EditNoteActivity
        }
    }


    private fun loadUserNotes() {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            database.orderByChild("userId").equalTo(userId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        notesList.clear()
                        for (noteSnapshot in snapshot.children) {
                            val note = noteSnapshot.getValue(Note::class.java)
                            note?.let {
                                it.id = noteSnapshot.key ?: "" // Ambil ID dari key snapshot
                                notesList.add(it)  // Menambahkan note yang ditemukan ke notesList
                            }
                        }

                        notesList.sortByDescending { it.timestamp }
                        filteredNotesList.clear()
                        filteredNotesList.addAll(notesList)

                        userNotesAdapter.notifyDataSetChanged()  // Mengupdate tampilan RecyclerView

                        if (notesList.isEmpty()) {
                            // Menampilkan pesan "Belum Ada Catatan" jika tidak ada catatan
                            binding.textViewNoNotes.visibility = View.VISIBLE
                            binding.textViewNoNotes.text = "Belum Ada Catatan"
                        } else {
                            // Menyembunyikan pesan jika ada catatan
                            binding.textViewNoNotes.visibility = View.GONE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@UserNotesActivity, "Gagal memuat catatan", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            Toast.makeText(this, "Pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }


    private fun searchNotesByText(searchText: String) {
        filteredNotesList.clear()
        filteredNotesList.addAll(notesList.filter { it.text.contains(searchText, ignoreCase = true) })
        userNotesAdapter.notifyDataSetChanged()

        if (filteredNotesList.isEmpty()) {
            // Tampilkan pesan "Catatan Tidak Ditemukan" jika hasil pencarian kosong
            binding.textViewNoNotes.visibility = View.VISIBLE
            binding.textViewNoNotes.text = "Catatan Tidak Ditemukan"
        } else {
            // Menyembunyikan pesan jika ada hasil pencarian
            binding.textViewNoNotes.visibility = View.GONE
        }
    }

    private fun deleteNote(note: Note) {
        database.child(note.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Catatan dihapus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error: ${e.message}")
                Toast.makeText(this, "Gagal menghapus catatan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val selectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.editTextSearchDate.setText(dateFormatter.format(selectedDate.time))
                searchNotesByDate(selectedDate.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun searchNotesByDate(date: Date) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateStr = formatter.format(date)

        filteredNotesList.clear()
        filteredNotesList.addAll(notesList.filter { note ->
            val noteDateStr = formatter.format(Date(note.timestamp))
            noteDateStr == selectedDateStr
        })
        userNotesAdapter.notifyDataSetChanged()

        if (filteredNotesList.isEmpty()) {
            // Tampilkan pesan "Catatan Tidak Ditemukan" jika hasil pencarian berdasarkan tanggal kosong
            binding.textViewNoNotes.visibility = View.VISIBLE
            binding.textViewNoNotes.text = "Catatan Tidak Ditemukan"
        } else {
            binding.textViewNoNotes.visibility = View.GONE
        }
    }
}



//package com.example.recorder
//
//import android.app.DatePickerDialog
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log
//import android.view.View
//import android.widget.DatePicker
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.recorder.databinding.ActivityUserNotesBinding
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.*
//import java.text.SimpleDateFormat
//import java.util.*
//
//class UserNotesActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityUserNotesBinding
//    private lateinit var auth: FirebaseAuth
//    private lateinit var database: DatabaseReference
//    private lateinit var userNotesAdapter: UserNotesAdapter
//    private val notesList = mutableListOf<Note>()
//    private val filteredNotesList = mutableListOf<Note>()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityUserNotesBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        auth = FirebaseAuth.getInstance()
//        database = FirebaseDatabase.getInstance().reference.child("notes")
//
//        binding.recyclerViewUserNotes.layoutManager = LinearLayoutManager(this)
//        setupRecyclerView()
//
//        binding.buttonSearchText.setOnClickListener {
//            val searchText = binding.editTextSearch.text.toString().trim()
//            searchNotesByText(searchText)
//        }
//
//        binding.editTextSearchDate.setOnClickListener {
//            showDatePickerDialog()
//        }
//
//        loadUserNotes()
//    }
//
//    private fun setupRecyclerView() {
//        userNotesAdapter = UserNotesAdapter(
//            filteredNotesList,
//            onNoteClick = { note -> /* Aksi ketika catatan diklik jika perlu */ },
//            onEditClick = { note ->
//                val intent = Intent(this, EditNoteActivity::class.java).apply {
//                    putExtra("NOTE_ID", note.id)
//                    putExtra("NOTE_TEXT", note.text)
//                    putExtra("NOTE_IMAGE_URL", note.imageUrl)
//                    putExtra("NOTE_TIMESTAMP", note.timestamp)
//                    putExtra("NOTE_AUTHOR", note.author)
//                }
//                startActivity(intent)
//            },
//            onDeleteClick = { note -> deleteNote(note) }
//        )
//        binding.recyclerViewUserNotes.adapter = userNotesAdapter
//    }
//
//    private fun loadUserNotes() {
//        val userId = auth.currentUser?.uid
//
//        if (userId != null) {
//            database.orderByChild("userId").equalTo(userId)
//                .addValueEventListener(object : ValueEventListener {
//                    override fun onDataChange(snapshot: DataSnapshot) {
//                        notesList.clear()
//                        for (noteSnapshot in snapshot.children) {
//                            val note = noteSnapshot.getValue(Note::class.java)
//                            note?.let {
//                                it.id = noteSnapshot.key ?: ""
//                                notesList.add(it)
//                            }
//                        }
//
//                        notesList.sortByDescending { it.timestamp }
//                        filteredNotesList.clear()
//                        filteredNotesList.addAll(notesList)
//
//                        userNotesAdapter.notifyDataSetChanged()
//
//                        if (notesList.isEmpty()) {
//                            // Menampilkan pesan "Belum Ada Catatan" jika tidak ada catatan
//                            binding.textViewNoNotes.visibility = View.VISIBLE
//                            binding.textViewNoNotes.text = "Belum Ada Catatan"
//                        } else {
//                            // Menyembunyikan pesan jika ada catatan
//                            binding.textViewNoNotes.visibility = View.GONE
//                        }
//                    }
//
//                    override fun onCancelled(error: DatabaseError) {
//                        Toast.makeText(this@UserNotesActivity, "Gagal memuat catatan", Toast.LENGTH_SHORT).show()
//                    }
//                })
//        } else {
//            Toast.makeText(this, "Pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun searchNotesByText(searchText: String) {
//        filteredNotesList.clear()
//        filteredNotesList.addAll(notesList.filter { it.text.contains(searchText, ignoreCase = true) })
//        userNotesAdapter.notifyDataSetChanged()
//
//        if (filteredNotesList.isEmpty()) {
//            // Tampilkan pesan "Catatan Tidak Ditemukan" jika hasil pencarian kosong
//            binding.textViewNoNotes.visibility = View.VISIBLE
//            binding.textViewNoNotes.text = "Catatan Tidak Ditemukan"
//        } else {
//            // Menyembunyikan pesan jika ada hasil pencarian
//            binding.textViewNoNotes.visibility = View.GONE
//        }
//    }
//
//    private fun deleteNote(note: Note) {
//        database.child(note.id).removeValue()
//            .addOnSuccessListener {
//                Toast.makeText(this, "Catatan dihapus", Toast.LENGTH_SHORT).show()
//            }
//            .addOnFailureListener { e ->
//                Log.e("Firebase", "Error: ${e.message}")
//                Toast.makeText(this, "Gagal menghapus catatan", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//    private fun showDatePickerDialog() {
//        val calendar = Calendar.getInstance()
//        val datePickerDialog = DatePickerDialog(
//            this,
//            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
//                val selectedDate = Calendar.getInstance().apply {
//                    set(Calendar.YEAR, year)
//                    set(Calendar.MONTH, month)
//                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
//                }
//                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                binding.editTextSearchDate.setText(dateFormatter.format(selectedDate.time))
//                searchNotesByDate(selectedDate.time)
//            },
//            calendar.get(Calendar.YEAR),
//            calendar.get(Calendar.MONTH),
//            calendar.get(Calendar.DAY_OF_MONTH)
//        )
//        datePickerDialog.show()
//    }
//
//    private fun searchNotesByDate(date: Date) {
//        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//        val selectedDateStr = formatter.format(date)
//
//        filteredNotesList.clear()
//        filteredNotesList.addAll(notesList.filter { note ->
//            val noteDateStr = formatter.format(Date(note.timestamp))
//            noteDateStr == selectedDateStr
//        })
//        userNotesAdapter.notifyDataSetChanged()
//
//        if (filteredNotesList.isEmpty()) {
//            // Tampilkan pesan "Catatan Tidak Ditemukan" jika hasil pencarian berdasarkan tanggal kosong
//            binding.textViewNoNotes.visibility = View.VISIBLE
//            binding.textViewNoNotes.text = "Catatan Tidak Ditemukan"
//        } else {
//            binding.textViewNoNotes.visibility = View.GONE
//        }
//    }
//}