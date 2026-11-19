package com.example.agenda_tutoria.repository;

import com.example.agenda_tutoria.model.Tutoria;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TutoriaRepository extends MongoRepository<Tutoria, String> {
    List<Tutoria> findByEstudianteId(String estudianteId);
    List<Tutoria> findByProfesorId(String profesorId);
    List<Tutoria> findByEstado(Tutoria.Estado estado);
}
