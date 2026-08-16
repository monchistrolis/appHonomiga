package com.example.hornomigaadmin

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class ReservaCompletadaAdapter(
    private var reservas: List<Reserva> = emptyList()
) : RecyclerView.Adapter<ReservaCompletadaAdapter.ReservaCompletadaViewHolder>() {

    private val formatoMoneda = NumberFormat.getInstance(Locale("es", "CL"))
    private val db = FirebaseFirestore.getInstance()

    class ReservaCompletadaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFechaHora: TextView = view.findViewById(R.id.tvFechaHora)
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvEntrega: TextView = view.findViewById(R.id.tvEntrega)
        val tvItems: TextView = view.findViewById(R.id.tvItems)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        val btnReabrir: Button = view.findViewById(R.id.btnReabrir)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaCompletadaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reserva_completado, parent, false)
        return ReservaCompletadaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservaCompletadaViewHolder, position: Int) {
        val r = reservas[position]

        holder.tvFechaHora.text = "${if (r.entrega == "despacho") "Despacho" else "Retiro"}: ${r.fecha} · ${r.hora} · N.º ${r.id}"
        holder.tvNombre.text = r.nombre
        holder.tvEntrega.text = if (r.entrega == "despacho") {
            "Dirección: ${r.direccionDespacho}"
        } else {
            "Retiro en tienda"
        }
        holder.tvItems.text = r.items.joinToString(", ") {
            "${it.qty}× ${it.name}${if (it.extras.isNotEmpty()) " (+ ${it.extras.joinToString(", ")})" else ""}"
        }
        holder.tvTotal.text = "Total: $${formatoMoneda.format(r.total)}"

        holder.btnReabrir.setOnClickListener {
            confirmarYReabrir(holder.itemView.context, r)
        }
    }

    override fun getItemCount(): Int = reservas.size

    fun actualizarDatos(nuevas: List<Reserva>) {
        reservas = nuevas
        notifyDataSetChanged()
    }

    private fun confirmarYReabrir(context: Context, r: Reserva) {
        AlertDialog.Builder(context)
            .setTitle("¿Reabrir pedido?")
            .setMessage("El pedido N.º ${r.id} de ${r.nombre} volverá a la lista de pendientes.")
            .setPositiveButton("Sí, reabrir") { _, _ -> reabrir(context, r) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun reabrir(context: Context, r: Reserva) {
        db.collection("reservas").document(r.id)
            .update("estado", "pendiente")
            .addOnSuccessListener {
                Toast.makeText(context, "Pedido reabierto", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "No se pudo reabrir: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}