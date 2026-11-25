package com.yungsamy.comunidadconectada.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import com.yungsamy.comunidadconectada.MainActivity;
import com.yungsamy.comunidadconectada.R;
import com.yungsamy.comunidadconectada.databinding.FragmentAnunciosBinding;
import com.yungsamy.comunidadconectada.models.Anuncio;
import com.yungsamy.comunidadconectada.utils.NotificationHelper;
import java.text.SimpleDateFormat;
import java.util.*;

public class AnunciosFragment extends Fragment {
    private FragmentAnunciosBinding binding;
    private DatabaseReference anunciosRef;
    private ValueEventListener anunciosListener;
    private final List<Anuncio> listaAnuncios = new ArrayList<>();
    private AnunciosAdapter adapter;
    private boolean cargaInicial = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAnunciosBinding.inflate(inflater, container, false);

        adapter = new AnunciosAdapter(listaAnuncios, this::mostrarDetalleAnuncio);
        binding.rvAnuncios.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAnuncios.setAdapter(adapter);

        // Control de rol en interfaz (respaldado por Firebase Rules)
        if (MainActivity.usuarioActual != null && "admin".equals(MainActivity.usuarioActual.rol)) {
            binding.btnNuevoAnuncio.setVisibility(View.VISIBLE);
            binding.tvAvisoSoloAdmin.setText("Modo Administrador: puedes publicar comunicados.");
        }

        anunciosRef = FirebaseDatabase.getInstance().getReference("comunidades/comunidad01/anuncios");
        cargarAnuncios();

        binding.btnNuevoAnuncio.setOnClickListener(v -> mostrarDialogoPublicar());

        return binding.getRoot();
    }

    private void cargarAnuncios() {
        anunciosListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int cantidadPrevia = listaAnuncios.size();
                listaAnuncios.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Anuncio a = ds.getValue(Anuncio.class);
                    if (a != null) listaAnuncios.add(0, a); // Más recientes arriba
                }
                adapter.notifyDataSetChanged();

                // Notificar al dispositivo cuando llega un anuncio nuevo en tiempo real
                if (!cargaInicial && listaAnuncios.size() > cantidadPrevia && getContext() != null) {
                    Anuncio ultimo = listaAnuncios.get(0);
                    NotificationHelper.mostrarNotificacion(
                            getContext(),
                            NotificationHelper.CHANNEL_ANUNCIOS,
                            ultimo.id.hashCode(),
                            "📢 Nuevo anuncio: " + ultimo.titulo,
                            ultimo.contenido,
                            "anuncios"
                    );
                }
                cargaInicial = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        anunciosRef.orderByChild("timestamp").addValueEventListener(anunciosListener);
    }

    private void mostrarDialogoPublicar() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        EditText etTitulo = new EditText(getContext());
        etTitulo.setHint("Título del anuncio");
        EditText etContenido = new EditText(getContext());
        etContenido.setHint("Contenido del comunicado");
        etContenido.setMinLines(3);

        layout.addView(etTitulo);
        layout.addView(etContenido);

        new AlertDialog.Builder(requireContext())
                .setTitle("Nuevo anuncio oficial")
                .setView(layout)
                .setPositiveButton("Publicar anuncio", (dialog, which) -> {
                    String titulo = etTitulo.getText().toString().trim();
                    String contenido = etContenido.getText().toString().trim();
                    if (TextUtils.isEmpty(titulo) || TextUtils.isEmpty(contenido)) {
                        Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String id = anunciosRef.push().getKey();
                    String fecha = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
                    Anuncio nuevo = new Anuncio(id, titulo, contenido, "Administrador", fecha, System.currentTimeMillis());

                    if (id != null) {
                        anunciosRef.child(id).setValue(nuevo)
                                .addOnSuccessListener(u -> Toast.makeText(getContext(), "Anuncio publicado", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Permiso denegado: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDetalleAnuncio(Anuncio a) {
        new AlertDialog.Builder(requireContext())
                .setTitle(a.titulo)
                .setMessage(a.autor + " · " + a.fecha + "\n\n" + a.contenido)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (anunciosRef != null && anunciosListener != null) {
            anunciosRef.removeEventListener(anunciosListener);
        }
        binding = null;
    }

    interface OnAnuncioClick { void onClick(Anuncio a); }

    private static class AnunciosAdapter extends RecyclerView.Adapter<AnunciosAdapter.VH> {
        private final List<Anuncio> items;
        private final OnAnuncioClick listener;
        AnunciosAdapter(List<Anuncio> items, OnAnuncioClick listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_anuncio, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Anuncio a = items.get(position);
            holder.tvTitulo.setText(a.titulo);
            holder.tvMeta.setText(a.autor + " · " + a.fecha);
            holder.tvCuerpo.setText(a.contenido);
            holder.itemView.setOnClickListener(v -> listener.onClick(a));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitulo, tvMeta, tvCuerpo;
            VH(View v) {
                super(v);
                tvTitulo = v.findViewById(R.id.tvTituloAnuncio);
                tvMeta = v.findViewById(R.id.tvMetaAnuncio);
                tvCuerpo = v.findViewById(R.id.tvCuerpoAnuncio);
            }
        }
    }
}