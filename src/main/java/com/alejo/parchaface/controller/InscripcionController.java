package com.alejo.parchaface.controller;

import com.alejo.parchaface.model.Inscripcion;
import com.alejo.parchaface.service.InscripcionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inscripciones")
public class InscripcionController {

    @Autowired
    private InscripcionService inscripcionService;

    @GetMapping
    public List<Inscripcion> getAllInscripciones() {
        return inscripcionService.getAllInscripciones();
    }

    @GetMapping("/{id}")
    public Inscripcion getInscripcionById(@PathVariable Integer id) {
        return inscripcionService.getInscripcionById(id);
    }

    @PostMapping
    public Inscripcion createInscripcion(@RequestBody Inscripcion inscripcion) {
        return inscripcionService.createInscripcion(inscripcion);
    }

    @PutMapping("/{id}")
    public Inscripcion updateInscripcion(@PathVariable Integer id, @RequestBody Inscripcion inscripcion) {
        // Seteamos el id del path en el objeto antes de actualizar
        inscripcion.setIdInscripcion(id);
        return inscripcionService.updateInscripcion(inscripcion);
    }

    @DeleteMapping("/{id}")
    public void deleteInscripcion(@PathVariable Integer id) {
        inscripcionService.deleteInscripcion(id);
    }
}
