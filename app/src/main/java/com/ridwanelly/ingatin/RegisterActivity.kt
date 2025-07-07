package com.ridwanelly.ingatin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmailRegister)
        val etPassword = findViewById<EditText>(R.id.etPasswordRegister)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Registrasi berhasil
                        Toast.makeText(this, "Registrasi Berhasil. Silakan Login.", Toast.LENGTH_LONG).show()
                        // Kembali ke halaman login
                        finish()
                    } else {
                        // Registrasi gagal
                        Toast.makeText(this, "Registrasi Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvGoToLogin.setOnClickListener {
            finish()
        }
    }
}