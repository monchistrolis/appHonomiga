package com.example.hornomigaadmin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// MainActivity es solo la puerta de entrada: manda directo a LoginActivity,
// que ya decide si hay sesión abierta (va a ReservasActivity) o no (pide login).
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}