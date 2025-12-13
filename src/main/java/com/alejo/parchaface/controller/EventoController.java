package com.alejo.parchaface.controller;

import com.alejo.parchaface.model.Evento;
import com.alejo.parchaface.model.enums.EstadoEvento;
import com.alejo.parchaface.service.EventoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/eventos")
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @GetMapping
    public List<Evento> obtenerTodos() {
        return eventoService.getAllEventos();
    }

    @GetMapping("/{id}")
    public Evento obtenerPorId(@PathVariable Integer id) {
        return eventoService.getEventoById(id);
    }

    @PostMapping
    public Evento guardar(@RequestBody Evento evento) {
        return eventoService.saveEvento(evento);
    }

    @GetMapping("/estado/{estado}")
    public List<Evento> obtenerPorEstado(@PathVariable EstadoEvento estado) {
        return eventoService.getEventosPorEstado(estado);
    }


    @PutMapping("/{id}")
    public Evento actualizar(@PathVariable Integer id, @RequestBody Evento evento) {
        // Asegurar que el id del path se use para actualizar
        evento.setId_evento(id);
        return eventoService.saveEvento(evento);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        eventoService.deleteEvento(id);
    }
}
