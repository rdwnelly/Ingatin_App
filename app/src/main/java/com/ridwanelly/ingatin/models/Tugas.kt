package com.ridwanelly.ingatin.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Tugas(
    @DocumentId
    val id: String? = null,
    val namaTugas: String? = null,
    val deskripsi: String? = null,
    val deadline: Timestamp? = null, // Menggunakan Timestamp Firebase untuk presisi
    var isCompleted: Boolean = false,
    val matkulId: String? = null, // Kunci untuk menghubungkan ke MataKuliah
    val userId: String? = null,

    // --- TAMBAHAN BARU ---
    // Menyimpan waktu kapan tugas ditandai selesai.
    // Ini akan digunakan untuk fitur Analisis Produktivitas Mingguan.
    val completedAt: Timestamp? = null
    // --- AKHIR TAMBAHAN ---
) {
    // Konstruktor kosong yang dibutuhkan oleh Firestore
    constructor() : this(null, null, null, null, false, null, null, null)
}