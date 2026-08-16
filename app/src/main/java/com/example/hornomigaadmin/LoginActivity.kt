package com.example.hornomigaadmin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Si ya había una sesión abierta, saltamos directo a la lista de reservas.
        if (auth.currentUser != null) {
            goToReservas()
            return
        }

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val tvError = findViewById<android.widget.TextView>(R.id.tvError)
        val btnLogin = findViewById<android.widget.Button>(R.id.btnLogin)
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)

        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val password = etPassword.text?.toString().orEmpty()
            tvError.text = ""

            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = "Ingresa tu correo y contraseña."
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            progressBar.visibility = View.VISIBLE

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    btnLogin.isEnabled = true
                    progressBar.visibility = View.GONE

                    if (task.isSuccessful) {
                        goToReservas()
                    } else {
                        tvError.text = "No se pudo iniciar sesión. Revisa tu correo y contraseña."
                    }
                }
        }
    }

    private fun goToReservas() {
        startActivity(Intent(this, ReservasActivity::class.java))
        finish()
    }
}