package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.model.Archivo;
import com.alejo.parchaface.repository.ArchivoRepository;
import com.alejo.parchaface.service.ArchivoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ArchivoServiceImpl implements ArchivoService {

    @Autowired
    private ArchivoRepository archivoRepository;

    @Override
    public Archivo guardarArchivo(Archivo archivo) {
        return archivoRepository.save(archivo);
    }

    @Override
    public Archivo obtenerArchivoPorId(int id) {
        Optional<Archivo> optional = archivoRepository.findById(id);
        return optional.orElse(null);
    }

    @Override
    public List<Archivo> obtenerTodos() {
        return archivoRepository.findAll();
    }

    @Override
    public List<Archivo> obtenerArchivosPorEvento(Integer idEvento) {
        return archivoRepository.findByEvento_IdEvento(idEvento);
    }

    @Override
    public Archivo actualizarArchivo(Archivo archivo) {
        return archivoRepository.save(archivo);
    }

    @Override
    public void eliminarArchivo(int id) {
        archivoRepository.deleteById(id);
    }
}
