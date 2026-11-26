package com.yungsamy.comunidadconectada.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MqttHelper {
    private static final String TAG = "MqttHelper";

    // Configuración EMQX Cloud Serverless (TLS 8883)
    private static final String BROKER_URI = "ssl://p8a9198a.ala.us-east-1.emqxsl.com";
    private static final String MQTT_USER = "yungsamy";
    private static final String MQTT_PASS = "evaluacion";
    public static final String TOPIC_ALERTAS = "comunidad/comunidad01/alertas";

    private MqttAsyncClient client;
    private final String clientId;
    private final AlertaListener listener;
    private final Set<String> alertasProcesadas = new HashSet<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AlertaListener {
        void onNuevaAlertaRecibida(String id, String emisor, String vivienda, String fecha);
        void onEstadoConexionCambiado(boolean conectado);
    }

    public MqttHelper(Context context, String uidUsuario, AlertaListener listener) {
        this.listener = listener;
        this.clientId = "android_" + uidUsuario + "_" + UUID.randomUUID().toString().substring(0, 6);
        try {
            this.client = new MqttAsyncClient(BROKER_URI, clientId, new MemoryPersistence());
            configurarCallbacks();
        } catch (MqttException e) {
            Log.e(TAG, "Error inicializando MqttAsyncClient: " + e.getMessage(), e);
        }
    }

    private void configurarCallbacks() {
        if (client == null) return;
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d(TAG, "Conectado a EMQX (reconexión=" + reconnect + ")");
                notificarEstado(true);
                suscribirAlertas();
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.w(TAG, "Conexión MQTT perdida: " + (cause != null ? cause.getMessage() : "desconocida"));
                notificarEstado(false);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                try {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    JSONObject json = new JSONObject(payload);
                    String id = json.optString("id", UUID.randomUUID().toString());

                    // Evita procesar dos veces la misma alerta (QoS 1)
                    if (ValidadorComunidad.esAlertaDuplicada(id, alertasProcesadas)) {
                        return;
                    }

                    String emisor = json.optString("nombreEmisor", "Vecino");
                    String vivienda = json.optString("viviendaEmisor", "S/N");
                    String fecha = json.optString("fechaHora", "");

                    if (listener != null) {
                        mainHandler.post(() -> listener.onNuevaAlertaRecibida(id, emisor, vivienda, fecha));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parseando payload MQTT: " + e.getMessage(), e);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });
    }

    public void conectar() {
        if (client == null || client.isConnected()) return;

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(MQTT_USER);
        options.setPassword(MQTT_PASS.toCharArray());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);

        try {
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    notificarEstado(true);
                    suscribirAlertas();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Fallo conexión EMQX: " + (exception != null ? exception.getMessage() : "error"));
                    notificarEstado(false);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Excepción al conectar MQTT: " + e.getMessage(), e);
            notificarEstado(false);
        }
    }

    private void suscribirAlertas() {
        try {
            if (client != null && client.isConnected()) {
                client.subscribe(TOPIC_ALERTAS, 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error suscribiendo a alertas: " + e.getMessage(), e);
        }
    }

    public void publicarAlerta(String id, String uid, String nombre, String vivienda, String fechaHora, IMqttActionListener callback) {
        if (client == null || !client.isConnected()) {
            if (callback != null) {
                callback.onFailure(null, new IllegalStateException("Cliente MQTT desconectado"));
            }
            return;
        }

        try {
            // Registrar como propia para no auto-notificarse en bucle
            alertasProcesadas.add(id);

            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("comunidad", "comunidad01");
            json.put("uidUsuario", uid);
            json.put("nombreEmisor", nombre);
            json.put("viviendaEmisor", vivienda);
            json.put("fechaHora", fechaHora);
            json.put("estado", "Activa");

            MqttMessage message = new MqttMessage(json.toString().getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            message.setRetained(false);

            client.publish(TOPIC_ALERTAS, message, null, callback);
        } catch (Exception e) {
            if (callback != null) {
                callback.onFailure(null, e);
            }
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
            }
        } catch (Exception e) {
            Log.e(TAG, "Error desconectando MQTT: " + e.getMessage());
        }
    }

    private void notificarEstado(boolean conectado) {
        if (listener != null) {
            mainHandler.post(() -> listener.onEstadoConexionCambiado(conectado));
        }
    }
}