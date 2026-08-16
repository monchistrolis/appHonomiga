package com.example.hornomigaadmin

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class ReservasActivity : AppCompatActivity() {

    // --- Vistas ---
    private lateinit var slidingPane: SlidingPaneLayout
    private lateinit var llWeekGrid: LinearLayout
    private lateinit var tvRangoSemana: TextView
    private lateinit var chipGroup: ChipGroup
    private lateinit var tvStatPedidos: TextView
    private lateinit var tvStatTotal: TextView
    private lateinit var tvStatDocumentos: TextView
    private lateinit var tvTotalPeriodo: TextView
    private lateinit var tvResumenPendientes: TextView
    private lateinit var tvResumenPreparacion: TextView
    private lateinit var tvResumenListos: TextView
    private lateinit var tvResumenEntregados: TextView
    private lateinit var tvConsejoDelDia: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvVacioOError: TextView
    private lateinit var scrollLista: NestedScrollView

    // Panel de detalle (incluido vía <include>, ids únicos en todo el árbol)
    private lateinit var detailEmptyState: View
    private lateinit var detailContent: View
    private lateinit var tvDetPedidoId: TextView
    private lateinit var tvDetEstadoChip: TextView
    private lateinit var btnDetCerrar: ImageButton
    private lateinit var tvDetCliente: TextView
    private lateinit var tvDetTelefono: TextView
    private lateinit var btnDetWhatsapp: ImageButton
    private lateinit var tvDetFecha: TextView
    private lateinit var tvDetHora: TextView
    private lateinit var llDetProductos: LinearLayout
    private lateinit var tvDetTotal: TextView
    private lateinit var tvDetDocumentoNombre: TextView
    private lateinit var btnDetDocumentoDescargar: ImageButton
    private lateinit var tvDetEntrega: TextView
    private lateinit var tvDetNotas: TextView
    private lateinit var llDetStepper: LinearLayout
    private lateinit var btnAccionAvanzar: Button
    private lateinit var btnAccionCancelar: Button

    private val db = FirebaseFirestore.getInstance()
    private val formatoMoneda = NumberFormat.getInstance(Locale("es", "CL"))
    private val formatoFechaIso = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd

    private var todasLasReservas: List<Reserva> = emptyList()
    private var inicioSemana: LocalDate = lunesDeEstaSemana()
    private var filtroEstado: String? = null // null = "Todos"
    private var reservaSeleccionadaId: String? = null
    private var reservasSemanaFiltradas: List<Reserva> = emptyList()

    private val consejos = listOf(
        "¡Buen día! Recuerda revisar los pedidos pendientes y preparar todo con amor. 💛",
        "Un vistazo rápido a los pedidos de despacho te ahorra sorpresas más tarde.",
        "Marca los pedidos como 'Listos' apenas salgan del horno para avisar antes.",
        "Revisa los documentos adjuntos de los pedidos grandes antes de despachar.",
        "Los clientes agradecen un mensaje de WhatsApp cuando su pan está listo.",
        "Fin de semana ocupado: prioriza los pedidos con hora de retiro más próxima.",
        "Cada pedido completado es un cliente feliz. ¡Buen trabajo!"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reservas)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindViews()
        // Al arrancar mostramos solo la lista; en tablet SlidingPaneLayout
        // igual va a mostrar ambos paneles lado a lado automáticamente.
        slidingPane.closePane()

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener { cerrarSesion() }
        findViewById<Button>(R.id.btnCompletados).setOnClickListener {
            startActivity(Intent(this, CompletadosActivity::class.java))
        }
        findViewById<Button>(R.id.btnSemanaAnterior).setOnClickListener {
            inicioSemana = inicioSemana.minusWeeks(1)
            renderTodo()
        }
        findViewById<Button>(R.id.btnSemanaSiguiente).setOnClickListener {
            inicioSemana = inicioSemana.plusWeeks(1)
            renderTodo()
        }

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            filtroEstado = when (checkedIds.firstOrNull()) {
                R.id.chipPendientes -> Estado.PENDIENTE
                R.id.chipPreparacion -> Estado.EN_PREPARACION
                R.id.chipListos -> Estado.LISTO
                R.id.chipEntregados -> Estado.ENTREGADO
                else -> null
            }
            renderTodo()
        }

        btnDetCerrar.setOnClickListener { cerrarDetalle() }
        tvConsejoDelDia.text = consejos[LocalDate.now().dayOfWeek.value % consejos.size]

        cargarReservas()
    }

    private fun bindViews() {
        slidingPane = findViewById(R.id.slidingPane)
        llWeekGrid = findViewById(R.id.llWeekGrid)
        tvRangoSemana = findViewById(R.id.tvRangoSemana)
        chipGroup = findViewById(R.id.chipGroupFiltros)
        tvStatPedidos = findViewById(R.id.tvStatPedidos)
        tvStatTotal = findViewById(R.id.tvStatTotal)
        tvStatDocumentos = findViewById(R.id.tvStatDocumentos)
        tvTotalPeriodo = findViewById(R.id.tvTotalPeriodo)
        tvResumenPendientes = findViewById(R.id.tvResumenPendientes)
        tvResumenPreparacion = findViewById(R.id.tvResumenPreparacion)
        tvResumenListos = findViewById(R.id.tvResumenListos)
        tvResumenEntregados = findViewById(R.id.tvResumenEntregados)
        tvConsejoDelDia = findViewById(R.id.tvConsejoDelDia)
        progressBar = findViewById(R.id.progressBar)
        tvVacioOError = findViewById(R.id.tvVacioOError)
        scrollLista = findViewById(R.id.scrollLista)

        detailEmptyState = findViewById(R.id.detailEmptyState)
        detailContent = findViewById(R.id.detailContent)
        tvDetPedidoId = findViewById(R.id.tvDetPedidoId)
        tvDetEstadoChip = findViewById(R.id.tvDetEstadoChip)
        btnDetCerrar = findViewById(R.id.btnDetCerrar)
        tvDetCliente = findViewById(R.id.tvDetCliente)
        tvDetTelefono = findViewById(R.id.tvDetTelefono)
        btnDetWhatsapp = findViewById(R.id.btnDetWhatsapp)
        tvDetFecha = findViewById(R.id.tvDetFecha)
        tvDetHora = findViewById(R.id.tvDetHora)
        llDetProductos = findViewById(R.id.llDetProductos)
        tvDetTotal = findViewById(R.id.tvDetTotal)
        tvDetDocumentoNombre = findViewById(R.id.tvDetDocumentoNombre)
        btnDetDocumentoDescargar = findViewById(R.id.btnDetDocumentoDescargar)
        tvDetEntrega = findViewById(R.id.tvDetEntrega)
        tvDetNotas = findViewById(R.id.tvDetNotas)
        llDetStepper = findViewById(R.id.llDetStepper)
        btnAccionAvanzar = findViewById(R.id.btnAccionAvanzar)
        btnAccionCancelar = findViewById(R.id.btnAccionCancelar)
    }

    private fun cerrarSesion() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun cargarReservas() {
        progressBar.visibility = View.VISIBLE
        tvVacioOError.visibility = View.GONE

        // Escucha en tiempo real: si llega un pedido nuevo o cambia de estado
        // desde el sitio web u otro dispositivo, la grilla se refresca sola.
        db.collection("reservas")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                progressBar.visibility = View.GONE
                if (error != null) {
                    tvVacioOError.text = "No pudimos cargar las reservas. Revisa tu conexión o los permisos de Firestore."
                    tvVacioOError.visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                // Los pedidos cancelados no se muestran en la vista operativa semanal.
                todasLasReservas = snapshot?.documents?.mapNotNull { it.toObject(Reserva::class.java) }
                    ?.filter { it.estadoNormalizado != Estado.CANCELADO }
                    ?: emptyList()

                renderTodo()
            }
    }

    private fun lunesDeEstaSemana(): LocalDate {
        val hoy = LocalDate.now()
        return hoy.minusDays((hoy.dayOfWeek.value - 1).toLong())
    }

    private fun parseFecha(fecha: String): LocalDate? =
        runCatching { LocalDate.parse(fecha, formatoFechaIso) }.getOrNull()

    private fun renderTodo() {
        val finSemana = inicioSemana.plusDays(6) // lunes .. domingo
        tvRangoSemana.text = formatearRangoSemana(inicioSemana, finSemana)

        val reservasSemana = todasLasReservas.filter { r ->
            val fecha = parseFecha(r.fecha)
            fecha != null && !fecha.isBefore(inicioSemana) && !fecha.isAfter(finSemana)
        }

        actualizarChips(reservasSemana)

        reservasSemanaFiltradas = if (filtroEstado == null) {
            reservasSemana
        } else {
            reservasSemana.filter { it.estadoNormalizado == filtroEstado }
        }

        pintarGrilla(reservasSemanaFiltradas)
        actualizarStats(reservasSemanaFiltradas)
        actualizarResumenYPie(reservasSemana)

        if (reservasSemana.isEmpty()) {
            tvVacioOError.text = "No hay pedidos en esta semana."
            tvVacioOError.visibility = View.VISIBLE
        } else {
            tvVacioOError.visibility = View.GONE
        }

        // Si el pedido seleccionado ya no está en la semana/filtro actual, cerramos el detalle.
        val seleccionado = reservaSeleccionadaId
        if (seleccionado != null && todasLasReservas.none { it.id == seleccionado }) {
            cerrarDetalle()
        } else if (seleccionado != null) {
            todasLasReservas.find { it.id == seleccionado }?.let { mostrarDetalle(it) }
        }
    }

    private fun formatearRangoSemana(inicio: LocalDate, fin: LocalDate): String {
        val locale = Locale("es", "CL")
        val fechaFinFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", locale)
        return "${inicio.dayOfMonth} – ${fin.format(fechaFinFormatter)}"
    }

    private fun actualizarChips(reservasSemana: List<Reserva>) {
        val porEstado = reservasSemana.groupingBy { it.estadoNormalizado }.eachCount()
        findViewById<Chip>(R.id.chipTodos).text = "Todos (${reservasSemana.size})"
        findViewById<Chip>(R.id.chipPendientes).text = "Pendientes (${porEstado[Estado.PENDIENTE] ?: 0})"
        findViewById<Chip>(R.id.chipPreparacion).text = "En preparación (${porEstado[Estado.EN_PREPARACION] ?: 0})"
        findViewById<Chip>(R.id.chipListos).text = "Listos (${porEstado[Estado.LISTO] ?: 0})"
        findViewById<Chip>(R.id.chipEntregados).text = "Entregados (${porEstado[Estado.ENTREGADO] ?: 0})"
    }

    private fun actualizarStats(reservas: List<Reserva>) {
        tvStatPedidos.text = reservas.size.toString()
        tvStatTotal.text = "$${formatoMoneda.format(reservas.sumOf { it.total })}"
        tvStatDocumentos.text = reservas.count { it.documentoUrl.isNotBlank() }.toString()
    }

    private fun actualizarResumenYPie(reservasSemana: List<Reserva>) {
        val porEstado = reservasSemana.groupingBy { it.estadoNormalizado }.eachCount()
        tvTotalPeriodo.text = "$${formatoMoneda.format(reservasSemana.sumOf { it.total })}"
        tvResumenPendientes.text = "Pendientes ${porEstado[Estado.PENDIENTE] ?: 0}"
        tvResumenPreparacion.text = "En preparación ${porEstado[Estado.EN_PREPARACION] ?: 0}"
        tvResumenListos.text = "Listos ${porEstado[Estado.LISTO] ?: 0}"
        tvResumenEntregados.text = "Entregados ${porEstado[Estado.ENTREGADO] ?: 0}"
    }

    private val diasAbreviados = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    private fun pintarGrilla(reservas: List<Reserva>) {
        llWeekGrid.removeAllViews()
        val inflater = layoutInflater

        for (i in 0..6) {
            val fechaDia = inicioSemana.plusDays(i.toLong())
            val nombreDia = diasAbreviados[i]

            val pedidosDia = reservas.filter { parseFecha(it.fecha) == fechaDia }.sortedBy { it.hora }

            val columna = inflater.inflate(R.layout.item_columna_dia, llWeekGrid, false) as LinearLayout
            columna.findViewById<TextView>(R.id.tvNombreDia).text = nombreDia
            columna.findViewById<TextView>(R.id.tvNumeroDia).text = fechaDia.dayOfMonth.toString()
            val contenedorTarjetas = columna.findViewById<LinearLayout>(R.id.llTarjetasDia)

            if (pedidosDia.isEmpty()) {
                val tvVacio = TextView(this).apply {
                    text = "—"
                    setTextColor(getColor(R.color.ink_soft))
                    gravity = Gravity.CENTER
                    setPadding(0, 24, 0, 0)
                }
                contenedorTarjetas.addView(tvVacio)
            } else {
                for (r in pedidosDia) {
                    val tarjeta = inflater.inflate(R.layout.item_tarjeta_pedido, contenedorTarjetas, false)
                    tarjeta.findViewById<TextView>(R.id.tvHora).text = r.hora
                    tarjeta.findViewById<TextView>(R.id.tvNombreCliente).text = r.nombre

                    val resumenItems = if (r.items.size == 1) {
                        val item = r.items[0]
                        val extras = if (item.extras.isNotEmpty()) " (+ ${item.extras.joinToString(", ")})" else ""
                        "${item.qty}× ${item.name}$extras"
                    } else {
                        "${r.items.sumOf { it.qty }} productos"
                    }
                    tarjeta.findViewById<TextView>(R.id.tvResumenItems).text = resumenItems
                    tarjeta.findViewById<TextView>(R.id.tvTotalTarjeta).text = "$${formatoMoneda.format(r.total)}"
                    tarjeta.findViewById<TextView>(R.id.tvEstadoTarjeta).apply {
                        text = Estado.etiqueta(r.estadoNormalizado)
                        setTextColor(getColor(Estado.colorRes(r.estadoNormalizado)))
                    }

                    val card = tarjeta.findViewById<MaterialCardView>(R.id.cardPedido)
                    card.setStrokeColor(getColor(if (r.entrega == "despacho") R.color.rye else R.color.gold))
                    card.setCardBackgroundColor(
                        getColor(if (reservaSeleccionadaId == r.id) R.color.gold_soft else R.color.paper)
                    )

                    tarjeta.setOnClickListener { seleccionarReserva(r) }
                    contenedorTarjetas.addView(tarjeta)
                }
            }
            llWeekGrid.addView(columna)
        }
    }

    private fun seleccionarReserva(r: Reserva) {
        reservaSeleccionadaId = r.id
        mostrarDetalle(r)
        pintarGrilla(reservasSemanaFiltradas) // repinta para resaltar la tarjeta elegida
        slidingPane.openPane()
    }

    private fun cerrarDetalle() {
        reservaSeleccionadaId = null
        detailContent.visibility = View.GONE
        detailEmptyState.visibility = View.VISIBLE
        slidingPane.closePane()
        pintarGrilla(reservasSemanaFiltradas)
    }

    private fun mostrarDetalle(r: Reserva) {
        detailEmptyState.visibility = View.GONE
        detailContent.visibility = View.VISIBLE

        val numeroCorto = if (r.id.length > 4) r.id.takeLast(4) else r.id
        tvDetPedidoId.text = "Pedido #$numeroCorto"
        tvDetEstadoChip.text = Estado.etiqueta(r.estadoNormalizado)
        tvDetEstadoChip.backgroundTintList = ColorStateList.valueOf(getColor(Estado.colorRes(r.estadoNormalizado)))

        tvDetCliente.text = r.nombre
        tvDetTelefono.text = r.telefono
        btnDetWhatsapp.setOnClickListener { abrirWhatsapp(r) }

        val fecha = parseFecha(r.fecha)
        val formatterLegible = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", Locale("es", "CL"))
        tvDetFecha.text = fecha?.let {
            formatterLegible.format(it).replaceFirstChar { c -> c.titlecase(Locale("es", "CL")) }
        } ?: r.fecha
        tvDetHora.text = r.hora

        llDetProductos.removeAllViews()
        for (item in r.items) {
            val fila = layoutInflater.inflate(R.layout.item_fila_producto, llDetProductos, false)
            fila.findViewById<TextView>(R.id.tvCantidad).text = item.qty.toString()
            fila.findViewById<TextView>(R.id.tvNombreProducto).text = item.name
            fila.findViewById<TextView>(R.id.tvExtrasProducto).apply {
                if (item.extras.isNotEmpty()) {
                    text = "+ ${item.extras.joinToString(", ")}"
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }
            fila.findViewById<TextView>(R.id.tvPrecioProducto).text =
                "$${formatoMoneda.format(item.price + item.extrasTotal)}"
            llDetProductos.addView(fila)
        }
        tvDetTotal.text = "$${formatoMoneda.format(r.total)}"

        if (r.documentoUrl.isNotBlank()) {
            tvDetDocumentoNombre.text = r.documentoNombre.ifBlank { "Documento adjunto" }
            btnDetDocumentoDescargar.visibility = View.VISIBLE
            btnDetDocumentoDescargar.setOnClickListener {
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.documentoUrl)))
                }.onFailure {
                    Toast.makeText(this, "No se pudo abrir el documento", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            tvDetDocumentoNombre.text = "Sin documento adjunto"
            btnDetDocumentoDescargar.visibility = View.GONE
        }

        tvDetEntrega.text = if (r.entrega == "despacho") {
            "Despacho a: ${r.direccionDespacho}"
        } else {
            "Retiro en local"
        }
        tvDetNotas.text = r.notas.ifBlank { "Sin notas adicionales" }

        pintarStepper(r.estadoNormalizado)
        configurarAcciones(r)
    }

    private fun pintarStepper(estadoActual: String) {
        llDetStepper.removeAllViews()
        val indiceActual = Estado.FLUJO.indexOf(estadoActual).coerceAtLeast(0)
        Estado.FLUJO.forEachIndexed { i, estado ->
            val paso = layoutInflater.inflate(R.layout.item_paso_estado, llDetStepper, false)
            paso.findViewById<TextView>(R.id.tvEtiquetaPaso).text = Estado.etiqueta(estado)
            val circulo = paso.findViewById<View>(R.id.viewCirculoPaso)
            val colorRes = if (i <= indiceActual) Estado.colorRes(estado) else R.color.ink_soft
            circulo.backgroundTintList = ColorStateList.valueOf(getColor(colorRes))
            llDetStepper.addView(paso)
        }
    }

    private fun configurarAcciones(r: Reserva) {
        val siguiente = Estado.siguiente(r.estadoNormalizado)
        if (siguiente != null) {
            btnAccionAvanzar.visibility = View.VISIBLE
            btnAccionAvanzar.text = "Marcar como ${Estado.etiqueta(siguiente).lowercase(Locale("es", "CL"))}"
            btnAccionAvanzar.setOnClickListener { actualizarEstado(r, siguiente) }
        } else {
            btnAccionAvanzar.visibility = View.GONE
        }

        btnAccionCancelar.visibility = if (r.estadoNormalizado == Estado.ENTREGADO) View.GONE else View.VISIBLE
        btnAccionCancelar.setOnClickListener { confirmarCancelar(r) }
    }

    private fun actualizarEstado(r: Reserva, nuevoEstado: String) {
        db.collection("reservas").document(r.id)
            .update("estado", nuevoEstado)
            .addOnSuccessListener {
                Toast.makeText(this, "Pedido actualizado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Si esto falla, probablemente falta permitir "update" para
                // este campo en las reglas de Firestore (mismo caso que ya
                // documentamos para "estado" -> "completado").
                Toast.makeText(this, "No se pudo actualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun confirmarCancelar(r: Reserva) {
        AlertDialog.Builder(this)
            .setTitle("¿Cancelar pedido?")
            .setMessage("El pedido N.º ${r.id} de ${r.nombre} se marcará como cancelado y saldrá de la vista semanal.")
            .setPositiveButton("Sí, cancelar") { _, _ ->
                actualizarEstado(r, Estado.CANCELADO)
                cerrarDetalle()
            }
            .setNegativeButton("Volver", null)
            .show()
    }

    private fun abrirWhatsapp(r: Reserva) {
        val soloDigitos = r.telefono.filter { it.isDigit() }
        val telefono = if (soloDigitos.startsWith("56")) soloDigitos else "56$soloDigitos"
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$telefono")))
        }.onFailure {
            Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }
}