package com.example.hornomigaadmin

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CompletadosActivity : AppCompatActivity() {

    private lateinit var rvCompletados: RecyclerView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var tvVacioOError: android.widget.TextView
    private lateinit var btnVolver: android.widget.Button
    private val adapter = ReservaCompletadaAdapter()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_completados)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rvCompletados = findViewById(R.id.rvCompletados)
        progressBar = findViewById(R.id.progressBar)
        tvVacioOError = findViewById(R.id.tvVacioOError)
        btnVolver = findViewById(R.id.btnVolver)

        rvCompletados.layoutManager = LinearLayoutManager(this)
        rvCompletados.adapter = adapter

        btnVolver.setOnClickListener { finish() }

        cargarCompletados()
    }

    private fun cargarCompletados() {
        mostrarCargando()

        // "whereIn" para no perder pedidos viejos que todavía tienen
        // estado="completado" (antes del rediseño con 4 estados). Si
        // Firestore pide crear un índice compuesto para este where + orderBy,
        // el error trae un link directo para crearlo con un clic.
        db.collection("reservas")
            .whereIn("estado", listOf("completado", Estado.ENTREGADO))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    mostrarError()
                    return@addSnapshotListener
                }

                val completados = snapshot?.documents?.mapNotNull { it.toObject(Reserva::class.java) }
                    ?: emptyList()

                if (completados.isEmpty()) {
                    mostrarVacio()
                } else {
                    mostrarLista(completados)
                }
            }
    }

    private fun mostrarCargando() {
        progressBar.visibility = View.VISIBLE
        tvVacioOError.visibility = View.GONE
        rvCompletados.visibility = View.GONE
    }

    private fun mostrarLista(reservas: List<Reserva>) {
        adapter.actualizarDatos(reservas)
        progressBar.visibility = View.GONE
        tvVacioOError.visibility = View.GONE
        rvCompletados.visibility = View.VISIBLE
    }

    private fun mostrarVacio() {
        progressBar.visibility = View.GONE
        rvCompletados.visibility = View.GONE
        tvVacioOError.text = "Todavía no hay pedidos completados."
        tvVacioOError.visibility = View.VISIBLE
    }

    private fun mostrarError() {
        progressBar.visibility = View.GONE
        rvCompletados.visibility = View.GONE
        tvVacioOError.text = "No pudimos cargar los completados. Revisa tu conexión, los permisos de Firestore, o si falta crear un índice (revisa Logcat por un link de Firebase)."
        tvVacioOError.visibility = View.VISIBLE
    }
}