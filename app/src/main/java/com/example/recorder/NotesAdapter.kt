package com.example.recorder

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recorder.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.Locale

// NotesAdapter class
class NotesAdapter(
    private val noteList: List<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onEditClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewNote: TextView = itemView.findViewById(R.id.textViewNote)
        val imageViewNote: ImageView = itemView.findViewById(R.id.imageViewNote)
        //        val buttonEdit: ImageButton = itemView.findViewById(R.id.imageButtonEdit)
//        val buttonDelete: ImageButton = itemView.findViewById(R.id.imageButtonDelete)
        val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        val textViewSeeDetails: TextView = itemView.findViewById(R.id.detailTextNote)
        val authorTextView: TextView = itemView.findViewById(R.id.authorTextView) // TextView for author

        fun bind(note: Note) {
            textViewNote.text = note.text

            // Set klik listener pada tombol edit, delete, dan item catatan
//            buttonEdit.setOnClickListener { onEditClick(note) }
//            buttonDelete.setOnClickListener { onDeleteClick(note) }
            itemView.setOnClickListener { onNoteClick(note) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = noteList[position]

        // Set note text
        holder.textViewNote.text = "Catatan : ${note.text}"

        // Set author or "Unknown" if not provided
        holder.authorTextView.text = if (!note.author.isNullOrEmpty()) {
            "Penulis : ${note.author}"
        } else {
            "Penulis : Tidak diketahui"
        }

        // Format timestamp to readable date
        note.timestamp?.let { timeInMillis ->
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = dateFormat.format(timeInMillis)
            holder.textViewDate.text = "Tanggal : $date"
        }

        // Load image with Glide if imageUrl is available
        if (!note.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context).load(note.imageUrl).into(holder.imageViewNote)
        } else {
            holder.imageViewNote.setImageResource(R.drawable.image_background) // Placeholder
        }

        // Handle note item click
        holder.itemView.setOnClickListener {
            onNoteClick(note)
        }

        // Handle edit button click
//        holder.buttonEdit.setOnClickListener {
//            onEditClick(note)
//        }

        // Handle delete button click with confirmation dialog
//        holder.buttonDelete.setOnClickListener {
//            showDeleteConfirmationDialog(holder.itemView.context, note)
//        }

        // "See Details" click opens DetailNoteActivity with note details
        holder.textViewSeeDetails.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DetailNoteActivity::class.java).apply {
                putExtra("NOTE_TEXT", note.text)
                putExtra("NOTE_DATE", holder.textViewDate.text)
                putExtra("NOTE_IMAGE", note.imageUrl)
                putExtra("NOTE_AUTHOR", note.author) // Pass author info to detail
            }
            context.startActivity(intent)
        }
    }

    // Method to show delete confirmation dialog
    private fun showDeleteConfirmationDialog(context: Context, note: Note) {
        AlertDialog.Builder(context)
            .setTitle("Konfirmasi Penghapusan")
            .setMessage("Apakah anda yakin ingin menghapus catatan ini?")
            .setPositiveButton("Hapus") { dialog, _ ->
                onDeleteClick(note) // Trigger delete callback
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    override fun getItemCount(): Int = noteList.size
}