package com.yungsamy.comunidadconectada.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.yungsamy.comunidadconectada.MainActivity;
import com.yungsamy.comunidadconectada.activities.LoginActivity;
import com.yungsamy.comunidadconectada.databinding.FragmentPerfilBinding;

public class PerfilFragment extends Fragment {
    private FragmentPerfilBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPerfilBinding.inflate(inflater, container, false);

        if (MainActivity.usuarioActual != null) {
            binding.tvPerfilIdentificacion.setText(MainActivity.usuarioActual.getIdentificacionFormateada());
            binding.tvPerfilRol.setText("Rol: " + MainActivity.usuarioActual.rol.toUpperCase());
            binding.etPerfilNombre.setText(MainActivity.usuarioActual.nombre);
            binding.tvPerfilCorreo.setText("Correo: " + MainActivity.usuarioActual.correo);
        }

        binding.btnGuardarPerfil.setOnClickListener(v -> {
            String nuevoNombre = binding.etPerfilNombre.getText().toString().trim();
            if (TextUtils.isEmpty(nuevoNombre)) {
                binding.etPerfilNombre.setError("El nombre no puede estar vacío");
                return;
            }
            if (MainActivity.usuarioActual != null) {
                FirebaseDatabase.getInstance().getReference("usuarios")
                        .child(MainActivity.usuarioActual.uid)
                        .child("nombre")
                        .setValue(nuevoNombre)
                        .addOnSuccessListener(u -> {
                            binding.tvPerfilIdentificacion.setText(nuevoNombre + " · #" + MainActivity.usuarioActual.vivienda);
                            Toast.makeText(getContext(), "Nombre actualizado", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        binding.btnCerrarSesion.setOnClickListener(v -> {
            if (MainActivity.mqttHelper != null) {
                MainActivity.mqttHelper.desconectar();
                MainActivity.mqttHelper = null;
            }
            MainActivity.usuarioActual = null;
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}