package com.yungsamy.comunidadconectada.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import com.yungsamy.comunidadconectada.MainActivity;
import com.yungsamy.comunidadconectada.R;
import com.yungsamy.comunidadconectada.databinding.FragmentChatBinding;
import com.yungsamy.comunidadconectada.models.Mensaje;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatFragment extends Fragment {
    private FragmentChatBinding binding;
    private DatabaseReference chatRef;
    private Query chatQuery;
    private ValueEventListener chatListener;
    private final List<Mensaje> listaMensajes = new ArrayList<>();
    private ChatAdapter adapter;
    private int limiteMensajes = 20; // Paginación inicial

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);

        adapter = new ChatAdapter(listaMensajes);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        lm.setStackFromEnd(true);
        binding.rvChat.setLayoutManager(lm);
        binding.rvChat.setAdapter(adapter);

        chatRef = FirebaseDatabase.getInstance().getReference("comunidades/comunidad01/chat");
        escucharMensajes();

        binding.btnEnviarChat.setOnClickListener(v -> enviarMensaje());
        binding.btnCargarMasChat.setOnClickListener(v -> {
            limiteMensajes += 20;
            escucharMensajes();
        });

        return binding.getRoot();
    }

    private void escucharMensajes() {
        if (chatQuery != null && chatListener != null) {
            chatQuery.removeEventListener(chatListener);
        }
        chatQuery = chatRef.orderByChild("timestamp").limitToLast(limiteMensajes);
        chatListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaMensajes.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Mensaje m = ds.getValue(Mensaje.class);
                    if (m != null) listaMensajes.add(m);
                }
                adapter.notifyDataSetChanged();
                if (!listaMensajes.isEmpty()) {
                    binding.rvChat.scrollToPosition(listaMensajes.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error de lectura: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        chatQuery.addValueEventListener(chatListener);
    }

    private void enviarMensaje() {
        String texto = binding.etMensajeChat.getText().toString().trim();
        if (TextUtils.isEmpty(texto) || MainActivity.usuarioActual == null) return;

        String id = chatRef.push().getKey();
        String fecha = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
        Mensaje msg = new Mensaje(
                id,
                MainActivity.usuarioActual.uid,
                MainActivity.usuarioActual.nombre,
                MainActivity.usuarioActual.vivienda,
                texto,
                fecha,
                System.currentTimeMillis()
        );

        binding.etMensajeChat.setText("");
        if (id != null) {
            chatRef.child(id).setValue(msg)
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "No se pudo enviar", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatQuery != null && chatListener != null) {
            chatQuery.removeEventListener(chatListener);
        }
        binding = null;
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {
        private final List<Mensaje> items;
        ChatAdapter(List<Mensaje> items) { this.items = items; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mensaje, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Mensaje m = items.get(position);
            holder.tvEmisor.setText(m.nombreEmisor + " · #" + m.viviendaEmisor);
            holder.tvContenido.setText(m.contenido);
            holder.tvFecha.setText(m.fechaHora);
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvEmisor, tvContenido, tvFecha;
            VH(View v) {
                super(v);
                tvEmisor = v.findViewById(R.id.tvEmisorMensaje);
                tvContenido = v.findViewById(R.id.tvContenidoMensaje);
                tvFecha = v.findViewById(R.id.tvFechaMensaje);
            }
        }
    }
}