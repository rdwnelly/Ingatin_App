package com.ridwanelly.ingatin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ridwanelly.ingatin.R
import com.ridwanelly.ingatin.models.Catatan
import java.text.SimpleDateFormat
import java.util.*

class CatatanAdapter(
    private var catatanList: List<Catatan>,
    private val onDeleteClick: (Catatan) -> Unit,
    private val onItemClick: (Catatan) -> Unit
) : RecyclerView.Adapter<CatatanAdapter.CatatanViewHolder>() {

    class CatatanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val content: TextView = itemView.findViewById(R.id.tvCatatanContent)
        val timestamp: TextView = itemView.findViewById(R.id.tvCatatanTimestamp)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btnHapusCatatan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatatanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_catatan, parent, false)
        return CatatanViewHolder(view)
    }

    override fun onBindViewHolder(holder: CatatanViewHolder, position: Int) {
        val catatan = catatanList[position]
        holder.content.text = catatan.content

        catatan.timestamp?.let {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            holder.timestamp.text = sdf.format(it.toDate())
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(catatan)
        }

        holder.itemView.setOnClickListener {
            onItemClick(catatan)
        }
    }

    override fun getItemCount(): Int = catatanList.size

    fun updateData(newCatatanList: List<Catatan>) {
        this.catatanList = newCatatanList
        notifyDataSetChanged()
    }
}