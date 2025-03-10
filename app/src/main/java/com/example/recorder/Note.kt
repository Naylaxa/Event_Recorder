// File: Note.kt
package com.example.recorder

data class Note(
    var id: String = "",
    var text: String = "",
    var imageUrl: String? = null,
    var author: String = "",
    var userId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)