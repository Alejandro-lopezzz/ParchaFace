package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.model.Publicidad;
import com.alejo.parchaface.repository.PublicidadRepository;
import com.alejo.parchaface.service.PublicidadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PublicidadServiceImpl implements PublicidadService {

    @Autowired
    private PublicidadRepository publicidadRepository;

    @Override
    public Publicidad guardarPublicidad(Publicidad publicidad) {
        return publicidadRepository.save(publicidad);
    }

    @Override
    public Publicidad obtenerPublicidadPorId(int id) {
        Optional<Publicidad> optional = publicidadRepository.findById(id);
        return optional.orElse(null);
    }

    @Override
    public List<Publicidad> obtenerTodas() {
        return publicidadRepository.findAll();
    }

    @Override
    public Publicidad actualizarPublicidad(Publicidad publicidad) {
        return publicidadRepository.save(publicidad);
    }

    @Override
    public void eliminarPublicidad(int id) {
        publicidadRepository.deleteById(id);
    }
}
