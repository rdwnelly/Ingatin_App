package com.ridwanelly.ingatin.models

import com.google.firebase.firestore.DocumentId

// data class adalah cara ringkas di Kotlin untuk membuat class yang tujuannya hanya untuk menyimpan data.
// @DocumentId akan secara otomatis diisi oleh Firestore dengan ID dokumennya. Ini sangat berguna.
data class MataKuliah(
    @DocumentId
    val id: String? = null, // ID unik dari Firestore
    val namaMatkul: String? = null,
    val hari: String? = null,
    val jamMulai: String? = null,
    val jamSelesai: String? = null,
    val ruangan: String? = null,
    val dosen: String? = null,
    val userId: String? = null // Untuk menyimpan ID pengguna yang membuat jadwal ini
) {
    // Konstruktor kosong diperlukan oleh Firestore untuk proses deserialisasi (mengubah data dari database menjadi objek).
    constructor() : this(null, null, null, null, null, null, null, null)
}