package com.yungsamy.comunidadconectada.models;

public class Anuncio {
    public String id;
    public String titulo;
    public String contenido;
    public String autor;
    public String fecha;
    public long timestamp;

    public Anuncio() {}

    public Anuncio(String id, String titulo, String contenido, String autor, String fecha, long timestamp) {
        this.id = id;
        this.titulo = titulo;
        this.contenido = contenido;
        this.autor = autor;
        this.fecha = fecha;
        this.timestamp = timestamp;
    }
}