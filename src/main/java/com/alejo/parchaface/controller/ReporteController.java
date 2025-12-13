package com.alejo.parchaface.controller;

import com.alejo.parchaface.model.Reporte;
import com.alejo.parchaface.service.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @GetMapping
    public List<Reporte> obtenerTodos() {
        return reporteService.obtenerTodos(); // Método en español del Service
    }

    @GetMapping("/{id}")
    public Reporte obtenerReportePorId(@PathVariable int id) {
        return reporteService.obtenerReportePorId(id);
    }

    @PostMapping
    public Reporte crearReporte(@RequestBody Reporte reporte) {
        return reporteService.crearReporte(reporte);
    }

    @DeleteMapping("/{id}")
    public void eliminarReporte(@PathVariable int id) {
        reporteService.eliminarReporte(id);
    }
}
