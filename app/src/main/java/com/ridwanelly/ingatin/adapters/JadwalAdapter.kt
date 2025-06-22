package com.ridwanelly.ingatin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ridwanelly.ingatin.R
import com.ridwanelly.ingatin.models.MataKuliah

class JadwalAdapter(
    private var itemList: List<Any>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_JADWAL = 1
    }

    // ViewHolder untuk Item Jadwal
    inner class JadwalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val namaMatkul: TextView = itemView.findViewById(R.id.tvNamaMatkul)
        val dosen: TextView = itemView.findViewById(R.id.tvDosen)
        val ruangan: TextView = itemView.findViewById(R.id.tvRuangan)
        val hari: TextView = itemView.findViewById(R.id.tvHari)
        val jam: TextView = itemView.findViewById(R.id.tvJam)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION && itemList[position] is MataKuliah) {
                listener.onItemClick(itemList[position] as MataKuliah)
            }
        }
    }

    // ViewHolder untuk Header Hari
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerText: TextView = itemView.findViewById(R.id.tvHeaderHari)
    }

    // Interface untuk click listener
    interface OnItemClickListener {
        fun onItemClick(mataKuliah: MataKuliah)
    }

    override fun getItemViewType(position: Int): Int {
        return when (itemList[position]) {
            is String -> VIEW_TYPE_HEADER
            is MataKuliah -> VIEW_TYPE_JADWAL
            else -> throw IllegalArgumentException("Tipe data tidak didukung")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hari_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_JADWAL -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_jadwal, parent, false)
                JadwalViewHolder(view)
            }
            else -> throw IllegalArgumentException("Tipe view tidak valid")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.headerText.text = itemList[position] as String
            }
            is JadwalViewHolder -> {
                val jadwal = itemList[position] as MataKuliah
                holder.namaMatkul.text = jadwal.namaMatkul
                holder.dosen.text = jadwal.dosen
                holder.ruangan.text = "Ruang: ${jadwal.ruangan}"
                holder.hari.text = jadwal.hari
                holder.jam.text = "${jadwal.jamMulai} - ${jadwal.jamSelesai}"
            }
        }
    }

    override fun getItemCount(): Int = itemList.size

    fun updateData(newItemList: List<Any>) {
        this.itemList = newItemList
        notifyDataSetChanged()
    }
}