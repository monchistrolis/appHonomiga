const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const TOPIC_NUEVOS_PEDIDOS = "nuevos_pedidos";

exports.notificarNuevoPedido = onDocumentCreated(
  "reservas/{reservaId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const data = snapshot.data();

    const tipoEntrega = data.entrega === "despacho" ? "Despacho" : "Retiro";
    const nombre = data.nombre || "Cliente";
    const id = data.id || event.params.reservaId;

    const mensaje = {
      topic: TOPIC_NUEVOS_PEDIDOS,
      notification: {
        title: "Nuevo pedido 🍞",
        body: `${nombre} — ${tipoEntrega} — N.º ${id}`,
      },
      android: {
        priority: "high",
        notification: {
          sound: "default",
        },
      },
    };

    try {
      await getMessaging().send(mensaje);
    } catch (err) {
      console.error("Error enviando notificación:", err);
    }
  }
);