package com.alejo.parchaface.controller;

import com.alejo.parchaface.model.Estadistica;
import com.alejo.parchaface.service.EstadisticaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/estadisticas")
public class EstadisticaController {

    @Autowired
    private EstadisticaService estadisticaService;

    @GetMapping
    public List<Estadistica> obtenerTodas() {
        return estadisticaService.obtenerTodas();
    }

    @GetMapping("/{id}")
    public Estadistica obtenerEstadisticaPorId(@PathVariable int id) {
        return estadisticaService.obtenerEstadisticaPorId(id);
    }

    @PostMapping
    public Estadistica guardarEstadistica(@RequestBody Estadistica estadistica) {
        return estadisticaService.guardarEstadistica(estadistica);
    }

    @PutMapping
    public Estadistica actualizarEstadistica(@RequestBody Estadistica estadistica) {
        return estadisticaService.actualizarEstadistica(estadistica);
    }

    @DeleteMapping("/{id}")
    public void eliminarEstadistica(@PathVariable int id) {
        estadisticaService.eliminarEstadistica(id);
    }
}
