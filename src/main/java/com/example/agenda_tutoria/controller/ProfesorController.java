package com.example.agenda_tutoria.controller;

import com.example.agenda_tutoria.model.Notificacion;
import com.example.agenda_tutoria.model.ModeloRegresion;
import com.example.agenda_tutoria.model.PrediccionPrecio;
import com.example.agenda_tutoria.model.SolicitudRetiro;
import com.example.agenda_tutoria.model.Transaccion;
import com.example.agenda_tutoria.model.Tutoria;
import com.example.agenda_tutoria.model.Usuario;
import com.example.agenda_tutoria.repository.NotificacionRepository;
import com.example.agenda_tutoria.repository.SolicitudRetiroRepository;
import com.example.agenda_tutoria.repository.TransaccionRepository;
import com.example.agenda_tutoria.repository.TutoriaRepository;
import com.example.agenda_tutoria.repository.UsuarioRepository;
import com.example.agenda_tutoria.service.NotificacionService;
import com.example.agenda_tutoria.service.RegresionLinealService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profesor")
public class ProfesorController {

    @Autowired private TutoriaRepository tutoriaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private NotificacionService notificacionService;
    @Autowired private SolicitudRetiroRepository solicitudRetiroRepository;
    @Autowired private TransaccionRepository transaccionRepository;
    @Autowired private RegresionLinealService regresionLinealService;

    // ───────────────────────── DASHBOARD ─────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        Usuario profesor = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        List<Tutoria> tutorias = tutoriaRepository.findByProfesorId(profesor.getId());

        long pendientes = tutorias.stream()
                .filter(t -> t.getEstado() == Tutoria.Estado.PENDIENTE).count();
        long aceptadas = tutorias.stream()
                .filter(t -> t.getEstado() == Tutoria.Estado.ACEPTADA).count();
        long noLeidas = notificacionRepository
                .countByUsuarioIdAndLeidaFalse(profesor.getId());

