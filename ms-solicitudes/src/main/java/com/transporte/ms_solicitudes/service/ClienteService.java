package com.transporte.ms_solicitudes.service;

import com.transporte.ms_solicitudes.model.Cliente;
import com.transporte.ms_solicitudes.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public Cliente buscarClientePorId(Long id) {
        if (id == null) {
        throw new IllegalArgumentException("El ID del cliente no puede ser nulo");
    }
        return clienteRepository.findById(id)   
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
    }

    @Transactional
    public Cliente crearCliente(Cliente cliente) {
        // Lógica de negocio: Validamos que no exista otro cliente con el mismo DNI
        if (clienteRepository.findByDni(cliente.getDni()).isPresent()) {
            throw new IllegalStateException("Ya existe un cliente con el DNI: " + cliente.getDni());
        }
        // Lógica de negocio: Validamos que no exista otro cliente con el mismo email
        if (clienteRepository.findByEmail(cliente.getEmail()).isPresent()) {
            throw new IllegalStateException("Ya existe un cliente con el email: " + cliente.getEmail());
        }
        
        return clienteRepository.save(cliente);
    }
}