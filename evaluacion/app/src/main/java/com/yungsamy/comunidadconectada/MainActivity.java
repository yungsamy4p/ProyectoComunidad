package com.yungsamy.comunidadconectada;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.messaging.FirebaseMessaging;
import com.yungsamy.comunidadconectada.databinding.ActivityMainBinding;
import com.yungsamy.comunidadconectada.fragments.*;
import com.yungsamy.comunidadconectada.models.Usuario;
import com.yungsamy.comunidadconectada.utils.MqttHelper;
import com.yungsamy.comunidadconectada.utils.NotificationHelper;

public class MainActivity extends AppCompatActivity implements MqttHelper.AlertaListener {
    private ActivityMainBinding binding;
    public static Usuario usuarioActual;
    public static MqttHelper mqttHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NotificationHelper.crearCanales(this);
        solicitarPermisoNotificaciones();

        // Suscribir al tópico FCM para recibir notificaciones de anuncios oficiales
        FirebaseMessaging.getInstance().subscribeToTopic("anuncios_comunidad01");

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        cargarPerfilYConectarMqtt(uid);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int id = item.getItemId();
            if (id == R.id.nav_chat) selected = new ChatFragment();
            else if (id == R.id.nav_anuncios) selected = new AnunciosFragment();
            else if (id == R.id.nav_alertas) selected = new AlertasFragment();
            else if (id == R.id.nav_perfil) selected = new PerfilFragment();

            if (selected != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selected)
                        .commit();
            }
            return true;
        });
    }

    private void cargarPerfilYConectarMqtt(String uid) {
        FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        usuarioActual = snapshot.getValue(Usuario.class);
                        if (usuarioActual != null && mqttHelper == null) {
                            mqttHelper = new MqttHelper(MainActivity.this, uid, MainActivity.this);
                            mqttHelper.conectar();

                            // Abrir pestaña según notificación tocada o Chat por defecto
                            String tab = getIntent().getStringExtra("abrir_tab");
                            if ("anuncios".equals(tab)) {
                                binding.bottomNav.setSelectedItemId(R.id.nav_anuncios);
                            } else if ("alertas".equals(tab)) {
                                binding.bottomNav.setSelectedItemId(R.id.nav_alertas);
                            } else {
                                binding.bottomNav.setSelectedItemId(R.id.nav_chat);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(MainActivity.this, "Error cargando perfil", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onNuevaAlertaRecibida(String id, String emisor, String vivienda, String fecha) {
        runOnUiThread(() -> {
            String cuerpo = emisor + " · Vivienda #" + vivienda + " ha solicitado ayuda (" + fecha + ")";
            NotificationHelper.mostrarNotificacion(
                    this,
                    NotificationHelper.CHANNEL_ALERTAS,
                    id.hashCode(),
                    "🚨 ALERTA COMUNITARIA",
                    cuerpo,
                    "alertas"
            );

            // Alerta visual inmediata si la app está abierta
            if (!isFinishing()) {
                new AlertDialog.Builder(this)
                        .setTitle("🚨 Alerta Comunitaria")
                        .setMessage(cuerpo)
                        .setPositiveButton("Ver Alertas", (d, w) -> binding.bottomNav.setSelectedItemId(R.id.nav_alertas))
                        .setNegativeButton("Cerrar", null)
                        .show();
            }
        });
    }

    @Override
    public void onEstadoConexionCambiado(boolean conectado) {
        runOnUiThread(() -> {
            if (conectado) {
                binding.tvEstadoMqtt.setText("MQTT: En línea");
                binding.tvEstadoMqtt.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_teal));
            } else {
                binding.tvEstadoMqtt.setText("MQTT: Desconectado");
                binding.tvEstadoMqtt.setBackgroundColor(ContextCompat.getColor(this, R.color.alert_red));
            }
        });
    }

    private void solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttHelper != null) {
            mqttHelper.desconectar();
            mqttHelper = null;
        }
    }
}