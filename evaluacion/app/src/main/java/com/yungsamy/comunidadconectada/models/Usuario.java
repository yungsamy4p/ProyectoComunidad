package com.yungsamy.comunidadconectada.models;

public class Usuario {
    public String uid;
    public String nombre;
    public String vivienda;
    public String correo;
    public String rol;
    public String comunidad;

    public Usuario() {}

    public Usuario(String uid, String nombre, String vivienda, String correo, String rol, String comunidad) {
        this.uid = uid;
        this.nombre = nombre;
        this.vivienda = vivienda;
        this.correo = correo;
        this.rol = rol;
        this.comunidad = comunidad;
    }

    public String getIdentificacionFormateada() {
        return nombre + " · #" + vivienda;
    }
}