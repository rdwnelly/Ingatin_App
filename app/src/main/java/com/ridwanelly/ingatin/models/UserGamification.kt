package com.ridwanelly.ingatin.models

import com.google.firebase.firestore.DocumentId

data class UserGamification(
    @DocumentId
    val userId: String? = null,
    val points: Long = 0,
    val level: Int = 1,
    val onTimeSubmissions: Long = 0, // Penghitung tugas yang selesai tepat waktu
    val badges: List<String> = emptyList() // Daftar ID badge yang dimiliki
) {
    // Konstruktor kosong untuk Firestore
    constructor() : this(null, 0, 1, 0, emptyList())
}