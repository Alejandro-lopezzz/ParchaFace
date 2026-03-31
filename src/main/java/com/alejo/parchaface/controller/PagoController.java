package com.alejo.parchaface.controller;

import com.alejo.parchaface.model.PagoEvento;
import com.alejo.parchaface.model.enums.EstadoPagoEvento;
import com.alejo.parchaface.model.enums.MetodoPagoSimulado;
import com.alejo.parchaface.service.Impl.PagoSimuladoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoSimuladoService pagoSimuladoService;

    public PagoController(PagoSimuladoService pagoSimuladoService) {
        this.pagoSimuladoService = pagoSimuladoService;
    }

    @PostMapping("/eventos/{idEvento}/crear")
    public ResponseEntity<Map<String, Object>> crearPago(
            @PathVariable Integer idEvento,
            Principal principal
    ) {
        PagoEvento pago = pagoSimuladoService.crearPago(idEvento, principal.getName());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Pago pendiente creado");
        body.put("idPago", pago.getIdPago());
        body.put("estado", pago.getEstado().name());
        body.put("monto", pago.getMonto());
        body.put("referencia", pago.getReferencia());
        body.put("eventoId", pago.getEvento().getIdEvento());

        return ResponseEntity.ok(body);
    }

    @GetMapping("/{idPago}")
    public ResponseEntity<Map<String, Object>> obtenerPago(
            @PathVariable Integer idPago,
            Principal principal
    ) {
        PagoEvento pago = pagoSimuladoService.obtenerPago(idPago, principal.getName());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idPago", pago.getIdPago());
        body.put("estado", pago.getEstado().name());
        body.put("monto", pago.getMonto());
        body.put("referencia", pago.getReferencia());
        body.put("metodoPago", pago.getMetodoPago() != null ? pago.getMetodoPago().name() : null);
        body.put("eventoId", pago.getEvento().getIdEvento());
        body.put("tituloEvento", pago.getEvento().getTitulo());

        return ResponseEntity.ok(body);
    }

    @PostMapping("/{idPago}/simular")
    public ResponseEntity<Map<String, Object>> simularPago(
            @PathVariable Integer idPago,
            @RequestBody Map<String, String> request,
            Principal principal
    ) {
        String metodoRaw = request.get("metodoPago");
        String resultadoRaw = request.get("resultado");

        MetodoPagoSimulado metodoPago;
        EstadoPagoEvento resultado;

        try {
            metodoPago = MetodoPagoSimulado.valueOf(String.valueOf(metodoRaw).trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Método de pago inválido");
        }

        try {
            resultado = EstadoPagoEvento.valueOf(String.valueOf(resultadoRaw).trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Resultado inválido");
        }

        PagoEvento pago = pagoSimuladoService.simularPago(
                idPago,
                principal.getName(),
                metodoPago,
                resultado
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Pago procesado");
        body.put("idPago", pago.getIdPago());
        body.put("estado", pago.getEstado().name());
        body.put("metodoPago", pago.getMetodoPago() != null ? pago.getMetodoPago().name() : null);
        body.put("eventoId", pago.getEvento().getIdEvento());

        return ResponseEntity.ok(body);
    }
}