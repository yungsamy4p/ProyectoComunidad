package com.yungsamy.comunidadconectada.utils;

import java.util.Set;

public class ValidadorComunidad {

    public static boolean esRegistroValido(String nombre, String vivienda, String correo, String password) {
        if (nombre == null || nombre.trim().isEmpty()) return false;
        if (vivienda == null || !vivienda.trim().matches("^[a-zA-Z0-9-]+$")) return false;
        if (correo == null || !correo.trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) return false;
        if (password == null || password.length() < 6) return false;
        return true;
    }

    public static boolean esAlertaDuplicada(String alertaId, Set<String> historialProcesado) {
        if (alertaId == null || historialProcesado == null) return false;
        if (historialProcesado.contains(alertaId)) {
            return true;
        }
        historialProcesado.add(alertaId);
        return false;
    }

    public static String formatearIdentificacion(String nombre, String vivienda) {
        return nombre.trim() + " · #" + vivienda.trim().toUpperCase();
    }
}