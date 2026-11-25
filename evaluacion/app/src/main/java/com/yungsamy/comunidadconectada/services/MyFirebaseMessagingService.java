package com.yungsamy.comunidadconectada.services;

import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.yungsamy.comunidadconectada.utils.NotificationHelper;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        String titulo = "Nuevo anuncio oficial";
        String cuerpo = "El administrador ha publicado información.";

        if (message.getNotification() != null) {
            titulo = message.getNotification().getTitle();
            cuerpo = message.getNotification().getBody();
        } else if (message.getData().containsKey("titulo")) {
            titulo = message.getData().get("titulo");
            cuerpo = message.getData().get("contenido");
        }

        NotificationHelper.crearCanales(this);
        NotificationHelper.mostrarNotificacion(
                this,
                NotificationHelper.CHANNEL_ANUNCIOS,
                (int) System.currentTimeMillis(),
                titulo,
                cuerpo,
                "anuncios"
        );
    }
}