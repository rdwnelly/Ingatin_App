package com.ridwanelly.ingatin.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tugas(
    @DocumentId
    val id: String? = null,
    val namaTugas: String? = null,
    val deskripsi: String? = null,
    val deadline: Timestamp? = null,
    var isCompleted: Boolean = false,
    val matkulId: String? = null,
    val userId: String? = null,
    val completedAt: Timestamp? = null
) : Parcelable {
    constructor() : this(null, null, null, null, false, null, null, null)
}