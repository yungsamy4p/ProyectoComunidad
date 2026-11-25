package com.yungsamy.comunidadconectada.models;

public class Alerta {
    public String id;
    public String comunidad;
    public String uidUsuario;
    public String nombreEmisor;
    public String viviendaEmisor;
    public String fechaHora;
    public String estado;
    public String actualizadoPor;

    public Alerta() {}

    public Alerta(String id, String comunidad, String uidUsuario, String nombreEmisor, String viviendaEmisor, String fechaHora, String estado) {
        this.id = id;
        this.comunidad = comunidad;
        this.uidUsuario = uidUsuario;
        this.nombreEmisor = nombreEmisor;
        this.viviendaEmisor = viviendaEmisor;
        this.fechaHora = fechaHora;
        this.estado = estado;
    }
}