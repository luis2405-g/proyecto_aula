package com.example.agenda_tutoria.service;

import com.example.agenda_tutoria.model.ModeloRegresion;
import com.example.agenda_tutoria.model.PrediccionPrecio;
import com.example.agenda_tutoria.model.Usuario;
import com.example.agenda_tutoria.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo predictivo sencillo de regresión lineal simple.
 *
 * Variable independiente (x): años de experiencia del tutor.
 * Variable dependiente (y): precio por hora.
 *
 * El modelo aprende con los tutores registrados y sugiere un precio
 * para un tutor según su experiencia.
 */
@Service
public class RegresionLinealService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public ModeloRegresion entrenar() {
        List<Usuario> tutores = usuarioRepository.findByRol("PROFESOR").stream()
                .filter(t -> t.getPrecioPorHora() != null && t.getAniosExperiencia() != null)
                .toList();

        int n = tutores.size();
        double sumaX = 0, sumaY = 0, sumaXY = 0, sumaX2 = 0;
        for (Usuario t : tutores) {
            double x = t.getAniosExperiencia();
            double y = t.getPrecioPorHora();
            sumaX += x;
            sumaY += y;
            sumaXY += x * y;
            sumaX2 += x * x;
        }

        double promedio = n > 0 ? sumaY / n : 0.0;
        double denominador = n * sumaX2 - sumaX * sumaX;
        boolean disponible = n >= 2 && Math.abs(denominador) > 0.000001;

        double pendiente = 0.0;
        double intercepto = promedio;
        if (disponible) {
            pendiente = (n * sumaXY - sumaX * sumaY) / denominador;
            intercepto = (sumaY - pendiente * sumaX) / n;
        }

        double r2 = 0.0;
        if (disponible) {
            double mediaY = sumaY / n;
            double ssTotal = 0.0;
            double ssResidual = 0.0;
            for (Usuario t : tutores) {
                double x = t.getAniosExperiencia();
                double y = t.getPrecioPorHora();
                double predicho = intercepto + pendiente * x;
                ssTotal += Math.pow(y - mediaY, 2);
                ssResidual += Math.pow(y - predicho, 2);
            }
            r2 = ssTotal == 0 ? 1.0 : 1.0 - (ssResidual / ssTotal);
        }

        List<PrediccionPrecio> predicciones = new ArrayList<>();
        for (Usuario t : tutores) {
            double sugerido = disponible
                    ? intercepto + pendiente * t.getAniosExperiencia()
                    : promedio;
            predicciones.add(new PrediccionPrecio(
                    t.getId(),
                    t.getNombre(),
                    t.getAniosExperiencia(),
                    t.getPrecioPorHora(),
                    redondearPrecio(sugerido)));
        }

        ModeloRegresion modelo = new ModeloRegresion();
        modelo.setDisponible(disponible);
        modelo.setMuestra(n);
        modelo.setPendiente(pendiente);
        modelo.setIntercepto(intercepto);
        modelo.setR2(r2);
        modelo.setPromedioPrecio(promedio);
        modelo.setPredicciones(predicciones);
        modelo.setFormula(disponible
                ? String.format("precio = $%,.0f + $%,.0f × años de experiencia", intercepto, pendiente)
                : "Se necesitan al menos 2 tutores con experiencia y precio para entrenar el modelo.");
        return modelo;
    }

    public PrediccionPrecio predecirPara(Usuario profesor) {
        ModeloRegresion modelo = entrenar();
        return modelo.getPredicciones().stream()
                .filter(p -> profesor.getId() != null && profesor.getId().equals(p.getId()))
                .findFirst()
                .orElseGet(() -> new PrediccionPrecio(
                        profesor.getId(),
                        profesor.getNombre(),
                        profesor.getAniosExperiencia(),
                        profesor.getPrecioPorHora(),
                        profesor.getPrecioPorHora()));
    }

    private double redondearPrecio(double valor) {
        double redondeado = Math.round(valor / 1000.0) * 1000.0;
        return Math.max(0, redondeado);
    }
}
