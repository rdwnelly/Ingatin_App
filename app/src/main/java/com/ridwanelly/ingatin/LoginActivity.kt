package com.ridwanelly.ingatin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Memastikan aplikasi tetap di LoginActivity jika pengguna belum login
        if (auth.currentUser == null) {
            // Tetap di LoginActivity
        } else {
            // Jika sudah login, arahkan ke MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Login berhasil
                        Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        // Login gagal
                        Toast.makeText(this, "Login Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}