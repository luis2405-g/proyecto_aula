package com.example.agenda_tutoria.controller;

import com.example.agenda_tutoria.model.Notificacion;
import com.example.agenda_tutoria.model.SolicitudRetiro;
import com.example.agenda_tutoria.model.Usuario;
import com.example.agenda_tutoria.repository.NotificacionRepository;
import com.example.agenda_tutoria.repository.SolicitudRetiroRepository;
import com.example.agenda_tutoria.repository.TutoriaRepository;
import com.example.agenda_tutoria.repository.UsuarioRepository;
import com.example.agenda_tutoria.service.NotificacionService;
import com.example.agenda_tutoria.service.RegresionLinealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TutoriaRepository tutoriaRepository;
    @Autowired private NotificacionService notificacionService;
    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private SolicitudRetiroRepository solicitudRetiroRepository;
    @Autowired private RegresionLinealService regresionLinealService;

    // ───────────────────────── DASHBOARD ─────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsuarios", usuarioRepository.count());
        model.addAttribute("totalTutorias", tutoriaRepository.count());
        model.addAttribute("solicitudesPendientes",
                usuarioRepository.findByEstadoSolicitudTutor("PENDIENTE").size());
        return "admin/dashboard";
    }

    // ───────────────────────── MODELO PREDICTIVO ─────────────────────────
    @GetMapping("/prediccion")
    public String prediccion(Model model) {
        model.addAttribute("modelo", regresionLinealService.entrenar());
        model.addAttribute("solicitudesPendientes",
                usuarioRepository.findByEstadoSolicitudTutor("PENDIENTE").size());
        return "admin/prediccion";
    }

    // ───────────────────────── USUARIOS ─────────────────────────
    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("solicitudesPendientes",
                usuarioRepository.findByEstadoSolicitudTutor("PENDIENTE").size());
        return "admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/eliminar")
    public String eliminarUsuario(@PathVariable String id, Authentication auth) {
        String adminCorreo = auth.getName();
        usuarioRepository.findById(id).ifPresent(u -> {
            if ("ADMIN".equals(u.getRol()) || u.getCorreo().equals(adminCorreo)) return;
            tutoriaRepository.findByEstudianteId(id).forEach(tutoriaRepository::delete);
            tutoriaRepository.findByProfesorId(id).forEach(tutoriaRepository::delete);
            notificacionRepository.findByUsuarioIdOrderByFechaDesc(id)
                    .forEach(notificacionRepository::delete);
            usuarioRepository.deleteById(id);
        });
        return "redirect:/admin/usuarios?eliminado=true";
    }

    // ───────────────────────── SOLICITUDES TUTOR ─────────────────────────
    @GetMapping("/solicitudes-tutor")
    public String solicitudesTutor(Model model) {
        var pendientes = usuarioRepository.findByEstadoSolicitudTutor("PENDIENTE");
        model.addAttribute("solicitudes", pendientes);
        model.addAttribute("solicitudesPendientes", pendientes.size());
        return "admin/solicitudes-tutor";
    }

    @PostMapping("/solicitudes-tutor/{id}/aprobar")
public String aprobarSolicitud(@PathVariable String id) {
    usuarioRepository.findById(id).ifPresent(u -> {
        u.setRol("PROFESOR");
        u.setEstadoSolicitudTutor("APROBADA");
        u.setCuentaVerificada(true); // ← CORRECCIÓN: el tutor aprobado siempre puede operar
        usuarioRepository.save(u);
        notificacionService.crear(
                u.getId(),
                "¡Solicitud aprobada!",
                "Felicitaciones, ya eres tutor en Agenda Tutorías. Completa tu perfil.",
                Notificacion.Tipo.SOLICITUD_TUTOR_APROBADA);
    });
    return "redirect:/admin/solicitudes-tutor";
}

    @PostMapping("/solicitudes-tutor/{id}/rechazar")
    public String rechazarSolicitud(@PathVariable String id) {
        usuarioRepository.findById(id).ifPresent(u -> {
            u.setEstadoSolicitudTutor("RECHAZADA");
            usuarioRepository.save(u);
            notificacionService.crear(
                    u.getId(),
                    "Solicitud de tutor rechazada",
                    "Tu solicitud para ser tutor no fue aprobada en esta ocasión.",
                    Notificacion.Tipo.SOLICITUD_TUTOR_RECHAZADA);
        });
        return "redirect:/admin/solicitudes-tutor";
    }

    // ───────────────────────── RETIROS ─────────────────────────
    @GetMapping("/retiros")
    public String verRetiros(Model model) {
        model.addAttribute("pendientes",
                solicitudRetiroRepository.findByEstado(SolicitudRetiro.Estado.PENDIENTE));
        model.addAttribute("todos", solicitudRetiroRepository.findAll());
        model.addAttribute("solicitudesPendientes",
                usuarioRepository.findByEstadoSolicitudTutor("PENDIENTE").size());
        return "admin/retiros";
    }

    @PostMapping("/retiros/{id}/aprobar")
    public String aprobarRetiro(@PathVariable String id) {
        solicitudRetiroRepository.findById(id).ifPresent(r -> {
            r.setEstado(SolicitudRetiro.Estado.APROBADO);
            solicitudRetiroRepository.save(r);
            notificacionService.crear(
                    r.getTutorId(),
                    "Retiro aprobado",
                    "Tu solicitud de retiro por $" +
                            String.format("%,.0f", r.getMonto()) + " fue aprobada.",
                    Notificacion.Tipo.RETIRO_APROBADO);
        });
        return "redirect:/admin/retiros?ok=true";
    }

    @PostMapping("/retiros/{id}/rechazar")
    public String rechazarRetiro(@PathVariable String id) {
        solicitudRetiroRepository.findById(id).ifPresent(r -> {
            usuarioRepository.findById(r.getTutorId()).ifPresent(tutor -> {
                tutor.setBalance((tutor.getBalance() != null ? tutor.getBalance() : 0) + r.getMonto());
                usuarioRepository.save(tutor);
            });
            r.setEstado(SolicitudRetiro.Estado.RECHAZADO);
            solicitudRetiroRepository.save(r);
            notificacionService.crear(
                    r.getTutorId(),
                    "Retiro rechazado",
                    "Tu solicitud de retiro fue rechazada. El monto fue devuelto a tu balance.",
                    Notificacion.Tipo.RETIRO_RECHAZADO);
        });
        return "redirect:/admin/retiros?rechazado=true";
    }
}