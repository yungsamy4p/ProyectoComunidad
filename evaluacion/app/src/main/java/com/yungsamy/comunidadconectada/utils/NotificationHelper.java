package com.yungsamy.comunidadconectada.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.yungsamy.comunidadconectada.MainActivity;

public class NotificationHelper {
    public static final String CHANNEL_ALERTAS = "canal_alertas_mqtt";
    public static final String CHANNEL_ANUNCIOS = "canal_anuncios_fcm";

    public static void crearCanales(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);

            NotificationChannel canalAlertas = new NotificationChannel(
                    CHANNEL_ALERTAS, "Alertas Comunitarias", NotificationManager.IMPORTANCE_HIGH);
            canalAlertas.setDescription("Alertas de emergencia en tiempo real vía MQTT");
            canalAlertas.enableVibration(true);

            NotificationChannel canalAnuncios = new NotificationChannel(
                    CHANNEL_ANUNCIOS, "Anuncios Oficiales", NotificationManager.IMPORTANCE_DEFAULT);
            canalAnuncios.setDescription("Comunicados del administrador");

            manager.createNotificationChannel(canalAlertas);
            manager.createNotificationChannel(canalAnuncios);
        }
    }

    public static void mostrarNotificacion(Context context, String canalId, int notifId, String titulo, String cuerpo, String destinoTab) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("abrir_tab", destinoTab);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, notifId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, canalId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(cuerpo))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify(notifId, builder.build());
    }
}