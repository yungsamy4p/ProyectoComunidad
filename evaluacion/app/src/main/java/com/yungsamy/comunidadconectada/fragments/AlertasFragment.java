package com.yungsamy.comunidadconectada.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import com.yungsamy.comunidadconectada.MainActivity;
import com.yungsamy.comunidadconectada.R;
import com.yungsamy.comunidadconectada.databinding.FragmentAlertasBinding;
import com.yungsamy.comunidadconectada.models.Alerta;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import java.text.SimpleDateFormat;
import java.util.*;

public class AlertasFragment extends Fragment {
    private FragmentAlertasBinding binding;
    private DatabaseReference alertasRef;
    private ValueEventListener alertasListener;
    private final List<Alerta> listaAlertas = new ArrayList<>();
    private AlertasAdapter adapter;
    private long ultimoEnvioTimestamp = 0; // Prevención de spam de alarmas

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAlertasBinding.inflate(inflater, container, false);

        adapter = new AlertasAdapter(listaAlertas, this::gestionarAlertaAdmin);
        binding.rvAlertas.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAlertas.setAdapter(adapter);

        alertasRef = FirebaseDatabase.getInstance().getReference("comunidades/comunidad01/alertas");
        escucharHistorialAlertas();

        binding.btnActivarAlarma.setOnClickListener(v -> confirmarActivacion());

        return binding.getRoot();
    }

    private void confirmarActivacion() {
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoEnvioTimestamp < 10000) {
            Toast.makeText(getContext(), "Espera 10 segundos antes de emitir otra alerta.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (MainActivity.mqttHelper == null || !MainActivity.mqttHelper.estaConectado()) {
            binding.tvEstadoEnvioAlerta.setText("❌ Sin conexión MQTT: no se envió la alerta.");
            binding.tvEstadoEnvioAlerta.setTextColor(ContextCompat.getColor(requireContext(), R.color.alert_red));
            Toast.makeText(getContext(), "No hay conexión con el broker MQTT. Revisa tu red.", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("🚨 ¿Enviar alerta a la comunidad?")
                .setMessage("Se notificará inmediatamente tu nombre y vivienda a todos los vecinos conectados.")
                .setPositiveButton("Enviar alerta", (dialog, which) -> ejecutarEnvioAlerta())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarEnvioAlerta() {
        if (MainActivity.usuarioActual == null) return;

        String alertaId = "ALT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

        binding.tvEstadoEnvioAlerta.setText("⏳ Enviando alerta vía MQTT...");
        binding.btnActivarAlarma.setEnabled(false);

        MainActivity.mqttHelper.publicarAlerta(
                alertaId,
                MainActivity.usuarioActual.uid,
                MainActivity.usuarioActual.nombre,
                MainActivity.usuarioActual.vivienda,
                fechaHora,
                new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        ultimoEnvioTimestamp = System.currentTimeMillis();
                        // Guardar registro histórico en Firebase Realtime Database
                        Alerta nueva = new Alerta(
                                alertaId, "comunidad01",
                                MainActivity.usuarioActual.uid,
                                MainActivity.usuarioActual.nombre,
                                MainActivity.usuarioActual.vivienda,
                                fechaHora, "Activa"
                        );
                        alertasRef.child(alertaId).setValue(nueva);

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                binding.btnActivarAlarma.setEnabled(true);
                                binding.tvEstadoEnvioAlerta.setText("✅ Alerta enviada (" + alertaId + ")");
                                binding.tvEstadoEnvioAlerta.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_teal));
                            });
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                binding.btnActivarAlarma.setEnabled(true);
                                binding.tvEstadoEnvioAlerta.setText("❌ Error al enviar alerta. Reintenta.");
                                binding.tvEstadoEnvioAlerta.setTextColor(ContextCompat.getColor(requireContext(), R.color.alert_red));
                            });
                        }
                    }
                }
        );
    }

    private void escucharHistorialAlertas() {
        alertasListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaAlertas.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Alerta al = ds.getValue(Alerta.class);
                    if (al != null) listaAlertas.add(0, al);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        alertasRef.addValueEventListener(alertasListener);
    }

    private void gestionarAlertaAdmin(Alerta alerta) {
        if (MainActivity.usuarioActual == null || !"admin".equals(MainActivity.usuarioActual.rol)) {
            Toast.makeText(getContext(), "Solo el administrador puede cambiar el estado de una alerta.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] opciones = {"Marcar como Atendida", "Cerrar alerta"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Gestión de Alerta " + alerta.id)
                .setItems(opciones, (dialog, which) -> {
                    String nuevoEstado = (which == 0) ? "Atendida" : "Cerrada";
                    alertasRef.child(alerta.id).child("estado").setValue(nuevoEstado)
                            .addOnSuccessListener(u -> Toast.makeText(getContext(), "Estado actualizado a: " + nuevoEstado, Toast.LENGTH_SHORT).show());
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (alertasRef != null && alertasListener != null) {
            alertasRef.removeEventListener(alertasListener);
        }
        binding = null;
    }

    interface OnAlertaClick { void onClick(Alerta a); }

    private static class AlertasAdapter extends RecyclerView.Adapter<AlertasAdapter.VH> {
        private final List<Alerta> items;
        private final OnAlertaClick listener;
        AlertasAdapter(List<Alerta> items, OnAlertaClick listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alerta, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Alerta a = items.get(position);
            holder.tvEmisor.setText(a.nombreEmisor + " · #" + a.viviendaEmisor);
            holder.tvDetalle.setText("ID: " + a.id + " · " + a.fechaHora);
            holder.tvEstado.setText(a.estado);

            int color = R.color.alert_red;
            if ("Atendida".equals(a.estado)) color = R.color.primary_teal;
            else if ("Cerrada".equals(a.estado)) color = R.color.text_secondary;
            holder.tvEstado.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), color));

            holder.itemView.setOnClickListener(v -> listener.onClick(a));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvEmisor, tvDetalle, tvEstado;
            VH(View v) {
                super(v);
                tvEmisor = v.findViewById(R.id.tvEmisorAlerta);
                tvDetalle = v.findViewById(R.id.tvDetalleAlerta);
                tvEstado = v.findViewById(R.id.tvEstadoAlerta);
            }
        }
    }
}