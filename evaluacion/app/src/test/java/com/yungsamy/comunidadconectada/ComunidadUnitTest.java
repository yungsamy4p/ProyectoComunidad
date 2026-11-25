package com.yungsamy.comunidadconectada;

import com.yungsamy.comunidadconectada.models.Alerta;
import com.yungsamy.comunidadconectada.utils.ValidadorComunidad;

import org.junit.Test;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ComunidadUnitTest {

    @Test
    public void registroConCamposVacios_debeFallar() {
        assertFalse(ValidadorComunidad.esRegistroValido("", "311A", "vecino@correo.cl", "123456"));
        assertFalse(ValidadorComunidad.esRegistroValido("Profe Benja", "", "vecino@correo.cl", "123456"));
        assertFalse(ValidadorComunidad.esRegistroValido("Profe Benja", "311A", "", "123456"));
        assertFalse(ValidadorComunidad.esRegistroValido("Profe Benja", "311A", "vecino@correo.cl", "123"));
    }

    @Test
    public void registroConViviendaAlfanumerica311A_debeSerValido() {
        boolean resultado = ValidadorComunidad.esRegistroValido(
                "Profe Benja",
                "311A",
                "profe.benja@correo.cl",
                "ClaveSegura123"
        );
        assertTrue(resultado);
        assertEquals("Profe Benja · #311A", ValidadorComunidad.formatearIdentificacion("Profe Benja", "311a"));
    }

    @Test
    public void deteccionDeAlertaDuplicadaMqtt_debeIgnorarRepetidos() {
        Set<String> procesadas = new HashSet<>();
        String idAlerta = "ALT-9981A";

        // Primera recepción: debe procesarse (no es duplicada)
        assertFalse(ValidadorComunidad.esAlertaDuplicada(idAlerta, procesadas));

        // Segunda recepción del mismo ID (simula reenvío de QoS 1): debe marcarse como duplicada
        assertTrue(ValidadorComunidad.esAlertaDuplicada(idAlerta, procesadas));
    }

    @Test
    public void creacionDeModeloAlerta_asignaCamposCorrectamente() {
        Alerta alerta = new Alerta(
                "ALT-1042",
                "comunidad01",
                "uid_123",
                "Samy",
                "402B",
                "25/11/2026 20:45:00",
                "Activa"
        );
        assertEquals("ALT-1042", alerta.id);
        assertEquals("Activa", alerta.estado);
        assertEquals("402B", alerta.viviendaEmisor);
    }
}