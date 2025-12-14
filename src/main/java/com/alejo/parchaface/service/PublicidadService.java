package com.alejo.parchaface.service;

import com.alejo.parchaface.model.Publicidad;
import java.util.List;

public interface PublicidadService {
    Publicidad guardarPublicidad(Publicidad publicidad);
    Publicidad obtenerPublicidadPorId(int id);
    List<Publicidad> obtenerTodas();
    Publicidad actualizarPublicidad(Publicidad publicidad);
    void eliminarPublicidad(int id);
}
