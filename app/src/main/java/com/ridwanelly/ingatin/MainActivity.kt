package com.ridwanelly.ingatin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ridwanelly.ingatin.adapters.JadwalAdapter
import com.ridwanelly.ingatin.models.MataKuliah

class MainActivity : AppCompatActivity(), JadwalAdapter.OnItemClickListener {

    private lateinit var rvJadwal: RecyclerView
    private lateinit var fabAddJadwal: FloatingActionButton
    private lateinit var tvEmptyView: TextView
    private lateinit var jadwalAdapter: JadwalAdapter
    private val itemList = mutableListOf<Any>()

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        fetchJadwalFromFirestore()
        setupSwipeToDelete()
    }

    private fun initViews() {
        rvJadwal = findViewById(R.id.rvJadwal)
        fabAddJadwal = findViewById(R.id.fabAddJadwal)
        tvEmptyView = findViewById(R.id.tvEmptyView)

        fabAddJadwal.setOnClickListener {
            startActivity(Intent(this, AddJadwalActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        jadwalAdapter = JadwalAdapter(itemList, this)
        rvJadwal.layoutManager = LinearLayoutManager(this)
        rvJadwal.adapter = jadwalAdapter
    }

    private fun fetchJadwalFromFirestore() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("jadwal")
            .orderBy("jamMulai")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Tangani error
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val jadwalFromDb = snapshot.toObjects(MataKuliah::class.java)
                    val urutanHari = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
                    val jadwalPerHari = jadwalFromDb.groupBy { it.hari }

                    itemList.clear()
                    urutanHari.forEach { hari ->
                        val jadwalHariIni = jadwalPerHari[hari]
                        if (!jadwalHariIni.isNullOrEmpty()) {
                            itemList.add(hari)
                            itemList.addAll(jadwalHariIni)
                        }
                    }
                    jadwalAdapter.updateData(itemList, rvJadwal)
                    checkIfEmpty()
                }
            }
    }

    private fun checkIfEmpty() {
        if (itemList.isEmpty()) {
            rvJadwal.visibility = View.GONE
            tvEmptyView.visibility = View.VISIBLE
        } else {
            rvJadwal.visibility = View.VISIBLE
            tvEmptyView.visibility = View.GONE
        }
    }

    override fun onItemClick(mataKuliah: MataKuliah) {
        val intent = Intent(this, DetailMatkulActivity::class.java).apply {
            putExtra("MATKUL_ID", mataKuliah.id)
            putExtra("MATKUL_NAMA", mataKuliah.namaMatkul)
            putExtra("MATKUL_DOSEN", mataKuliah.dosen)
            putExtra("MATKUL_HARI", mataKuliah.hari)
            putExtra("MATKUL_JAM_MULAI", mataKuliah.jamMulai)
            putExtra("MATKUL_JAM_SELESAI", mataKuliah.jamSelesai)
            putExtra("MATKUL_RUANGAN", mataKuliah.ruangan)
        }
        startActivity(intent)
    }

    // --- FUNGSI BARU UNTUK SWIPE-TO-DELETE ---
    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (itemList[position] is MataKuliah) {
                    val mataKuliah = itemList[position] as MataKuliah
                    showDeleteConfirmationDialog(mataKuliah, position)
                } else {
                    jadwalAdapter.notifyItemChanged(position)
                }
            }

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                if (itemList[viewHolder.adapterPosition] is String) {
                    return 0
                }
                return super.getSwipeDirs(recyclerView, viewHolder)
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(rvJadwal)
    }

    private fun showDeleteConfirmationDialog(mataKuliah: MataKuliah, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Jadwal")
            .setMessage("Anda yakin ingin menghapus jadwal ${mataKuliah.namaMatkul}? Pengingat mingguan untuk jadwal ini juga akan dihapus.")
            .setPositiveButton("Hapus") { _, _ ->
                deleteJadwal(mataKuliah)
            }
            .setNegativeButton("Batal") { _, _ ->
                jadwalAdapter.notifyItemChanged(position)
            }
            .setCancelable(false)
            .show()
    }

    private fun deleteJadwal(mataKuliah: MataKuliah) {
        val userId = auth.currentUser?.uid ?: return
        val jadwalId = mataKuliah.id ?: return

        db.collection("users").document(userId).collection("jadwal").document(jadwalId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Jadwal berhasil dihapus", Toast.LENGTH_SHORT).show()
                WorkManager.getInstance(this).cancelUniqueWork(jadwalId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
