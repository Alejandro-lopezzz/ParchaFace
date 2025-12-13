package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.model.Estadistica;
import com.alejo.parchaface.repository.EstadisticaRepository;
import com.alejo.parchaface.service.EstadisticaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EstadisticaServiceImpl implements EstadisticaService {

    @Autowired
    private EstadisticaRepository estadisticaRepository;

    @Override
    public Estadistica guardarEstadistica(Estadistica estadistica) {
        return estadisticaRepository.save(estadistica);
    }

    @Override
    public Estadistica obtenerEstadisticaPorId(int id) {
        Optional<Estadistica> optional = estadisticaRepository.findById(id);
        return optional.orElse(null);
    }

    @Override
    public List<Estadistica> obtenerTodas() {
        return estadisticaRepository.findAll();
    }

    @Override
    public Estadistica actualizarEstadistica(Estadistica estadistica) {
        return estadisticaRepository.save(estadistica);
    }

    @Override
    public void eliminarEstadistica(int id) {
        estadisticaRepository.deleteById(id);
    }
}
