package com.yungsamy.comunidadconectada.utils;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import info.mqtt.android.service.Ack;
import info.mqtt.android.service.MqttAndroidClient;

public class MqttHelper {
    private static final String TAG = "MqttHelper";

    // === DATOS ===
    private static final String BROKER_HOST = "a552166a.ala.us-east-1.emqxsl.com";
    private static final String USERNAME = "yungsamy";
    private static final String PASSWORD = "evaluacion";

    // ============================================================================

    private static final String BROKER_URL = "ssl://" + BROKER_HOST + ":8883";
    private static final String[] FALLBACK_URIS = new String[]{
            "ssl://" + BROKER_HOST + ":8883",
            "wss://" + BROKER_HOST + ":8084/mqtt"
    };

    private static final String TOPIC_ALERTAS = "comunidades/comunidad01/alertas";

    private MqttAndroidClient client;
    private final Context context;
    private final Set<String> alertasProcesadas = new HashSet<>();
    private final AlertaListener alertaListener;

    public interface AlertaListener {
        void onNuevaAlertaRecibida(String id, String emisor, String vivienda, String fecha);
        void onEstadoConexionCambiado(boolean conectado);
    }

    public MqttHelper(Context context, String uidUsuario, AlertaListener listener) {
        this.context = context.getApplicationContext();
        this.alertaListener = listener;

        // ClientID único por sesión para evitar que dos dispositivos se desconecten entre sí
        String sufijo = UUID.randomUUID().toString().substring(0, 6);
        String clientId = "android-" + uidUsuario + "-" + sufijo;

        this.client = new MqttAndroidClient(this.context, BROKER_URL, clientId, Ack.AUTO_ACK);
    }

    public void conectar() {
        if (client == null) return;

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(FALLBACK_URIS);
        options.setUserName(USERNAME);
        options.setPassword(PASSWORD.toCharArray());
        options.setAutomaticReconnect(true); // Reconexión automática ante cortes de red
        options.setCleanSession(true);       // Evita recibir mensajes viejos acumulados en sesión
        options.setConnectionTimeout(15);
        options.setKeepAliveInterval(30);

        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d(TAG, "Conectado a EMQX (" + serverURI + ") | Reconexión: " + reconnect);
                if (alertaListener != null) {
                    alertaListener.onEstadoConexionCambiado(true);
                }
                suscribirAlertas();
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.w(TAG, "Conexión MQTT perdida: " + (cause != null ? cause.getMessage() : "desconocida"));
                if (alertaListener != null) {
                    alertaListener.onEstadoConexionCambiado(false);
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                if (message != null && message.getPayload() != null) {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    procesarMensajeEntrante(payload);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Entrega confirmada por el broker (QoS 1)");
            }
        });

        try {
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Conexión inicial solicitada con éxito");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Error al conectar con el broker MQTT", exception);
                    if (alertaListener != null) {
                        alertaListener.onEstadoConexionCambiado(false);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Excepción al iniciar conexión MQTT", e);
            if (alertaListener != null) {
                alertaListener.onEstadoConexionCambiado(false);
            }
        }
    }

    private void suscribirAlertas() {
        try {
            if (client != null && client.isConnected()) {
                // QoS 1: garantiza entrega al menos una vez
                client.subscribe(TOPIC_ALERTAS, 1, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Suscrito correctamente a " + TOPIC_ALERTAS);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e(TAG, "Fallo al suscribir a " + TOPIC_ALERTAS, exception);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en suscripción MQTT", e);
        }
    }

    public void publicarAlerta(String alertaId, String uid, String nombre, String vivienda, String fechaHora, IMqttActionListener callback) {
        if (client == null || !client.isConnected()) {
            if (callback != null) {
                callback.onFailure(null, new IllegalStateException("Sin conexión al broker MQTT"));
            }
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("id", alertaId);
            json.put("comunidad", "comunidad01");
            json.put("uid", uid);
            json.put("nombre", nombre);
            json.put("vivienda", vivienda);
            json.put("fechaHora", fechaHora);
            json.put("estado", "Activa");

            MqttMessage message = new MqttMessage(json.toString().getBytes(StandardCharsets.UTF_8));
            message.setQos(1);         // QoS 1 obligatorio en rúbrica
            message.setRetained(false); // Sin retain para que alarmas antiguas no reaparezcan al suscribirse

            client.publish(TOPIC_ALERTAS, message, null, callback);
        } catch (Exception e) {
            Log.e(TAG, "Error construyendo o publicando alerta MQTT", e);
            if (callback != null) {
                callback.onFailure(null, e);
            }
        }
    }

    private void procesarMensajeEntrante(String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            String id = json.getString("id");

            // Filtro de idempotencia: evita duplicar alertas si QoS 1 reenvía el paquete
            if (alertasProcesadas.contains(id)) {
                Log.d(TAG, "Alerta duplicada ignorada: " + id);
                return;
            }
            alertasProcesadas.add(id);

            String nombre = json.getString("nombre");
            String vivienda = json.getString("vivienda");
            String fecha = json.getString("fechaHora");

            if (alertaListener != null) {
                alertaListener.onNuevaAlertaRecibida(id, nombre, vivienda, fecha);
            }
        } catch (Exception e) {
            Log.e(TAG, "Payload MQTT inválido o malformado: " + payload, e);
        }
    }

    public boolean estaConectado() {
        return client != null && client.isConnected();
    }

    public void desconectar() {
        try {
            if (client != null) {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
                client = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al desconectar cliente MQTT", e);
        }
    }
}