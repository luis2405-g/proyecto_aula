package com.example.agenda_tutoria.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "tutorias")
public class Tutoria {

    public enum Estado {
        PENDIENTE,
        ACEPTADA,
        RECHAZADA,
        REPROGRAMADA,
        CANCELADA,
        FINALIZADA
    }

    @Id
    private String id;

    private String estudianteId;
    private String estudianteNombre;

    private String profesorId;
    private String profesorNombre;

    private LocalDateTime fechaHora;
    private Integer duracionMin;
    private String tema;
    private String materia;

    private Estado estado;
    private String comentarios;

    private Double precioTotal;
    private Boolean pagado;

    private Boolean calificada;

    private String mensajeTutor;
    private String linkSesion;

    public Tutoria() {
        this.pagado = false;
        this.calificada = false;
        this.estado = Estado.PENDIENTE;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEstudianteId() {
        return estudianteId;
    }

    public void setEstudianteId(String estudianteId) {
        this.estudianteId = estudianteId;
    }

    public String getEstudianteNombre() {
        return estudianteNombre;
    }

    public void setEstudianteNombre(String nombre) {
        this.estudianteNombre = nombre;
    }

    public String getProfesorId() {
        return profesorId;
    }

    public void setProfesorId(String profesorId) {
        this.profesorId = profesorId;
    }

    public String getProfesorNombre() {
        return profesorNombre;
    }

    public void setProfesorNombre(String nombre) {
        this.profesorNombre = nombre;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public Integer getDuracionMin() {
        return duracionMin;
    }

    public void setDuracionMin(Integer duracionMin) {
        this.duracionMin = duracionMin;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getMateria() {
        return materia;
    }

    public void setMateria(String materia) {
        this.materia = materia;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public String getComentarios() {
        return comentarios;
    }

    public void setComentarios(String comentarios) {
        this.comentarios = comentarios;
    }

    public Double getPrecioTotal() {
        return precioTotal;
    }

    public void setPrecioTotal(Double precioTotal) {
        this.precioTotal = precioTotal;
    }

    public Boolean getPagado() {
        return pagado;
    }

    public void setPagado(Boolean pagado) {
        this.pagado = pagado;
    }

    public Boolean getCalificada() {
        return calificada;
    }

    public void setCalificada(Boolean calificada) {
        this.calificada = calificada;
    }

    public String getMensajeTutor() {
        return mensajeTutor;
    }

    public void setMensajeTutor(String mensajeTutor) {
        this.mensajeTutor = mensajeTutor;
    }

    public String getLinkSesion() {
        return linkSesion;
    }

    public void setLinkSesion(String linkSesion) {
        this.linkSesion = linkSesion;
    }
}