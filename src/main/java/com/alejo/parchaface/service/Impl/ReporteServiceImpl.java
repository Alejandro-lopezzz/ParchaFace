package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.model.Reporte;
import com.alejo.parchaface.repository.ReporteRepository;
import com.alejo.parchaface.service.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReporteServiceImpl implements ReporteService {

    @Autowired
    private ReporteRepository reporteRepository;

    @Override
    public Reporte guardarReporte(Reporte reporte) {
        return reporteRepository.save(reporte);
    }

    @Override
    public Reporte obtenerReportePorId(int id) {
        Optional<Reporte> optional = reporteRepository.findById(id);
        return optional.orElse(null);
    }

    @Override
    public List<Reporte> obtenerTodos() {
        return reporteRepository.findAll();
    }

    @Override
    public Reporte crearReporte(Reporte reporte) {
        return reporteRepository.save(reporte);}

    @Override
    public Reporte actualizarReporte(Reporte reporte) {
        return reporteRepository.save(reporte);
    }

    @Override
    public void eliminarReporte(int id) {
        reporteRepository.deleteById(id);
    }
}
