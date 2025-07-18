package com.ridwanelly.ingatin.adapters

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ridwanelly.ingatin.R
import com.ridwanelly.ingatin.models.Tugas
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TugasAdapter(private var tugasList: List<Tugas>) : RecyclerView.Adapter<TugasAdapter.TugasViewHolder>() {

    class TugasViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val namaTugas: TextView = itemView.findViewById(R.id.tvNamaTugas)
        val deadlineTugas: TextView = itemView.findViewById(R.id.tvDeadlineTugas)
        val deadlineTugasFull: TextView = itemView.findViewById(R.id.tvDeadlineTugasFull)
        val isCompleted: CheckBox = itemView.findViewById(R.id.cbTugasSelesai)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TugasViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tugas, parent, false)
        return TugasViewHolder(view)
    }

    override fun onBindViewHolder(holder: TugasViewHolder, position: Int) {
        val tugas = tugasList[position]
        val context = holder.itemView.context

        holder.namaTugas.text = tugas.namaTugas
        holder.isCompleted.isChecked = tugas.isCompleted

        tugas.deadline?.let { timestamp ->
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            holder.deadlineTugasFull.text = context.getString(R.string.deadline_format, sdf.format(timestamp.toDate()))

            val now = System.currentTimeMillis()
            val deadlineMillis = timestamp.toDate().time
            val diff = deadlineMillis - now

            if (diff > 0) {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                val hours = TimeUnit.MILLISECONDS.toHours(diff)

                val countdownText = when {
                    days > 0 -> context.getString(R.string.countdown_format_days, days)
                    hours > 0 -> context.getString(R.string.countdown_format_hours, hours)
                    else -> context.getString(R.string.countdown_format_hours, 1) // Kurang dari 1 jam
                }
                holder.deadlineTugas.text = countdownText
                holder.deadlineTugas.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
            } else {
                holder.deadlineTugas.text = context.getString(R.string.countdown_format_overdue)
                holder.deadlineTugas.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
            }
        }

        updateStrikethrough(holder.namaTugas, tugas.isCompleted)

        holder.isCompleted.setOnCheckedChangeListener(null)
        holder.isCompleted.setOnCheckedChangeListener { _, isChecked ->
            if (tugas.isCompleted != isChecked) {
                tugas.isCompleted = isChecked
                updateStrikethrough(holder.namaTugas, isChecked)
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
            val deadline = tugas.deadline
            if (deadline != null && Timestamp.now().compareTo(deadline) <= 0) {
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

    private fun awardPointsForOnTimeCompletion(db: FirebaseFirestore, userId: String, context: android.content.Context) {
        val gamificationRef = db.collection("users").document(userId).collection("gamification").document("summary")
        val pointsToAdd = 10L

        db.runTransaction { transaction ->
            val snapshot = transaction.get(gamificationRef)
            var newPoints = pointsToAdd
            var newOnTimeSubmissions = 1L
            var currentBadges = mutableListOf<String>()

            if (snapshot.exists()) {
                val currentPoints = snapshot.getLong("points") ?: 0L
                val currentSubmissions = snapshot.getLong("onTimeSubmissions") ?: 0L
                currentBadges = (snapshot.get("badges") as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
                newPoints = currentPoints + pointsToAdd
                newOnTimeSubmissions = currentSubmissions + 1
            }

            val newBadges = checkAndAwardBadges(newOnTimeSubmissions, currentBadges)
            val newLevel = (newPoints / 50) + 1

            val updateData = hashMapOf(
                "points" to newPoints,
                "level" to newLevel,
                "onTimeSubmissions" to newOnTimeSubmissions,
                "badges" to newBadges
            )

            transaction.set(gamificationRef, updateData)
            newBadges.filter { !currentBadges.contains(it) }
        }.addOnSuccessListener { newBadgesEarned ->
            Toast.makeText(context, "+$pointsToAdd Poin! Kerja bagus!", Toast.LENGTH_SHORT).show()
            newBadgesEarned.forEach { badgeId ->
                val badgeName = getBadgeName(badgeId)
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

    private fun updateStrikethrough(textView: TextView, isCompleted: Boolean) {
        if (isCompleted) {
            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    override fun getItemCount(): Int = tugasList.size

    fun updateData(newTugasList: List<Tugas>, rvTugasMendatang: RecyclerView) {
        this.tugasList = newTugasList
        notifyDataSetChanged()
    }
}