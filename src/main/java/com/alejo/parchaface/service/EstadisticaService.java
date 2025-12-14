package com.alejo.parchaface.service;

import com.alejo.parchaface.model.Estadistica;
import java.util.List;

public interface EstadisticaService {
    Estadistica guardarEstadistica(Estadistica estadistica);
    Estadistica obtenerEstadisticaPorId(int id);
    List<Estadistica> obtenerTodas();
    Estadistica actualizarEstadistica(Estadistica estadistica);
    void eliminarEstadistica(int id);
}
