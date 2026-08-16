package com.example.hornomigaadmin

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
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

class ReservaAdapter(
    private var reservas: List<Reserva> = emptyList()
) : RecyclerView.Adapter<ReservaAdapter.ReservaViewHolder>() {

    private val formatoMoneda = NumberFormat.getInstance(Locale("es", "CL"))
    private val db = FirebaseFirestore.getInstance()

    class ReservaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFechaHora: TextView = view.findViewById(R.id.tvFechaHora)
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvEntrega: TextView = view.findViewById(R.id.tvEntrega)
        val tvItems: TextView = view.findViewById(R.id.tvItems)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        val tvContacto: TextView = view.findViewById(R.id.tvContacto)
        val btnPanListoWhatsapp: Button = view.findViewById(R.id.btnPanListoWhatsapp)
        val btnPanListoCorreo: Button = view.findViewById(R.id.btnPanListoCorreo)
        val btnEnCamino: Button = view.findViewById(R.id.btnEnCamino)
        val btnMarcarCompletado: Button = view.findViewById(R.id.btnMarcarCompletado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reserva, parent, false)
        return ReservaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        val r = reservas[position]

        holder.tvFechaHora.text = "${if (r.entrega == "despacho") "Despacho" else "Retiro"}: ${r.fecha} · ${r.hora} · N.º ${r.id}"
        holder.tvNombre.text = r.nombre

        holder.tvEntrega.text = if (r.entrega == "despacho") {
            val km = r.distanciaDespachoKm?.let { "%.1f".format(it) } ?: "?"
            "Dirección: ${r.direccionDespacho} ($km km · $${formatoMoneda.format(r.costoDespacho)} despacho)"
        } else {
            "Retiro en tienda"
        }

        holder.tvItems.text = r.items.joinToString(", ") {
            "${it.qty}× ${it.name}${if (it.extras.isNotEmpty()) " (+ ${it.extras.joinToString(", ")})" else ""}"
        }
        holder.tvTotal.text = "Total: $${formatoMoneda.format(r.total)}"
        holder.tvContacto.text = "${r.telefono}${if (r.email.isNotBlank()) " · ${r.email}" else ""}" +
                if (r.notas.isNotBlank()) "\nNotas: ${r.notas}" else ""

        // "Vamos en camino" solo tiene sentido si el pedido es despacho a domicilio
        holder.btnEnCamino.visibility = if (r.entrega == "despacho") View.VISIBLE else View.GONE

        val mensajeListo = mensajePanListo(r)

        holder.btnPanListoWhatsapp.setOnClickListener {
            enviarWhatsApp(holder.itemView.context, r, mensajeListo)
        }
        holder.btnPanListoCorreo.setOnClickListener {
            enviarCorreo(holder.itemView.context, r, "Tu pedido está listo — Horno & Miga", mensajeListo)
        }
        holder.btnEnCamino.setOnClickListener {
            val mensaje = "Hola ${r.nombre}, ya vamos en camino con tu pedido N.º ${r.id} 🚴. ¡Nos vemos pronto!"
            enviarWhatsApp(holder.itemView.context, r, mensaje)
        }
        holder.btnMarcarCompletado.setOnClickListener {
            confirmarYMarcarCompletado(holder.itemView.context, r)
        }
    }

    override fun getItemCount(): Int = reservas.size

    fun actualizarDatos(nuevas: List<Reserva>) {
        reservas = nuevas
        notifyDataSetChanged()
    }

    private fun confirmarYMarcarCompletado(context: Context, r: Reserva) {
        AlertDialog.Builder(context)
            .setTitle("¿Marcar como completado?")
            .setMessage("El pedido N.º ${r.id} de ${r.nombre} pasará a la lista de Completados.")
            .setPositiveButton("Sí, completar") { _, _ -> marcarCompletado(context, r) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun marcarCompletado(context: Context, r: Reserva) {
        db.collection("reservas").document(r.id)
            .update("estado", "completado")
            .addOnSuccessListener {
                Toast.makeText(context, "Pedido marcado como completado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Lo más probable: las reglas de Firestore todavía tienen
                // "allow update: if false;" — hay que cambiarlas a
                // "allow update: if request.auth != null;"
                Toast.makeText(
                    context,
                    "No se pudo actualizar (¿revisaste las reglas de Firestore para 'update'?): ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /** El texto de "pan listo" cambia según si es retiro en tienda o despacho a domicilio. */
    private fun mensajePanListo(r: Reserva): String {
        return if (r.entrega == "despacho") {
            "Hola ${r.nombre}, tu pan ya está listo 🍞. En breve sale a despacho hacia tu dirección. ¡Gracias por tu paciencia!"
        } else {
            "Hola ${r.nombre}, tu pan ya está listo para retiro 🍞. ¡Te esperamos en Horno & Miga!"
        }
    }

    private fun enviarWhatsApp(context: Context, r: Reserva, mensaje: String) {
        try {
            val telefono = limpiarTelefono(r.telefono)
            val uri = Uri.parse("https://wa.me/$telefono?text=${Uri.encode(mensaje)}")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enviarCorreo(context: Context, r: Reserva, asunto: String, mensaje: String) {
        if (r.email.isBlank()) {
            Toast.makeText(context, "Este cliente no dejó correo registrado", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(r.email))
                putExtra(Intent.EXTRA_SUBJECT, asunto)
                putExtra(Intent.EXTRA_TEXT, mensaje)
            }
            context.startActivity(emailIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir la app de correo", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Limpia el número ingresado por el cliente (puede venir con espacios, +, guiones)
     * y le antepone el código de país de Chile (56) si hace falta, para que wa.me
     * pueda abrir el chat correcto.
     */
    private fun limpiarTelefono(telefonoOriginal: String): String {
        val soloDigitos = telefonoOriginal.filter { it.isDigit() }
        return when {
            soloDigitos.startsWith("56") -> soloDigitos
            else -> "56$soloDigitos"
        }
    }
}