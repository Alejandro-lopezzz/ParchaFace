package com.alejo.parchaface.controller;

import com.alejo.parchaface.model.Archivo;
import com.alejo.parchaface.service.ArchivoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/archivos")
public class ArchivoController {

    @Autowired
    private ArchivoService archivoService;

    @GetMapping
    public List<Archivo> obtenerTodos() {
        return archivoService.obtenerTodos();
    }

    @GetMapping("/{id}")
    public Archivo obtenerArchivoPorId(@PathVariable int id) {
        return archivoService.obtenerArchivoPorId(id);
    }

    @PostMapping
    public Archivo guardarArchivo(@RequestBody Archivo archivo) {
        return archivoService.guardarArchivo(archivo);
    }

    @PutMapping
    public Archivo actualizarArchivo(@RequestBody Archivo archivo) {
        return archivoService.actualizarArchivo(archivo);
    }

    @DeleteMapping("/{id}")
    public void eliminarArchivo(@PathVariable int id) {
        archivoService.eliminarArchivo(id);
    }
}
