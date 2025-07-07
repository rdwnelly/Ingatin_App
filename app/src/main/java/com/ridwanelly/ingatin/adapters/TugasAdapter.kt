package com.ridwanelly.ingatin.adapters

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ridwanelly.ingatin.R
import com.ridwanelly.ingatin.models.Tugas
import java.text.SimpleDateFormat
import java.util.*

class TugasAdapter(private var tugasList: List<Tugas>) : RecyclerView.Adapter<TugasAdapter.TugasViewHolder>() {

    class TugasViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val namaTugas: TextView = itemView.findViewById(R.id.tvNamaTugas)
        val deadlineTugas: TextView = itemView.findViewById(R.id.tvDeadlineTugas)
        val isCompleted: CheckBox = itemView.findViewById(R.id.cbTugasSelesai)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TugasViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tugas, parent, false)
        return TugasViewHolder(view)
    }

    override fun onBindViewHolder(holder: TugasViewHolder, position: Int) {
        val tugas = tugasList[position]

        holder.namaTugas.text = tugas.namaTugas
        holder.isCompleted.isChecked = tugas.isCompleted

        // Format timestamp ke tanggal yang mudah dibaca
        tugas.deadline?.let { timestamp ->
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID"))
            holder.deadlineTugas.text = "Deadline: ${sdf.format(timestamp.toDate())}"
        }

        updateStrikethrough(holder.namaTugas, tugas.isCompleted)

        // Hapus listener lama untuk mencegah pemanggilan ganda
        holder.isCompleted.setOnCheckedChangeListener(null)

        // Set listener baru
        holder.isCompleted.setOnCheckedChangeListener { _, isChecked ->
            // Hanya jalankan logika jika status berubah
            if (tugas.isCompleted != isChecked) {
                tugas.isCompleted = isChecked
                updateStrikethrough(holder.namaTugas, isChecked)

                // Update status di Firestore dan berikan poin jika perlu
                updateTugasCompletionStatus(tugas, isChecked, holder)
            }
        }
    }

    private fun updateTugasCompletionStatus(tugas: Tugas, isChecked: Boolean, holder: TugasViewHolder) {
        val db = FirebaseFirestore.getInstance()
        val userId = tugas.userId ?: return
        val tugasId = tugas.id ?: return

        val tugasRef = db.collection("users").document(userId).collection("tugas").document(tugasId)

        val updateData = hashMapOf<String, Any>(
            "completed" to isChecked
        )

        if (isChecked) {
            updateData["completedAt"] = FieldValue.serverTimestamp()

            // Cek apakah tugas diselesaikan tepat waktu
            val deadline = tugas.deadline
            if (deadline != null && Timestamp.now().compareTo(deadline) <= 0) {
                // TEPAT WAKTU: Berikan poin dan cek badge
                awardPointsForOnTimeCompletion(db, userId, holder.itemView.context)
            }
        } else {
            updateData["completedAt"] = FieldValue.delete()
        }

        tugasRef.update(updateData)
            .addOnFailureListener {
                holder.isCompleted.isChecked = !isChecked
                updateStrikethrough(holder.namaTugas, !isChecked)
            }
    }

    // --- FUNGSI BARU UNTUK GAMIFIKASI ---
    private fun awardPointsForOnTimeCompletion(db: FirebaseFirestore, userId: String, context: android.content.Context) {
        val gamificationRef = db.collection("users").document(userId).collection("gamification").document("summary")
        val pointsToAdd = 10L // Poin per tugas

        db.runTransaction { transaction ->
            val snapshot = transaction.get(gamificationRef)
            var newPoints = pointsToAdd
            var newOnTimeSubmissions = 1L
            var currentBadges = mutableListOf<String>()

            if (snapshot.exists()) {
                val currentPoints = snapshot.getLong("points") ?: 0L
                val currentSubmissions = snapshot.getLong("onTimeSubmissions") ?: 0L
                currentBadges = (snapshot.get("badges") as? List<*> ?: emptyList<Any>()).filterIsInstance<String>().toMutableList()
                newPoints = currentPoints + pointsToAdd
                newOnTimeSubmissions = currentSubmissions + 1
            }

            // Logika untuk menambahkan badge baru
            val newBadges = checkAndAwardBadges(newOnTimeSubmissions, currentBadges)

            val newLevel = (newPoints / 50) + 1 // Setiap 50 poin naik level

            val updateData = hashMapOf(
                "points" to newPoints,
                "level" to newLevel,
                "onTimeSubmissions" to newOnTimeSubmissions,
                "badges" to newBadges
            )

            transaction.set(gamificationRef, updateData)

            // Mengembalikan daftar badge baru untuk ditampilkan di Toast
            newBadges.filter { !currentBadges.contains(it) }
        }.addOnSuccessListener { newBadgesEarned ->
            Toast.makeText(context, "+$pointsToAdd Poin! Kerja bagus!", Toast.LENGTH_SHORT).show()
            newBadgesEarned.forEach { badgeId ->
                val badgeName = getBadgeName(badgeId) // Fungsi untuk mendapatkan nama badge
                Toast.makeText(context, "🏆 Badge Baru Diraih: $badgeName!", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { e ->
            Log.e("Gamification", "Gagal memberikan poin: ${e.message}")
        }
    }

    private fun checkAndAwardBadges(onTimeCount: Long, currentBadges: List<String>): List<String> {
        val newBadges = currentBadges.toMutableList()

        if (onTimeCount >= 1 && !newBadges.contains("FIRST_STEP")) newBadges.add("FIRST_STEP")
        if (onTimeCount >= 5 && !newBadges.contains("FIVE_STAR")) newBadges.add("FIVE_STAR")
        if (onTimeCount >= 10 && !newBadges.contains("TEN_MASTER")) newBadges.add("TEN_MASTER")
        if (onTimeCount >= 25 && !newBadges.contains("DILIGENT_25")) newBadges.add("DILIGENT_25")

        return newBadges
    }

    private fun getBadgeName(badgeId: String): String {
        return when(badgeId) {
            "FIRST_STEP" -> "Langkah Pertama"
            "FIVE_STAR" -> "Bintang Lima"
            "TEN_MASTER" -> "Master Sepuluh"
            "DILIGENT_25" -> "Rajin Belajar"
            else -> "Badge Spesial"
        }
    }
    // --- AKHIR FUNGSI BARU ---


    private fun updateStrikethrough(textView: TextView, isCompleted: Boolean) {
        if (isCompleted) {
            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    override fun getItemCount(): Int = tugasList.size

    fun updateData(newTugasList: List<Tugas>, recyclerView: RecyclerView? = null) {
        this.tugasList = newTugasList
        notifyDataSetChanged()
    }
}