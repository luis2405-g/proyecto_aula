package com.example.agenda_tutoria.repository;

import com.example.agenda_tutoria.model.Notificacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {
    List<Notificacion> findByUsuarioIdOrderByFechaDesc(String usuarioId);
    List<Notificacion> findByUsuarioIdAndLeidaFalse(String usuarioId);
    long countByUsuarioIdAndLeidaFalse(String usuarioId);
}