        model.addAttribute("profesor", profesor);
        model.addAttribute("tutorias", tutorias);
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("aceptadas", aceptadas);
        model.addAttribute("noLeidas", noLeidas);
        return "profesor/dashboard";
    }

    // ───────────────────────── NOTIFICAR ─────────────────────────
    @PostMapping("/tutorias/{id}/notificar")
    public String notificar(@PathVariable String id,
            @RequestParam String mensaje,
            Authentication auth) {
        Usuario profesor = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();

        tutoriaRepository.findById(id).ifPresent(t -> {
            if (!t.getProfesorId().equals(profesor.getId())) return;
            notificacionService.crear(
                    t.getEstudianteId(),
                    "Mensaje de tu tutor",
                    profesor.getNombre() + ": " + mensaje,
                    Notificacion.Tipo.TUTORIA_AGENDADA);
        });

        return "redirect:/profesor/tutorias/" + id + "/gestionar?msg=enviado";
    }

    // ───────────────────────── GESTIONAR TUTORÍA ─────────────────────────
    @GetMapping("/tutorias/{id}/gestionar")
    public String gestionarTutoria(@PathVariable String id,
            Model model, Authentication auth) {
        Usuario profesor = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        Tutoria t = tutoriaRepository.findById(id).orElse(null);

        if (t == null || !t.getProfesorId().equals(profesor.getId())) {
            return "redirect:/profesor/dashboard";
        }

        model.addAttribute("profesor", profesor);
        model.addAttribute("noLeidas",
                notificacionRepository.countByUsuarioIdAndLeidaFalse(profesor.getId()));
        model.addAttribute("tutoria", t);
        return "profesor/gestionar-tutoria";
    }

    // ───────────────────────── ACEPTAR ─────────────────────────
    @PostMapping("/tutorias/{id}/aceptar")
    public String aceptar(@PathVariable String id,
            @RequestParam String mensaje,
            Authentication auth) {
        Usuario profesor = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();

        tutoriaRepository.findById(id).ifPresent(t -> {
            if (!t.getProfesorId().equals(profesor.getId())) return;

            t.setEstado(Tutoria.Estado.ACEPTADA);
            t.setMensajeTutor(mensaje);
            tutoriaRepository.save(t);

            notificacionService.crear(
                    t.getEstudianteId(),
                    "¡Tutoría aceptada!",
                    "Tu tutoría de " + t.getMateria() + " fue aceptada. Mensaje del tutor: " + mensaje,
                    Notificacion.Tipo.TUTORIA_ACEPTADA);
        });

        return "redirect:/profesor/dashboard?ok=aceptada";
    }

    // ───────────────────────── CANCELAR (con reembolso) ─────────────────────────
    @PostMapping("/tutorias/{id}/cancelar")
    public String cancelar(@PathVariable String id, Authentication auth) {
        Usuario profesor = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();

        tutoriaRepository.findById(id).ifPresent(t -> {
            if (!t.getProfesorId().equals(profesor.getId())) return;

            if (t.getPrecioTotal() != null && t.getPrecioTotal() > 0) {
                usuarioRepository.findById(t.getEstudianteId()).ifPresent(estudiante -> {
                    double balanceEst = estudiante.getBalance() != null ? estudiante.getBalance() : 0.0;
                    estudiante.setBalance(balanceEst + t.getPrecioTotal());
                    usuarioRepository.save(estudiante);

                    double gananciaRevertir = t.getPrecioTotal() * 0.90;
                    double balanceProf = profesor.getBalance() != null ? profesor.getBalance() : 0.0;
                    profesor.setBalance(Math.max(0, balanceProf - gananciaRevertir));
                    usuarioRepository.save(profesor);

                    Transaccion reembolso = new Transaccion(
                            estudiante.getId(), estudiante.getNombre(),
                            Transaccion.Tipo.RECARGA, t.getPrecioTotal(),
                            "Reembolso por cancelación de tutoría con " + profesor.getNombre());
                    reembolso.setReferenciaId(t.getId());
                    transaccionRepository.save(reembolso);
                });
            }

            t.setEstado(Tutoria.Estado.CANCELADA);
            tutoriaRepository.save(t);

            notificacionService.crear(
                    t.getEstudianteId(),
                    "Tutoría cancelada — reembolso procesado",
                    "El profesor canceló la tutoría. El dinero fue devuelto a tu billetera.",
                    Notificacion.Tipo.TUTORIA_AGENDADA);
        });

        return "redirect:/profesor/dashboard?ok=cancelada";
    }

    // ───────────────────────── RECHAZAR (con reembolso) ─────────────────────────
    @PostMapping("/tutorias/{id}/rechazar")
    public String rechazar(@PathVariable String id, Authentication auth) {
        Usuario profesor = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();

        tutoriaRepository.findById(id).ifPresent(t -> {
            if (!t.getProfesorId().equals(profesor.getId())) return;

            if (t.getPrecioTotal() != null && t.getPrecioTotal() > 0) {
                usuarioRepository.findById(t.getEstudianteId()).ifPresent(estudiante -> {
                    double balanceEst = estudiante.getBalance() != null ? estudiante.getBalance() : 0.0;
                    estudiante.setBalance(balanceEst + t.getPrecioTotal());
                    usuarioRepository.save(estudiante);

                    double gananciaRevertir = t.getPrecioTotal() * 0.90;
                    double balanceProf = profesor.getBalance() != null ? profesor.getBalance() : 0.0;
                    profesor.setBalance(Math.max(0, balanceProf - gananciaRevertir));
                    usuarioRepository.save(profesor);

                    Transaccion reembolso = new Transaccion(
                            estudiante.getId(), estudiante.getNombre(),
                            Transaccion.Tipo.RECARGA, t.getPrecioTotal(),
                            "Reembolso por rechazo de tutoría con " + profesor.getNombre());
                    reembolso.setReferenciaId(t.getId());
                    transaccionRepository.save(reembolso);
                });
            }

            t.setEstado(Tutoria.Estado.RECHAZADA);
            tutoriaRepository.save(t);

            notificacionService.crear(
                    t.getEstudianteId(),
                    "Tutoría rechazada — reembolso procesado",
                    "El tutor no pudo aceptar tu solicitud. El dinero fue devuelto a tu billetera.",
                    Notificacion.Tipo.TUTORIA_AGENDADA);
        });

        return "redirect:/profesor/dashboard?ok=rechazada";
    }

    // ───────────────────────── REPROGRAMAR ─────────────────────────
    @PostMapping("/tutorias/{id}/reprogramar")
    public String reprogramar(@PathVariable String id,
            @RequestParam String nuevaFecha,
            Authentication auth) {
        Usuario profesor = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();

        tutoriaRepository.findById(id).ifPresent(t -> {
            if (!t.getProfesorId().equals(profesor.getId())) return;

            t.setFechaHora(java.time.LocalDateTime.parse(nuevaFecha));
            t.setEstado(Tutoria.Estado.REPROGRAMADA);
            tutoriaRepository.save(t);

            notificacionService.crear(
                    t.getEstudianteId(),
                    "Tutoría reprogramada",
                    "El profesor cambió la fecha de la tutoría.",
                    Notificacion.Tipo.TUTORIA_AGENDADA);
        });

        return "redirect:/profesor/dashboard?ok=reprogramada";
    }

    // ───────────────────────── FINALIZAR ─────────────────────────
    @PostMapping("/tutorias/{id}/finalizar")
    public String finalizar(@PathVariable String id, Authentication auth) {
        Usuario profesor = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();

        tutoriaRepository.findById(id).ifPresent(t -> {
            if (!t.getProfesorId().equals(profesor.getId())) return;

            t.setEstado(Tutoria.Estado.FINALIZADA);
            tutoriaRepository.save(t);

            notificacionService.crear(
                    t.getEstudianteId(),
                    "Tutoría finalizada",
                    "Tu tutoría ha sido marcada como finalizada.",
                    Notificacion.Tipo.TUTORIA_AGENDADA);
        });

        return "redirect:/profesor/dashboard?ok=finalizada";
    }

    // ───────────────────────── ELIMINAR ─────────────────────────
    @PostMapping("/tutorias/{id}/eliminar")
    public String eliminar(@PathVariable String id, Authentication auth) {
        Usuario profesor = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();

        tutoriaRepository.findById(id).ifPresent(t -> {
            if (!t.getProfesorId().equals(profesor.getId())) return;
            tutoriaRepository.delete(t);
        });

        return "redirect:/profesor/dashboard?ok=eliminada";
    }

    // ───────────────────────── PERFIL ─────────────────────────
    @GetMapping("/perfil")
    public String verPerfil(Authentication auth, Model model) {
        Usuario profesor = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        ModeloRegresion modelo = regresionLinealService.entrenar();
        PrediccionPrecio prediccion = modelo.getPredicciones().stream()
                .filter(p -> profesor.getId() != null && profesor.getId().equals(p.getId()))
                .findFirst()
                .orElseGet(() -> new PrediccionPrecio(
                        profesor.getId(), profesor.getNombre(),
                        profesor.getAniosExperiencia(), profesor.getPrecioPorHora(),
                        profesor.getPrecioPorHora()));
        model.addAttribute("profesor", profesor);
        model.addAttribute("prediccion", prediccion);
        model.addAttribute("modelo", modelo);
        model.addAttribute("noLeidas",
                notificacionRepository.countByUsuarioIdAndLeidaFalse(profesor.getId()));
        return "profesor/perfil";
    }

    @PostMapping("/perfil")
    public String actualizarPerfil(@ModelAttribute Usuario datos, Authentication auth) {
        Usuario profesor = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        profesor.setDescripcion(datos.getDescripcion());
        profesor.setEspecialidad(datos.getEspecialidad());
        profesor.setPrecioPorHora(datos.getPrecioPorHora());
        profesor.setAniosExperiencia(datos.getAniosExperiencia());
        usuarioRepository.save(profesor);
        return "redirect:/profesor/perfil?ok=true";
    }

    // ───────────────────────── RETIROS ─────────────────────────
    @GetMapping("/retiros")
    public String verRetiros(Authentication auth, Model model) {
        Usuario profesor = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        model.addAttribute("profesor", profesor);
        model.addAttribute("noLeidas",
                notificacionRepository.countByUsuarioIdAndLeidaFalse(profesor.getId()));
        model.addAttribute("retiros",
                solicitudRetiroRepository.findByTutorIdOrderByFechaDesc(profesor.getId()));
        model.addAttribute("transacciones",
                transaccionRepository.findByUsuarioIdOrderByFechaDesc(profesor.getId()));
        return "profesor/retiros";
    }

    @PostMapping("/retiros/solicitar")
    public String solicitarRetiro(@RequestParam Double monto, Authentication auth) {
        Usuario profesor = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        double balance = profesor.getBalance() != null ? profesor.getBalance() : 0.0;

        if (monto <= 0 || monto > balance) {
            return "redirect:/profesor/retiros?error=saldo";
        }

        profesor.setBalance(balance - monto);
        usuarioRepository.save(profesor);

        SolicitudRetiro solicitud = new SolicitudRetiro();
        solicitud.setTutorId(profesor.getId());
        solicitud.setTutorNombre(profesor.getNombre());
        solicitud.setMonto(monto);
        solicitudRetiroRepository.save(solicitud);

        Transaccion tx = new Transaccion(
                profesor.getId(), profesor.getNombre(),
                Transaccion.Tipo.RETIRO, monto,
                "Solicitud de retiro de fondos");
        transaccionRepository.save(tx);

        return "redirect:/profesor/retiros?ok=true";
    }
}