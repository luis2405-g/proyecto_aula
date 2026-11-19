package com.example.agenda_tutoria.repository;

import com.example.agenda_tutoria.model.Calificacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CalificacionRepository extends MongoRepository<Calificacion, String> {

    List<Calificacion> findByProfesorIdOrderByFechaDesc(String profesorId);

    boolean existsByTutoriaId(String tutoriaId);
}
