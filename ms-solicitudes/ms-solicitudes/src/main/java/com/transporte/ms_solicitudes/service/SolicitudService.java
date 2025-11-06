package com.transporte.ms_solicitudes.service;

import com.transporte.ms_solicitudes.dto.EstadoDTO;
import com.transporte.ms_solicitudes.dto.SolicitudRequestDTO;
import com.transporte.ms_solicitudes.dto.SolicitudResponseDTO;
import com.transporte.ms_solicitudes.model.Cliente;
import com.transporte.ms_solicitudes.model.Contenedor;
import com.transporte.ms_solicitudes.model.EstadoContenedor;
import com.transporte.ms_solicitudes.model.Solicitud;
import com.transporte.ms_solicitudes.repository.ClienteRepository;
import com.transporte.ms_solicitudes.repository.SolicitudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    // (Más adelante) @Autowired private RutasFeignClient rutasFeignClient;
    // (Más adelante) @Autowired private CamionesFeignClient camionesFeignClient;


}