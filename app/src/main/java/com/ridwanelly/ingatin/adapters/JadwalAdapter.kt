package com.ridwanelly.ingatin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ridwanelly.ingatin.R
import com.ridwanelly.ingatin.models.MataKuliah

// Tambahkan parameter listener
class JadwalAdapter(
    private var jadwalList: List<MataKuliah>,
    private val listener: OnItemClickListener // <-- TAMBAHKAN INI
) : RecyclerView.Adapter<JadwalAdapter.JadwalViewHolder>() {

    // 'memegang' view dari setiap item
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
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(jadwalList[position])
            }
        }
    }

    // Interface untuk click listener <-- TAMBAHKAN INI
    interface OnItemClickListener {
        fun onItemClick(mataKuliah: MataKuliah)
    }

    // Membuat ViewHolder baru
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JadwalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_jadwal, parent, false)
        return JadwalViewHolder(view)
    }

    // ... (sisa kode onBindViewHolder, getItemCount, updateData tetap sama)
    override fun onBindViewHolder(holder: JadwalViewHolder, position: Int) {
        val jadwal = jadwalList[position]
        holder.namaMatkul.text = jadwal.namaMatkul
        holder.dosen.text = jadwal.dosen
        holder.ruangan.text = "Ruang: ${jadwal.ruangan}"
        holder.hari.text = jadwal.hari
        holder.jam.text = "${jadwal.jamMulai} - ${jadwal.jamSelesai}"
    }

    override fun getItemCount(): Int = jadwalList.size

    fun updateData(newJadwalList: List<MataKuliah>) {
        this.jadwalList = newJadwalList
        notifyDataSetChanged()
    }
}