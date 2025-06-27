package com.ridwanelly.ingatin.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Catatan(
    @DocumentId
    val id: String? = null,
    val content: String? = null,
    val timestamp: Timestamp? = null,
    val matkulId: String? = null,
    val userId: String? = null
) {
    // Firestore memerlukan konstruktor tanpa argumen
    constructor() : this(null, null, null, null, null)
}