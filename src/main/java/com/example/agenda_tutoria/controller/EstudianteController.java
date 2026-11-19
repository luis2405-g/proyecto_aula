package com.example.agenda_tutoria.controller;

import com.example.agenda_tutoria.model.Calificacion;
import com.example.agenda_tutoria.model.Notificacion;
import com.example.agenda_tutoria.model.Tutoria;
import com.example.agenda_tutoria.model.Usuario;
import com.example.agenda_tutoria.model.UsuarioPublicoDto;
import com.example.agenda_tutoria.service.NotificacionService;
import com.example.agenda_tutoria.service.PagoService;
import com.example.agenda_tutoria.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/estudiante")
public class EstudianteController {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TutoriaRepository tutoriaRepository;
    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private NotificacionService notificacionService;
    @Autowired private TransaccionRepository transaccionRepository;
    @Autowired private CalificacionRepository calificacionRepository;
    @Autowired private PagoService pagoService;
    @Autowired private PasswordEncoder passwordEncoder;

    // ───────────────────────── DASHBOARD ─────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        List<Tutoria> misTutorias = tutoriaRepository.findByEstudianteId(usuario.getId());

        List<Tutoria> proximas = misTutorias.stream()
                .filter(t -> t.getEstado() == Tutoria.Estado.ACEPTADA
                        || t.getEstado() == Tutoria.Estado.PENDIENTE)
                .sorted(Comparator.comparing(Tutoria::getFechaHora,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(3)
                .collect(Collectors.toList());

        long aceptadas = misTutorias.stream()
                .filter(t -> t.getEstado() == Tutoria.Estado.ACEPTADA).count();
        long pendientes = misTutorias.stream()
                .filter(t -> t.getEstado() == Tutoria.Estado.PENDIENTE).count();

        // Excluir al usuario logueado de tutoresDestacados
        List<Usuario> tutoresDestacados = usuarioRepository.findByRol("PROFESOR")
                .stream()
                .filter(t -> !t.getId().equals(usuario.getId()))
                .limit(3)
                .collect(Collectors.toList());

        long noLeidas = notificacionRepository.countByUsuarioIdAndLeidaFalse(usuario.getId());

        model.addAttribute("usuario", usuario);
        model.addAttribute("misTutorias", misTutorias);
        model.addAttribute("proximasTutorias", proximas);
        model.addAttribute("tutoriasAceptadas", aceptadas);
        model.addAttribute("tutoriasPendientes", pendientes);
        model.addAttribute("tutoresDestacados", tutoresDestacados);
        model.addAttribute("noLeidas", noLeidas);
        return "estudiante/dashboard";
    }

    // ───────────────────────── TUTORES ─────────────────────────
    @GetMapping("/tutores")
    public String verTutores(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) Double precioMax,
            Authentication auth, Model model) {

        Usuario usuario = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();

        // Excluir al usuario logueado de la lista de tutores
        List<Usuario> tutores = usuarioRepository.findByRol("PROFESOR")
                .stream()
                .filter(t -> !t.getId().equals(usuario.getId()))
                .collect(Collectors.toList());

        if (q != null && !q.isBlank()) {
            String busq = q.toLowerCase();
            tutores = tutores.stream()
                    .filter(t -> (t.getNombre() != null && t.getNombre().toLowerCase().contains(busq))
                            || (t.getEspecialidad() != null && t.getEspecialidad().toLowerCase().contains(busq)))
                    .collect(Collectors.toList());
        }
        if (especialidad != null && !especialidad.isBlank()) {
            tutores = tutores.stream()
                    .filter(t -> especialidad.equalsIgnoreCase(t.getEspecialidad()))
                    .collect(Collectors.toList());
        }
        if (precioMax != null && precioMax > 0) {
            tutores = tutores.stream()
                    .filter(t -> t.getPrecioPorHora() == null || t.getPrecioPorHora() <= precioMax)
                    .collect(Collectors.toList());
        }

        // Dropdown de especialidades también excluye al propio usuario
        List<String> especialidades = usuarioRepository.findByRol("PROFESOR").stream()
                .filter(t -> !t.getId().equals(usuario.getId()))
                .map(Usuario::getEspecialidad)
                .filter(e -> e != null && !e.isBlank())
                .distinct().sorted().collect(Collectors.toList());

        model.addAttribute("usuario", usuario);
        model.addAttribute("noLeidas",
                notificacionRepository.countByUsuarioIdAndLeidaFalse(usuario.getId()));
        model.addAttribute("tutores", tutores);
        model.addAttribute("especialidades", especialidades);
        model.addAttribute("q", q);
        model.addAttribute("especialidadSeleccionada", especialidad);
        model.addAttribute("precioMax", precioMax);
        return "estudiante/tutores";
    }

