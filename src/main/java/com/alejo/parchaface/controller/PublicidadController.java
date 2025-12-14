package com.alejo.parchaface.controller;

import com.alejo.parchaface.model.Publicidad;
import com.alejo.parchaface.service.PublicidadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/publicidad")
public class PublicidadController {

    @Autowired
    private PublicidadService publicidadService;

    @GetMapping
    public List<Publicidad> obtenerTodas() {
        return publicidadService.obtenerTodas();
    }

    @GetMapping("/{id}")
    public Publicidad obtenerPorId(@PathVariable int id) {
        return publicidadService.obtenerPublicidadPorId(id);
    }

    @PostMapping
    public Publicidad guardar(@RequestBody Publicidad publicidad) {
        return publicidadService.guardarPublicidad(publicidad);
    }

    @PutMapping("/{id}")
    public Publicidad actualizar(@PathVariable int id, @RequestBody Publicidad publicidad) {
        publicidad.setId_publicidad(id); // asegurar que el ID coincida
        return publicidadService.actualizarPublicidad(publicidad);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable int id) {
        publicidadService.eliminarPublicidad(id);
    }
}
