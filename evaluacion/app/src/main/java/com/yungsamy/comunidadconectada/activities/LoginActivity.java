package com.yungsamy.comunidadconectada.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.yungsamy.comunidadconectada.MainActivity;
import com.yungsamy.comunidadconectada.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();


        if (mAuth.getCurrentUser() != null) {
            irAMain();
            return;
        }

        binding.btnIniciarSesion.setOnClickListener(v -> iniciarSesion());
        binding.tvIrARegistro.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        binding.tvOlvidePassword.setOnClickListener(v -> mostrarDialogoRecuperar());
    }

    private void iniciarSesion() {
        String correo = binding.etCorreoLogin.getText().toString().trim();
        String pass = binding.etPasswordLogin.getText().toString().trim();

        if (TextUtils.isEmpty(correo) || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.etCorreoLogin.setError("Ingresa un correo válido");
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            binding.etPasswordLogin.setError("Ingresa tu contraseña");
            return;
        }

        binding.progressLogin.setVisibility(View.VISIBLE);
        binding.btnIniciarSesion.setEnabled(false);

        mAuth.signInWithEmailAndPassword(correo, pass)
                .addOnSuccessListener(authResult -> irAMain())
                .addOnFailureListener(e -> {
                    binding.progressLogin.setVisibility(View.GONE);
                    binding.btnIniciarSesion.setEnabled(true);
                    Toast.makeText(this, "Error al iniciar sesión: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void mostrarDialogoRecuperar() {
        EditText inputCorreo = new EditText(this);
        inputCorreo.setHint("ejemplo@correo.com");

        new AlertDialog.Builder(this)
                .setTitle("Recuperar contraseña")
                .setMessage("Ingresa tu correo registrado para enviarte un enlace de restablecimiento:")
                .setView(inputCorreo)
                .setPositiveButton("Enviar enlace", (dialog, which) -> {
                    String correo = inputCorreo.getText().toString().trim();
                    if (!TextUtils.isEmpty(correo) && Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                        mAuth.sendPasswordResetEmail(correo)
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this, "Revisa tu correo electrónico para cambiar tu clave.", Toast.LENGTH_LONG).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "No se pudo enviar: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show());
                    } else {
                        Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void irAMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}