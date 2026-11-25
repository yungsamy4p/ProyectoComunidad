package com.yungsamy.comunidadconectada.models;

public class Mensaje {
    public String id;
    public String uidEmisor;
    public String nombreEmisor;
    public String viviendaEmisor;
    public String contenido;
    public String fechaHora;
    public long timestamp;

    public Mensaje() {}

    public Mensaje(String id, String uidEmisor, String nombreEmisor, String viviendaEmisor, String contenido, String fechaHora, long timestamp) {
        this.id = id;
        this.uidEmisor = uidEmisor;
        this.nombreEmisor = nombreEmisor;
        this.viviendaEmisor = viviendaEmisor;
        this.contenido = contenido;
        this.fechaHora = fechaHora;
        this.timestamp = timestamp;
    }
}