package ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.AsignarCamionRequest;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.CrearRutaRequest;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Ruta;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Tramo;

public interface RutaService {

    Ruta crearRutaTentativa(CrearRutaRequest request);

    Tramo asignarCamionATramo(Long idTramo, AsignarCamionRequest request);

    Tramo iniciarTramo(Long idTramo);

    Tramo finalizarTramo(Long idTramo);

    // Obtener tramos asignados a una patente y con un estado (paginado)
    org.springframework.data.domain.Page<ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Tramo> obtenerTramosPorPatenteYEstado(String patente, Long estadoId, org.springframework.data.domain.Pageable pageable);

    // Marcar una ruta como asignada (y notificar a ms-solicitudes si corresponde)
    ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Ruta asignarRuta(Long idRuta);
}