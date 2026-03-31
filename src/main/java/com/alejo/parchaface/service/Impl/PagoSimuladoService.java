package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.PagoEvento;
import com.alejo.parchaface.model.Usuario;
import com.alejo.parchaface.model.enums.EstadoInscripcion;
import com.alejo.parchaface.model.enums.EstadoPagoEvento;
import com.alejo.parchaface.model.enums.MetodoPagoSimulado;
import com.alejo.parchaface.repository.EventoRepository;
import com.alejo.parchaface.repository.InscripcionRepository;
import com.alejo.parchaface.repository.PagoEventoRepository;
import com.alejo.parchaface.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PagoSimuladoService {

    @Autowired
    private PagoEventoRepository pagoEventoRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private InscripcionServiceImpl inscripcionService;

    @Transactional
    public PagoEvento crearPago(Integer idEvento, String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));

        if (Boolean.TRUE.equals(evento.getEventoGratuito())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este evento es gratuito");
        }

        if (evento.getPrecio() == null || evento.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El evento no tiene un precio válido");
        }

        if (evento.getOrganizador() != null
                && evento.getOrganizador().getIdUsuario() != null
                && evento.getOrganizador().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El organizador no puede pagar su propio evento");
        }

        boolean yaInscrito = inscripcionRepository.existsByEvento_IdEventoAndUsuario_IdUsuarioAndEstadoInscripcion(
                idEvento,
                usuario.getIdUsuario(),
                EstadoInscripcion.vigente
        );

        if (yaInscrito) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya estás inscrito en este evento");
        }

        Integer cupo = evento.getCupo();
        long inscritos = inscripcionRepository.countByEvento_IdEventoAndEstadoInscripcion(
                idEvento,
                EstadoInscripcion.vigente
        );

        if (cupo != null && cupo > 0 && inscritos >= cupo) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El evento ya no tiene cupo");
        }

        PagoEvento pago = new PagoEvento();
        pago.setEvento(evento);
        pago.setUsuario(usuario);
        pago.setMonto(evento.getPrecio());
        pago.setEstado(EstadoPagoEvento.PENDIENTE);
        pago.setReferencia(generarReferencia(idEvento, usuario.getIdUsuario()));

        return pagoEventoRepository.save(pago);
    }

    @Transactional(readOnly = true)
    public PagoEvento obtenerPago(Integer idPago, String correo) {
        PagoEvento pago = pagoEventoRepository.findById(idPago)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado"));

        validarPropietario(pago, correo);
        return pago;
    }

    @Transactional
    public PagoEvento simularPago(
            Integer idPago,
            String correo,
            MetodoPagoSimulado metodoPago,
            EstadoPagoEvento resultado
    ) {
        PagoEvento pago = pagoEventoRepository.findById(idPago)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado"));

        validarPropietario(pago, correo);

        if (metodoPago == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Método de pago inválido");
        }

        if (resultado == null || resultado == EstadoPagoEvento.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resultado de pago inválido");
        }

        if (pago.getEstado() != EstadoPagoEvento.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pago ya fue procesado");
        }

        pago.setMetodoPago(metodoPago);
        pago.setEstado(resultado);
        pago = pagoEventoRepository.save(pago);

        if (resultado == EstadoPagoEvento.PAGADO) {
            inscripcionService.confirmarInscripcionPagada(
                    pago.getEvento().getIdEvento(),
                    correo
            );
        }

        return pago;
    }

    private void validarPropietario(PagoEvento pago, String correo) {
        if (pago.getUsuario() == null || pago.getUsuario().getCorreo() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este pago");
        }

        if (!pago.getUsuario().getCorreo().equalsIgnoreCase(correo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este pago");
        }
    }

    private String generarReferencia(Integer idEvento, Integer idUsuario) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "PF-" + idEvento + "-" + idUsuario + "-" + random;
    }
}