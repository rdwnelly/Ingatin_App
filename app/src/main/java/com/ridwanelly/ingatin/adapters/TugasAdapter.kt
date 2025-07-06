package com.ridwanelly.ingatin.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
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

        // Listener untuk checkbox
        holder.isCompleted.setOnCheckedChangeListener { _, isChecked ->
            tugas.isCompleted = isChecked
            updateStrikethrough(holder.namaTugas, isChecked)

            // Update status di Firestore
            updateTugasCompletionStatus(tugas, isChecked, holder)
        }
    }

    // --- FUNGSI DIPERBARUI ---
    private fun updateTugasCompletionStatus(tugas: Tugas, isChecked: Boolean, holder: TugasViewHolder) {
        tugas.id?.let { id ->
            tugas.userId?.let { userId ->
                val db = FirebaseFirestore.getInstance()
                val tugasRef = db.collection("users").document(userId).collection("tugas").document(id)

                val updateData = hashMapOf<String, Any>(
                    "completed" to isChecked
                )

                // Jika dicentang, tambahkan waktu selesai. Jika tidak, hapus.
                if (isChecked) {
                    updateData["completedAt"] = FieldValue.serverTimestamp()
                } else {
                    updateData["completedAt"] = FieldValue.delete()
                }

                tugasRef.update(updateData)
                    .addOnFailureListener {
                        // Jika gagal, kembalikan checkbox ke state semula
                        holder.isCompleted.isChecked = !isChecked
                    }
            }
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

    fun updateData(newTugasList: List<Tugas>, recyclerView: RecyclerView? = null) {
        this.tugasList = newTugasList
        notifyDataSetChanged()
    }
}