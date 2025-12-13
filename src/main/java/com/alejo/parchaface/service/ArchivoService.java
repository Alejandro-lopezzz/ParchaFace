package com.alejo.parchaface.service;

import com.alejo.parchaface.model.Archivo;
import java.util.List;

public interface ArchivoService {
    Archivo guardarArchivo(Archivo archivo);
    Archivo obtenerArchivoPorId(int id);
    List<Archivo> obtenerTodos();
    Archivo actualizarArchivo(Archivo archivo);
    void eliminarArchivo(int id);
}
