package com.ridwanelly.ingatin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ridwanelly.ingatin.adapters.CatatanAdapter
import com.ridwanelly.ingatin.adapters.TugasAdapter
import com.ridwanelly.ingatin.models.Catatan
import com.ridwanelly.ingatin.models.Tugas

class DetailMatkulActivity : AppCompatActivity() {

    // --- Variabel Data Mata Kuliah ---
    private var matkulId: String? = null
    private var matkulNama: String? = null
    private var matkulDosen: String? = null
    private var matkulInfo: String? = null

    // --- Variabel Komponen UI ---
    private lateinit var tvDetailNamaMatkul: TextView
    private lateinit var tvDetailDosen: TextView
    private lateinit var tvDetailInfo: TextView
    private lateinit var rvTugas: RecyclerView
    private lateinit var tvEmptyTugas: TextView
    private lateinit var fabAddTask: FloatingActionButton
    // UI Baru untuk Catatan
    private lateinit var rvCatatan: RecyclerView
    private lateinit var tvEmptyCatatan: TextView
    private lateinit var etCatatan: EditText
    private lateinit var btnSimpanCatatan: Button


    // --- Variabel Adapter dan Firebase ---
    private lateinit var tugasAdapter: TugasAdapter
    private lateinit var catatanAdapter: CatatanAdapter
    private val tugasList = mutableListOf<Tugas>()
    private val catatanList = mutableListOf<Catatan>()
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_matkul)

        getIntentData()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupHeaderViews()
        setupRecyclerViews()

        fetchTugasFromFirestore()
        fetchCatatanFromFirestore()

        fabAddTask.setOnClickListener {
            val intent = Intent(this, AddTugasActivity::class.java)
            intent.putExtra("MATKUL_ID", matkulId)
            startActivity(intent)
        }

        btnSimpanCatatan.setOnClickListener {
            saveCatatan()
        }
    }

    private fun getIntentData() {
        matkulId = intent.getStringExtra("MATKUL_ID")
        matkulNama = intent.getStringExtra("MATKUL_NAMA")
        matkulDosen = intent.getStringExtra("MATKUL_DOSEN")
        val hari = intent.getStringExtra("MATKUL_HARI")
        val jamMulai = intent.getStringExtra("MATKUL_JAM_MULAI")
        val jamSelesai = intent.getStringExtra("MATKUL_JAM_SELESAI")
        val ruangan = intent.getStringExtra("MATKUL_RUANGAN")
        matkulInfo = "$hari, $jamMulai-$jamSelesai di $ruangan"

        if (matkulId == null) {
            Toast.makeText(this, "Error: ID Mata Kuliah tidak ditemukan.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initViews() {
        tvDetailNamaMatkul = findViewById(R.id.tvDetailNamaMatkul)
        tvDetailDosen = findViewById(R.id.tvDetailDosen)
        tvDetailInfo = findViewById(R.id.tvDetailInfo)
        rvTugas = findViewById(R.id.rvTugas)
        tvEmptyTugas = findViewById(R.id.tvEmptyTugas)
        fabAddTask = findViewById(R.id.fabAddTask)
        rvCatatan = findViewById(R.id.rvCatatan)
        tvEmptyCatatan = findViewById(R.id.tvEmptyCatatan)
        etCatatan = findViewById(R.id.etCatatan)
        btnSimpanCatatan = findViewById(R.id.btnSimpanCatatan)
    }

    private fun setupHeaderViews() {
        tvDetailNamaMatkul.text = matkulNama
        tvDetailDosen.text = matkulDosen
        tvDetailInfo.text = matkulInfo
    }

    private fun setupRecyclerViews() {
        // Inisialisasi TugasAdapter dengan click listener
        tugasAdapter = TugasAdapter(tugasList) { tugas ->
            showTugasReviewDialog(tugas)
        }
        rvTugas.layoutManager = LinearLayoutManager(this)
        rvTugas.adapter = tugasAdapter

        // Inisialisasi CatatanAdapter dengan click listener
        catatanAdapter = CatatanAdapter(catatanList,
            onDeleteClick = { catatan -> showDeleteConfirmationDialog(catatan) },
            onItemClick = { catatan -> showCatatanEditDialog(catatan) }
        )
        rvCatatan.layoutManager = LinearLayoutManager(this)
        rvCatatan.adapter = catatanAdapter
    }

    // --- FUNGSI-FUNGSI BARU UNTUK TUGAS & CATATAN ---

    private fun showTugasReviewDialog(tugas: Tugas) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tinjau_tugas, null)
        val tvNamaTugas = dialogView.findViewById<TextView>(R.id.tvDialogNamaTugas)
        val tvDeskripsiTugas = dialogView.findViewById<TextView>(R.id.tvDialogDeskripsiTugas)

        tvNamaTugas.text = tugas.namaTugas
        tvDeskripsiTugas.text = tugas.deskripsi?.ifEmpty { "Tidak ada deskripsi." }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Edit") { _, _ ->
                // Belum diimplementasikan, perlu membuat EditTugasActivity
                Toast.makeText(this, "Fitur edit belum tersedia.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun showCatatanEditDialog(catatan: Catatan) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_catatan, null)
        val etEditCatatan = dialogView.findViewById<EditText>(R.id.etEditCatatan)
        etEditCatatan.setText(catatan.content)

        AlertDialog.Builder(this)
            .setTitle("Edit Catatan")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val newContent = etEditCatatan.text.toString().trim()
                if (newContent.isNotEmpty() && newContent != catatan.content) {
                    updateCatatan(catatan, newContent)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateCatatan(catatan: Catatan, newContent: String) {
        val userId = auth.currentUser?.uid ?: return
        if (catatan.id == null) return

        db.collection("users").document(userId).collection("catatan").document(catatan.id)
            .update("content", newContent)
            .addOnSuccessListener {
                Toast.makeText(this, "Catatan diperbarui.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memperbarui: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // --- FUNGSI-FUNGSI LAMA ---

    private fun fetchCatatanFromFirestore() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("catatan")
            .whereEqualTo("matkulId", matkulId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    catatanList.clear()
                    val result = snapshot.toObjects(Catatan::class.java)
                    catatanList.addAll(result)
                    catatanAdapter.updateData(catatanList)
                    checkIfCatatanEmpty()
                }
            }
    }

    private fun saveCatatan() {
        val content = etCatatan.text.toString().trim()
        val userId = auth.currentUser?.uid

        if (content.isEmpty()) {
            Toast.makeText(this, "Catatan tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            return
        }
        if (userId == null || matkulId == null) {
            Toast.makeText(this, "Error: Tidak bisa menyimpan catatan.", Toast.LENGTH_SHORT).show()
            return
        }

        val catatanBaru = Catatan(
            content = content,
            timestamp = Timestamp.now(),
            matkulId = matkulId,
            userId = userId
        )

        db.collection("users").document(userId).collection("catatan")
            .add(catatanBaru)
            .addOnSuccessListener {
                etCatatan.text.clear()
                Toast.makeText(this, "Catatan disimpan!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDeleteConfirmationDialog(catatan: Catatan) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Catatan")
            .setMessage("Apakah Anda yakin ingin menghapus catatan ini?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteCatatan(catatan)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteCatatan(catatan: Catatan) {
        val userId = auth.currentUser?.uid ?: return
        if (catatan.id == null) return

        db.collection("users").document(userId).collection("catatan").document(catatan.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Catatan dihapus.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkIfCatatanEmpty() {
        if (catatanList.isEmpty()) {
            rvCatatan.visibility = View.GONE
            tvEmptyCatatan.visibility = View.VISIBLE
        } else {
            rvCatatan.visibility = View.VISIBLE
            tvEmptyCatatan.visibility = View.GONE
        }
    }

    private fun fetchTugasFromFirestore() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("tugas")
            .whereEqualTo("matkulId", matkulId)
            .orderBy("deadline", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    tugasList.clear()
                    val result = snapshot.toObjects(Tugas::class.java)
                    tugasList.addAll(result)
                    tugasAdapter.updateData(tugasList, rvTugas)
                    checkIfTugasEmpty()
                }
            }
    }

    private fun checkIfTugasEmpty() {
        if (tugasList.isEmpty()) {
            rvTugas.visibility = View.GONE
            tvEmptyTugas.visibility = View.VISIBLE
        } else {
            rvTugas.visibility = View.VISIBLE
            tvEmptyTugas.visibility = View.GONE
        }
    }
}