    // ───────────────────────── PERFIL TUTOR (con DTO) ─────────────────────────
    @GetMapping("/tutor/{id}")
    public String verPerfilTutor(@PathVariable String id,
            Authentication auth, Model model) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        Usuario tutor = usuarioRepository.findById(id).orElseThrow();

        UsuarioPublicoDto tutorDto = UsuarioPublicoDto.fromUsuario(tutor);

        List<UsuarioPublicoDto> relacionados = usuarioRepository.findByRol("PROFESOR").stream()
                .filter(t -> !t.getId().equals(id))
                .filter(t -> t.getEspecialidad() != null
                        && tutor.getEspecialidad() != null
                        && t.getEspecialidad().equalsIgnoreCase(tutor.getEspecialidad()))
                .limit(3)
                .map(UsuarioPublicoDto::fromUsuario)
                .collect(Collectors.toList());

        if (relacionados.isEmpty()) {
            relacionados = usuarioRepository.findByRol("PROFESOR").stream()
                    .filter(t -> !t.getId().equals(id))
                    .limit(3)
                    .map(UsuarioPublicoDto::fromUsuario)
                    .collect(Collectors.toList());
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("noLeidas",
                notificacionRepository.countByUsuarioIdAndLeidaFalse(usuario.getId()));
        model.addAttribute("tutor", tutorDto);
        model.addAttribute("relacionados", relacionados);
        model.addAttribute("calificaciones", calificacionRepository
                .findByProfesorIdOrderByFechaDesc(tutor.getId())
                .stream().limit(5).collect(Collectors.toList()));
        return "estudiante/tutor-perfil";
    }

    // ───────────────────────── AGENDAR ─────────────────────────
    @GetMapping("/agendar/{profesorId}")
    public String mostrarAgendar(@PathVariable String profesorId,
            Authentication auth, Model model) {
        Usuario estudiante = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();

        // Solo bloquear si es ESTUDIANTE sin verificar; un PROFESOR siempre puede operar
        boolean esProfesor = "PROFESOR".equals(estudiante.getRol());
        if (!esProfesor && Boolean.FALSE.equals(estudiante.getCuentaVerificada())) {
            return "redirect:/estudiante/dashboard?sinVerificar=true";
        }

        // Un profesor no puede agendarse a sí mismo
        if (estudiante.getId().equals(profesorId)) {
            return "redirect:/estudiante/tutores?error=propio";
        }

        Usuario profesor = usuarioRepository.findById(profesorId).orElseThrow();
        model.addAttribute("usuario", estudiante);
        model.addAttribute("noLeidas",
                notificacionRepository.countByUsuarioIdAndLeidaFalse(estudiante.getId()));
        model.addAttribute("profesor", profesor);
        model.addAttribute("tutoria", new Tutoria());
        model.addAttribute("balanceEstudiante", estudiante.getBalance());
        return "estudiante/agendar";
    }

    @PostMapping("/agendar/{profesorId}")
    public String agendar(@PathVariable String profesorId,
            @ModelAttribute Tutoria tutoria,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        Usuario estudiante = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        Usuario profesor = usuarioRepository.findById(profesorId).orElseThrow();

        // Solo bloquear si es ESTUDIANTE sin verificar; un PROFESOR siempre puede operar
        boolean esProfesor = "PROFESOR".equals(estudiante.getRol());
        if (!esProfesor && Boolean.FALSE.equals(estudiante.getCuentaVerificada())) {
            return "redirect:/estudiante/dashboard?sinVerificar=true";
        }

        // Un profesor no puede agendarse a sí mismo
        if (estudiante.getId().equals(profesorId)) {
            return "redirect:/estudiante/tutores?error=propio";
        }

        double costo = pagoService.calcularCosto(
                profesor.getPrecioPorHora(), tutoria.getDuracionMin());

        if (!pagoService.tieneSaldo(estudiante, costo)) {
            return "redirect:/estudiante/agendar/" + profesorId + "?saldoInsuficiente=true";
        }

        tutoria.setEstudianteId(estudiante.getId());
        tutoria.setEstudianteNombre(estudiante.getNombre());
        tutoria.setProfesorId(profesorId);
        tutoria.setProfesorNombre(profesor.getNombre());
        tutoria.setEstado(Tutoria.Estado.PENDIENTE);
        tutoria.setPrecioTotal(costo);
        tutoria.setCalificada(false);
        tutoriaRepository.save(tutoria);

        boolean pagoProcesado = pagoService.procesarPago(
                estudiante, profesor, costo, tutoria.getId());
        tutoria.setPagado(pagoProcesado);
        tutoriaRepository.save(tutoria);

        redirectAttributes.addFlashAttribute("montoPagado", costo);
        redirectAttributes.addFlashAttribute("saldoRestante", estudiante.getBalance());

        notificacionService.crear(
                profesor.getId(),
                "Nueva solicitud de tutoría",
                estudiante.getNombre() + " quiere una tutoría de "
                        + tutoria.getMateria() + ". Pago procesado: $"
                        + String.format("%,.0f", costo),
                Notificacion.Tipo.TUTORIA_AGENDADA);

        return "redirect:/estudiante/dashboard?agendado=true";
    }

