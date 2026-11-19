package com.example.agenda_tutoria.model;

import java.util.List;

public class ModeloRegresion {

    private boolean disponible;
    private int muestra;
    private double pendiente;
    private double intercepto;
    private double r2;
    private double promedioPrecio;
    private String formula;
    private List<PrediccionPrecio> predicciones;

    public ModeloRegresion() {
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public int getMuestra() {
        return muestra;
    }

    public void setMuestra(int muestra) {
        this.muestra = muestra;
    }

    public double getPendiente() {
        return pendiente;
    }

    public void setPendiente(double pendiente) {
        this.pendiente = pendiente;
    }

    public double getIntercepto() {
        return intercepto;
    }

    public void setIntercepto(double intercepto) {
        this.intercepto = intercepto;
    }

    public double getR2() {
        return r2;
    }

    public void setR2(double r2) {
        this.r2 = r2;
    }

    public double getPromedioPrecio() {
        return promedioPrecio;
    }

    public void setPromedioPrecio(double promedioPrecio) {
        this.promedioPrecio = promedioPrecio;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public List<PrediccionPrecio> getPredicciones() {
        return predicciones;
    }

    public void setPredicciones(List<PrediccionPrecio> predicciones) {
        this.predicciones = predicciones;
    }
}
