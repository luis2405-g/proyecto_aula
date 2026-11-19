package com.example.agenda_tutoria.model;

public class PrediccionPrecio {

    private String id;
    private String nombre;
    private Integer aniosExperiencia;
    private Double precioActual;
    private Double precioSugerido;

    public PrediccionPrecio() {
    }

    public PrediccionPrecio(String id, String nombre, Integer aniosExperiencia,
                            Double precioActual, Double precioSugerido) {
        this.id = id;
        this.nombre = nombre;
        this.aniosExperiencia = aniosExperiencia;
        this.precioActual = precioActual;
        this.precioSugerido = precioSugerido;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getAniosExperiencia() {
        return aniosExperiencia;
    }

    public void setAniosExperiencia(Integer aniosExperiencia) {
        this.aniosExperiencia = aniosExperiencia;
    }

    public Double getPrecioActual() {
        return precioActual;
    }

    public void setPrecioActual(Double precioActual) {
        this.precioActual = precioActual;
    }

    public Double getPrecioSugerido() {
        return precioSugerido;
    }

    public void setPrecioSugerido(Double precioSugerido) {
        this.precioSugerido = precioSugerido;
    }

    public Double getDiferencia() {
        if (precioActual == null || precioSugerido == null) return 0.0;
        return precioSugerido - precioActual;
    }
}
