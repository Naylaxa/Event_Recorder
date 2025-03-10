package com.example.recorder

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recorder.databinding.ActivityUserItemNotesBinding
import java.text.SimpleDateFormat
import java.util.Locale

class UserNotesAdapter(
    private val noteList: List<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onEditClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit
) : RecyclerView.Adapter<UserNotesAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(private val binding: ActivityUserItemNotesBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.textViewNote.text = "Catatan : ${note.text}"
            binding.textViewDate.text = "Tanggal : ${formatDate(note.timestamp)}"
            binding.authorTextView.text = "Penulis : ${note.author ?: "Tidak diketahui"}"

            // Set image using Glide or a placeholder
            if (!note.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.imageViewNote.context).load(note.imageUrl).into(binding.imageViewNote)
            } else {
                binding.imageViewNote.setImageResource(R.drawable.image_background) // Default image
            }

            // Set click listeners for Edit and Delete buttons
            binding.imageButtonEdit.setOnClickListener {
                onEditClick(note)
            }
            binding.imageButtonDelete.setOnClickListener {
                showDeleteConfirmationDialog(binding.root.context, note)
            }

            // Set click listener for "See Details"
            binding.detailTextNote.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, DetailNoteActivity::class.java).apply {
                    putExtra("NOTE_TEXT", note.text)
                    putExtra("NOTE_DATE", binding.textViewDate.text)
                    putExtra("NOTE_IMAGE", note.imageUrl)
                    putExtra("NOTE_AUTHOR", note.author)
                }
                context.startActivity(intent)
            }
        }

        // Helper function to format date
        private fun formatDate(timestamp: Long?): String {
            return timestamp?.let {
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                dateFormat.format(it)
            } ?: "Tanggal tidak diketahui"
        }

        // Show confirmation dialog before deleting
        private fun showDeleteConfirmationDialog(context: Context, note: Note) {
            AlertDialog.Builder(context)
                .setTitle("Konfirmasi Penghapusan")
                .setMessage("Apakah Anda yakin ingin menghapus catatan ini?")
                .setPositiveButton("Hapus") { dialog, _ ->
                    onDeleteClick(note)
                    dialog.dismiss()
                }
                .setNegativeButton("Batal") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ActivityUserItemNotesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(noteList[position])
    }

    override fun getItemCount(): Int = noteList.size
}