    // ───────────────────────── BILLETERA ─────────────────────────
    @GetMapping("/billetera")
    public String mostrarBilletera(Authentication auth, Model model) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        model.addAttribute("usuario", usuario);
        model.addAttribute("noLeidas",
                notificacionRepository.countByUsuarioIdAndLeidaFalse(usuario.getId()));
        model.addAttribute("transacciones",
                transaccionRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId()));
        return "estudiante/billetera";
    }

    @PostMapping("/billetera/recargar")
    public String recargar(@RequestParam Double monto, Authentication auth) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();

        if (monto <= 0 || monto > 5000000) {
            return "redirect:/estudiante/billetera?error=monto";
        }

        pagoService.recargar(usuario, monto);
        return "redirect:/estudiante/billetera?recargado=true";
    }

    // ───────────────────────── SOLICITAR TUTOR ─────────────────────────
    @GetMapping("/solicitar-tutor")
    public String mostrarSolicitud(Authentication auth, Model model) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        model.addAttribute("usuario", usuario);
        model.addAttribute("noLeidas",
                notificacionRepository.countByUsuarioIdAndLeidaFalse(usuario.getId()));
        return "estudiante/solicitar-tutor";
    }

    @PostMapping("/solicitar-tutor")
    public String solicitarTutor(@RequestParam String motivacion, Authentication auth) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        usuario.setMotivacionTutor(motivacion);
        usuario.setEstadoSolicitudTutor("PENDIENTE");
        usuarioRepository.save(usuario);
        return "redirect:/estudiante/dashboard?solicitud=true";
    }

    // ───────────────────────── CALIFICAR TUTOR ─────────────────────────
    @GetMapping("/tutorias/{id}/calificar")
    public String mostrarCalificar(@PathVariable String id,
            Authentication auth, Model model, RedirectAttributes redirectAttributes) {
        Usuario estudiante = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        Tutoria tutoria = tutoriaRepository.findById(id).orElse(null);

        if (tutoria == null || !estudiante.getId().equals(tutoria.getEstudianteId())) {
            redirectAttributes.addFlashAttribute("errorCalificacion",
                    "No puedes calificar esta tutoría.");
            return "redirect:/estudiante/dashboard";
        }

        if (tutoria.getEstado() != Tutoria.Estado.FINALIZADA
                || Boolean.TRUE.equals(tutoria.getCalificada())) {
            redirectAttributes.addFlashAttribute("errorCalificacion",
                    "Solo puedes calificar tutorías finalizadas y una sola vez.");
            return "redirect:/estudiante/dashboard";
        }

        Usuario profesor = usuarioRepository.findById(tutoria.getProfesorId()).orElse(null);
        model.addAttribute("usuario", estudiante);
        model.addAttribute("tutoria", tutoria);
        model.addAttribute("profesor", profesor);
        model.addAttribute("noLeidas",
                notificacionRepository.countByUsuarioIdAndLeidaFalse(estudiante.getId()));
        return "estudiante/calificar";
    }

    @PostMapping("/tutorias/{id}/calificar")
    public String calificar(@PathVariable String id,
            @RequestParam Integer puntuacion,
            @RequestParam(required = false) String comentario,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        Usuario estudiante = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        Tutoria tutoria = tutoriaRepository.findById(id).orElse(null);

        if (tutoria == null || !estudiante.getId().equals(tutoria.getEstudianteId())
                || tutoria.getEstado() != Tutoria.Estado.FINALIZADA
                || Boolean.TRUE.equals(tutoria.getCalificada())
                || calificacionRepository.existsByTutoriaId(id)) {
            redirectAttributes.addFlashAttribute("errorCalificacion",
                    "No puedes calificar esta tutoría.");
            return "redirect:/estudiante/dashboard";
        }

        if (puntuacion == null || puntuacion < 1 || puntuacion > 5) {
            redirectAttributes.addFlashAttribute("errorCalificacion",
                    "La calificación debe estar entre 1 y 5 estrellas.");
            return "redirect:/estudiante/tutorias/" + id + "/calificar";
        }

        Usuario profesor = usuarioRepository.findById(tutoria.getProfesorId()).orElse(null);
        if (profesor == null) {
            redirectAttributes.addFlashAttribute("errorCalificacion",
                    "No se encontró el tutor de esta tutoría.");
            return "redirect:/estudiante/dashboard";
        }

        Calificacion calificacion = new Calificacion();
        calificacion.setTutoriaId(tutoria.getId());
        calificacion.setEstudianteId(estudiante.getId());
        calificacion.setEstudianteNombre(estudiante.getNombre());
        calificacion.setProfesorId(profesor.getId());
        calificacion.setPuntuacion(puntuacion);
        calificacion.setComentario(comentario == null ? "" : comentario.trim());
        calificacionRepository.save(calificacion);

        int total = profesor.getTotalCalificaciones() != null
                ? profesor.getTotalCalificaciones() : 0;
        double promedio = profesor.getCalificacionPromedio() != null
                ? profesor.getCalificacionPromedio() : 0.0;
        double nuevoPromedio = (promedio * total + puntuacion) / (total + 1.0);
        profesor.setCalificacionPromedio(Math.round(nuevoPromedio * 10.0) / 10.0);
        profesor.setTotalCalificaciones(total + 1);
        usuarioRepository.save(profesor);

        tutoria.setCalificada(true);
        tutoriaRepository.save(tutoria);

        String detalle = estudiante.getNombre() + " te calificó con "
                + puntuacion + "/5 estrellas.";
        if (!calificacion.getComentario().isBlank()) {
            detalle += " Comentario: " + calificacion.getComentario();
        }
        notificacionService.crear(
                profesor.getId(),
                "Nueva calificación recibida",
                detalle,
                Notificacion.Tipo.CALIFICACION);

        redirectAttributes.addFlashAttribute("calificacionExitosa",
                "¡Gracias! Tu calificación de " + puntuacion + "/5 fue registrada.");
        return "redirect:/estudiante/dashboard";
    }

    // ───────────────────────── ELIMINAR TUTORÍA ─────────────────────────
    @PostMapping("/tutorias/{id}/eliminar")
    public String eliminarTutoria(@PathVariable String id, Authentication auth) {
        Usuario estudiante = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();

        tutoriaRepository.findById(id).ifPresent(t -> {
            if (t.getEstudianteId().equals(estudiante.getId())
                    && t.getEstado() == Tutoria.Estado.FINALIZADA) {
                tutoriaRepository.delete(t);
            }
        });

        return "redirect:/estudiante/dashboard?eliminada=true";
    }

    // ───────────────────────── PERFIL ─────────────────────────
    @GetMapping("/perfil")
    public String verPerfil(Authentication auth, Model model) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        model.addAttribute("usuario", usuario);
        model.addAttribute("noLeidas",
                notificacionRepository.countByUsuarioIdAndLeidaFalse(usuario.getId()));
        return "estudiante/perfil";
    }

    @PostMapping("/perfil")
    public String actualizarPerfil(@RequestParam String nombre,
            @RequestParam(required = false) String passwordActual,
            @RequestParam(required = false) String passwordNueva,
            Authentication auth, Model model) {
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName()).orElseThrow();
        usuario.setNombre(nombre);

        if (passwordActual != null && !passwordActual.isBlank()
                && passwordNueva != null && !passwordNueva.isBlank()) {
            if (passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                usuario.setPassword(passwordEncoder.encode(passwordNueva));
            } else {
                model.addAttribute("error", "La contraseña actual no es correcta.");
                model.addAttribute("usuario", usuario);
                model.addAttribute("noLeidas",
                        notificacionRepository.countByUsuarioIdAndLeidaFalse(usuario.getId()));
                return "estudiante/perfil";
            }
        }

        usuarioRepository.save(usuario);
        return "redirect:/estudiante/perfil?ok=true";
    }
}