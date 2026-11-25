package com.yungsamy.comunidadconectada.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.yungsamy.comunidadconectada.MainActivity;
import com.yungsamy.comunidadconectada.databinding.ActivityRegisterBinding;
import com.yungsamy.comunidadconectada.models.Usuario;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("usuarios");

        binding.btnRegistrarme.setOnClickListener(v -> registrarUsuario());
        binding.tvVolverLogin.setOnClickListener(v -> finish());
    }

    private void registrarUsuario() {
        String nombre = binding.etNombreReg.getText().toString().trim();
        String vivienda = binding.etViviendaReg.getText().toString().trim().toUpperCase();
        String correo = binding.etCorreoReg.getText().toString().trim();
        String pass = binding.etPasswordReg.getText().toString().trim();

        if (TextUtils.isEmpty(nombre)) {
            binding.etNombreReg.setError("El nombre es obligatorio");
            return;
        }
        if (TextUtils.isEmpty(vivienda) || !vivienda.matches("^[a-zA-Z0-9-]+$")) {
            binding.etViviendaReg.setError("Ingresa una vivienda válida (ej. 311A)");
            return;
        }
        if (TextUtils.isEmpty(correo) || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.etCorreoReg.setError("Correo electrónico inválido");
            return;
        }
        if (pass.length() < 6) {
            binding.etPasswordReg.setError("La contraseña debe tener al menos 6 caracteres");
            return;
        }

        binding.progressReg.setVisibility(View.VISIBLE);
        binding.btnRegistrarme.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(correo, pass)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    Usuario nuevoUsuario = new Usuario(uid, nombre, vivienda, correo, "vecino", "comunidad01");

                    dbRef.child(uid).setValue(nuevoUsuario)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Bienvenido, " + nuevoUsuario.getIdentificacionFormateada(), Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                binding.progressReg.setVisibility(View.GONE);
                                binding.btnRegistrarme.setEnabled(true);
                                Toast.makeText(this, "Error guardando perfil: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.progressReg.setVisibility(View.GONE);
                    binding.btnRegistrarme.setEnabled(true);
                    Toast.makeText(this, "Error en registro: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
    }
}