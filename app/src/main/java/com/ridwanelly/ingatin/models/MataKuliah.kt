package com.ridwanelly.ingatin.models

import com.google.firebase.firestore.DocumentId

// data class adalah cara ringkas di Kotlin untuk membuat class yang tujuannya hanya untuk menyimpan data.
data class MataKuliah(
    @DocumentId
    val id: String? = null,
    val namaMatkul: String? = null,
    val hari: String? = null,
    val jamMulai: String? = null,
    val jamSelesai: String? = null,
    val ruangan: String? = null,
    val dosen: String? = null,
    val userId: String? = null
) {
    constructor() : this(null, null, null, null, null, null, null, null)
}