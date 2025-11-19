package ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion;

import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.AsignarCamionRequest;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.CrearRutaRequest;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Ruta;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Tramo;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.TarifaDTO;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.CostoTrasladoDTO;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.TramoDTO;
import ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto.RutaDTO;
import java.util.List;

public interface RutaService {

    Ruta crearRutaTentativa(CrearRutaRequest request);

    Tramo asignarCamionATramo(Long idTramo, AsignarCamionRequest request);

    Tramo iniciarTramo(Long idTramo);

    Tramo finalizarTramo(Long idTramo);

    // Obtener tramos asignados a una patente y con un estado (paginado)
    org.springframework.data.domain.Page<ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Tramo> obtenerTramosPorPatenteYEstado(String patente, Long estadoId, org.springframework.data.domain.Pageable pageable);

    // Marcar una ruta como asignada (y notificar a ms-solicitudes si corresponde)
    ar.edu.utn.frc.tpi.grupo148.ms_rutas.dominio.Ruta asignarRuta(Long idRuta);

    // Endpoint para exponer la tarifa vigente a otros servicios
    TarifaDTO obtenerTarifas();

    // Método para obtener el costo real de un traslado por idSolicitud (desglosado)
    CostoTrasladoDTO obtenerCostoTrasladoRealPorSolicitud(Long idSolicitud);

    // Método para obtener el costo real de un traslado por idRuta (desglosado)
    CostoTrasladoDTO obtenerCostoTrasladoReal(Long idRuta);

    // Método para obtener tramos asignados a una patente (devuelve TramoDTO con todos los datos)
    List<TramoDTO> obtenerTramosAsignadosPorPatente(String patente);

    // Método para obtener una ruta con todos sus tramos y detalles completos (RutaDTO)
    RutaDTO obtenerRutaConDetalles(Long idRuta);

    // Método para obtener todas las rutas de una solicitud con sus tramos y detalles completos
    List<RutaDTO> obtenerRutasConDetallesPorSolicitud(Long idSolicitud);

    // Obtener tiempo real (en HORAS) de una solicitud basándose en fechas de tramos
    Double obtenerTiempoRealPorSolicitud(Long idSolicitud);

    // Elegir una ruta y descartar todas las demás de la misma solicitud
    RutaDTO elegirRuta(Long idRuta);
}