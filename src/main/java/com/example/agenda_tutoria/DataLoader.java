package com.example.agenda_tutoria;

import com.example.agenda_tutoria.model.Usuario;
import com.example.agenda_tutoria.model.Calificacion;
import com.example.agenda_tutoria.repository.CalificacionRepository;
import com.example.agenda_tutoria.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CalificacionRepository calificacionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // Admin principal
        crearSiNoExiste("admin@tutorias.com", "Administrador", "admin123",
            "ADMIN", null, null, null, null, 0, true);

        // Tutores principales
        crearSiNoExiste("carlos.perez@tutorias.com", "Carlos Andrés Pérez",
            "tutor123", "PROFESOR", "Programación y Desarrollo Web",
            "Desarrollador con 6 años de experiencia en Java, Spring Boot y React. " +
            "He trabajado en proyectos empresariales y me apasiona enseñar.",
            Arrays.asList("Programación I", "Programación II", "Desarrollo Web", "Spring Boot"),
            45000.0, 6, true);

        crearSiNoExiste("maria.gonzalez@tutorias.com", "María Fernanda González",
            "tutor123", "PROFESOR", "Matemáticas y Cálculo",
            "Ingeniera matemática con maestría en educación. Especializada en cálculo " +
            "diferencial e integral, álgebra lineal y estadística.",
            Arrays.asList("Cálculo I", "Cálculo II", "Álgebra Lineal", "Estadística"),
            35000.0, 4, true);

        crearSiNoExiste("juan.martinez@tutorias.com", "Juan David Martínez",
            "tutor123", "PROFESOR", "Bases de Datos",
            "Experto en diseño y administración de bases de datos relacionales y NoSQL. " +
            "Certificado en MongoDB y experiencia con SQL Server, MySQL y PostgreSQL.",
            Arrays.asList("Bases de Datos I", "Bases de Datos II", "MongoDB", "SQL"),
            40000.0, 5, true);

        crearSiNoExiste("ana.rodriguez@tutorias.com", "Ana Sofía Rodríguez",
            "tutor123", "PROFESOR", "Inglés Técnico",
            "Profesora de inglés certificada por Cambridge con especialización en " +
            "inglés técnico para ingeniería y tecnología. Nivel C1.",
            Arrays.asList("Inglés I", "Inglés II", "Inglés Técnico", "Conversación"),
            30000.0, 7, true);

        crearSiNoExiste("david.torres@tutorias.com", "David Esteban Torres",
            "tutor123", "PROFESOR", "Redes y Sistemas",
            "Técnico en redes con certificación CCNA. Experiencia en configuración " +
            "de routers, switches, VLANs y protocolos de red.",
            Arrays.asList("Redes I", "Redes II", "Packet Tracer", "Seguridad en Redes"),
            38000.0, 3, true);

        crearSiNoExiste("laura.jimenez@tutorias.com", "Laura Valentina Jiménez",
            "tutor123", "PROFESOR", "Diseño de Software",
            "Ingeniera de software con experiencia en metodologías ágiles, " +
            "UML, patrones de diseño y arquitectura de software.",
            Arrays.asList("Ingeniería de Software", "UML", "Patrones de Diseño", "Scrum"),
            42000.0, 5, true);

        crearSiNoExiste("miguel.vargas@tutorias.com", "Miguel Ángel Vargas",
            "tutor123", "PROFESOR", "Física y Electrónica",
            "Ingeniero electrónico con especialización en física aplicada. " +
            "Amplia experiencia enseñando circuitos, electromagnetismo y mecánica.",
            Arrays.asList("Física I", "Física II", "Electrónica Básica", "Circuitos"),
            36000.0, 8, true);

        crearSiNoExiste("camila.herrera@tutorias.com", "Camila Andrea Herrera",
            "tutor123", "PROFESOR", "Contabilidad y Finanzas",
            "Contadora pública con experiencia en contabilidad empresarial, " +
            "finanzas personales y análisis financiero.",
            Arrays.asList("Contabilidad I", "Contabilidad II", "Finanzas", "Excel Avanzado"),
            32000.0, 4, true);

        // Estudiante de prueba principal
        crearSiNoExiste("estudiante@tutorias.com", "María García",
            "est123", "ESTUDIANTE", null, null, null, null, 0, true);

        sembrarCalificaciones();

        // --- GENERADOR MASIVO DE 500 USUARIOS ---
        if (usuarioRepository.count() < 500) {
            System.out.println("⏳ Generando usuarios masivos en la base de datos (esto puede tardar unos segundos)...");

            // 1. Generar 450 Estudiantes
            for (int i = 1; i <= 450; i++) {
                String correo = "estudiante" + i + "@tutorias.com";
                if (usuarioRepository.findByCorreo(correo).isEmpty()) {
                    Usuario u = new Usuario();
                    u.setNombre("Estudiante Gen " + i);
                    u.setCorreo(correo);
                    u.setPassword(passwordEncoder.encode("est123")); // Contraseña por defecto para los masivos: est123
                    u.setRol("ESTUDIANTE");
                    u.setCuentaVerificada(true);
                    u.setBalance(50000.0);
                    u.setCalificacionPromedio(4.0);
                    u.setTotalCalificaciones(1);
                    usuarioRepository.save(u);
                }
            }

            // 2. Generar 50 Profesores
            String[] materiasDisponibles = {"Matemáticas", "Programación", "Física", "Inglés", "Bases de Datos", "Química"};
            for (int j = 1; j <= 50; j++) {
                String correo = "profesor" + j + "@tutorias.com";
                if (usuarioRepository.findByCorreo(correo).isEmpty()) {
                    Usuario u = new Usuario();
                    u.setNombre("Profesor Tutor " + j);
                    u.setCorreo(correo);
                    u.setPassword(passwordEncoder.encode("tutor123")); // Contraseña por defecto para los profesores: tutor123
                    u.setRol("PROFESOR");
                    u.setEspecialidad(materiasDisponibles[j % materiasDisponibles.length]);
                    u.setDescripcion("Profesor experto con años de experiencia en el área.");
                    u.setPrecioPorHora(35000.0 + (j * 500));
                    u.setAniosExperiencia(3 + (j % 5));
                    u.setCuentaVerificada(true);
                    u.setBalance(0.0);
                    u.setCalificacionPromedio(4.5);
                    u.setTotalCalificaciones(10);
                    usuarioRepository.save(u);
                }
            }
            System.out.println("✅ ¡Generación masiva completada con éxito!");
        }

        System.out.println("✅ DataLoader completado — " +
            usuarioRepository.count() + " usuarios en total");
    }

    private void sembrarCalificaciones() {
        if (calificacionRepository.count() > 0) return;

        String[] comentarios = {
            "Explica muy claro y tiene paciencia.",
            "Muy buena tutoría, resolvió todas mis dudas.",
            "Dominó el tema y dio ejemplos prácticos.",
            "Puntual y con excelente metodología.",
            "Me ayudó a entender los ejercicios difíciles."
        };

        int indice = 0;
        for (Usuario profesor : usuarioRepository.findByRol("PROFESOR")) {
            int[] notas = {5, 4, 5};
            for (int i = 0; i < notas.length; i++) {
                Calificacion c = new Calificacion();
                c.setTutoriaId("demo-" + profesor.getId() + "-" + i);
                c.setEstudianteId("demo-estudiante");
                c.setEstudianteNombre("Estudiante de prueba");
                c.setProfesorId(profesor.getId());
                c.setPuntuacion(notas[i]);
                c.setComentario(comentarios[indice % comentarios.length]);
                calificacionRepository.save(c);
                indice++;
            }

            double promedio = Arrays.stream(notas).average().orElse(0);
            profesor.setCalificacionPromedio(Math.round(promedio * 10.0) / 10.0);
            profesor.setTotalCalificaciones(notas.length);
            usuarioRepository.save(profesor);
        }
    }

    private void crearSiNoExiste(String correo, String nombre, String password,
                                  String rol, String especialidad, String descripcion,
                                  List<String> materias, Double precio,
                                  int experiencia, boolean verificado) {
        if (usuarioRepository.findByCorreo(correo).isPresent()) return;

        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setPassword(passwordEncoder.encode(password));
        u.setRol(rol);
        u.setCuentaVerificada(verificado);
        u.setBalance("PROFESOR".equals(rol) ? 0.0 : 50000.0);
        u.setCalificacionPromedio(3.5 + Math.random() * 1.5);
        u.setTotalCalificaciones((int)(Math.random() * 50) + 5);

        if (especialidad != null) u.setEspecialidad(especialidad);
        if (descripcion != null)  u.setDescripcion(descripcion);
        if (materias != null)     u.setMaterias(materias);
        if (precio != null)       u.setPrecioPorHora(precio);
        if (experiencia > 0)      u.setAniosExperiencia(experiencia);

        usuarioRepository.save(u);
        System.out.println("✅ Creado: " + nombre + " (" + rol + ")");
    }
}