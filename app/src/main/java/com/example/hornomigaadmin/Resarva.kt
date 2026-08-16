package com.example.hornomigaadmin

// Modelo de una reserva tal como se guarda en Firestore desde el sitio web
// (ver js/app.js -> objeto `reservation`). Los valores por defecto son
// obligatorios para que Firestore pueda deserializar con toObject().

data class ReservaItem(
    val name: String = "",
    val qty: Long = 0,
    val price: Long = 0,
    val extras: List<String> = emptyList(),
    val extrasTotal: Long = 0
)

/**
 * Estados del flujo operativo de un pedido.
 *
 * Nota de compatibilidad: los datos viejos usaban solo "pendiente" /
 * "completado". Ahora el flujo es pendiente -> en_preparacion -> listo ->
 * entregado, más "cancelado" como salida aparte. `normalizar()` traduce
 * cualquier "completado" viejo a "entregado" para que no se pierdan pedidos
 * históricos al mostrar la grilla o los contadores.
 */
object Estado {
    const val PENDIENTE = "pendiente"
    const val EN_PREPARACION = "en_preparacion"
    const val LISTO = "listo"
    const val ENTREGADO = "entregado"
    const val CANCELADO = "cancelado"

    // Orden del flujo operativo normal (cancelado queda fuera del flujo).
    val FLUJO = listOf(PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO)

    fun etiqueta(estado: String): String = when (normalizar(estado)) {
        PENDIENTE -> "Pendiente"
        EN_PREPARACION -> "En preparación"
        LISTO -> "Listo"
        ENTREGADO -> "Entregado"
        CANCELADO -> "Cancelado"
        else -> "Pendiente"
    }

    fun colorRes(estado: String): Int = when (normalizar(estado)) {
        PENDIENTE -> R.color.estado_pendiente
        EN_PREPARACION -> R.color.estado_en_preparacion
        LISTO -> R.color.estado_listo
        ENTREGADO -> R.color.estado_entregado
        CANCELADO -> R.color.danger
        else -> R.color.estado_pendiente
    }

    /** Traduce el estado viejo "completado" al vocabulario nuevo. */
    fun normalizar(estado: String): String = if (estado == "completado") ENTREGADO else estado

    /** Siguiente estado del flujo, o null si ya está en el último paso. */
    fun siguiente(estado: String): String? {
        val i = FLUJO.indexOf(normalizar(estado))
        return if (i in 0 until FLUJO.lastIndex) FLUJO[i + 1] else null
    }
}

data class Reserva(
    val id: String = "",
    val createdAt: Long = 0,
    val items: List<ReservaItem> = emptyList(),
    val total: Long = 0,
    val fecha: String = "", // formato YYYY-MM-DD
    val hora: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val email: String = "",
    val notas: String = "",
    val entrega: String = "retiro", // "retiro" | "despacho"
    val direccionDespacho: String = "",
    val distanciaDespachoKm: Double? = null,
    val costoDespacho: Long = 0,
    val zonaDespacho: String = "",
    val estado: String = "pendiente", // ver objeto Estado arriba
    // Documento adjunto (p. ej. boleta). Si documentoUrl viene vacío, el
    // pedido no tiene documento. La carga del archivo (Firebase Storage)
    // no está implementada todavía: este campo solo permite MOSTRAR un
    // documento que ya exista en Firestore.
    val documentoNombre: String = "",
    val documentoUrl: String = ""
) {
    val estadoNormalizado: String get() = Estado.normalizar(estado)
}
