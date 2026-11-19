package com.example.agenda_tutoria.service;

import com.example.agenda_tutoria.model.Transaccion;
import com.example.agenda_tutoria.model.Usuario;
import com.example.agenda_tutoria.repository.TransaccionRepository;
import com.example.agenda_tutoria.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PagoService {

    private static final double COMISION = 0.10;
    private static final String PLATAFORMA_ID = "000000000000000000000000";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransaccionRepository transaccionRepository;

    public void recargar(Usuario usuario, Double monto) {
        usuario.setBalance(usuario.getBalance() + monto);
        usuarioRepository.save(usuario);

        transaccionRepository.save(new Transaccion(
            usuario.getId(), usuario.getNombre(),
            Transaccion.Tipo.RECARGA, monto,
            "Recarga de billetera por $" + String.format("%,.0f", monto)
        ));
    }

    public boolean tieneSaldo(Usuario estudiante, Double monto) {
        return estudiante.getBalance() != null && estudiante.getBalance() >= monto;
    }

    public Double calcularCosto(Double precioPorHora, Integer duracionMin) {
        if (precioPorHora == null || duracionMin == null) return 0.0;
        return precioPorHora * duracionMin / 60.0;
    }

    public boolean procesarPago(Usuario estudiante, Usuario tutor,
                                 Double monto, String tutoriaId) {
        if (!tieneSaldo(estudiante, monto)) return false;

        double comision = monto * COMISION;
        double gananciaTutor = monto - comision;

        estudiante.setBalance(estudiante.getBalance() - monto);
        usuarioRepository.save(estudiante);

        double balanceTutor = tutor.getBalance() != null ? tutor.getBalance() : 0.0;
        tutor.setBalance(balanceTutor + gananciaTutor);
        usuarioRepository.save(tutor);

        Transaccion pagoEst = new Transaccion(
            estudiante.getId(), estudiante.getNombre(),
            Transaccion.Tipo.PAGO, monto,
            "Pago por tutoría con " + tutor.getNombre()
        );
        pagoEst.setReferenciaId(tutoriaId);
        transaccionRepository.save(pagoEst);

        Transaccion ingresoTutor = new Transaccion(
            tutor.getId(), tutor.getNombre(),
            Transaccion.Tipo.INGRESO, gananciaTutor,
            "Ingreso por tutoría con " + estudiante.getNombre()
                + " (comisión 10% descontada)"
        );
        ingresoTutor.setReferenciaId(tutoriaId);
        transaccionRepository.save(ingresoTutor);

        Transaccion comisionTx = new Transaccion(
            PLATAFORMA_ID, "Agenda Tutorías",
            Transaccion.Tipo.COMISION, comision,
            "Comisión 10% de tutoría entre "
                + estudiante.getNombre() + " y " + tutor.getNombre()
        );
        comisionTx.setReferenciaId(tutoriaId);
        transaccionRepository.save(comisionTx);

        return true;
    }
}