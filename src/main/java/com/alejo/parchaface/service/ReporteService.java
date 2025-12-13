package com.alejo.parchaface.service;

import com.alejo.parchaface.model.Reporte;
import java.util.List;

public interface ReporteService {
    Reporte guardarReporte(Reporte reporte);
    Reporte obtenerReportePorId(int id);
    Reporte crearReporte(Reporte reporte);
    List<Reporte> obtenerTodos();
    Reporte actualizarReporte(Reporte reporte);
    void eliminarReporte(int id);
}
