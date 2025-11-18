package com.transporte.ms_solicitudes.controller;

import com.transporte.ms_solicitudes.model.Cliente;
import com.transporte.ms_solicitudes.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTE')")
    public ResponseEntity<?> obtenerClientePorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(clienteService.buscarClientePorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping
    @PreAuthorize("hasAuthority('CLIENTE')")
    public ResponseEntity<?> crearCliente(@RequestBody Cliente cliente) {
        try {
            // Validar que los campos obligatorios no sean nulos
            if (cliente.getNombre() == null || cliente.getNombre().isBlank()) {
                return ResponseEntity.badRequest().body("El nombre del cliente es obligatorio");
            }
            if (cliente.getApellido() == null || cliente.getApellido().isBlank()) {
                return ResponseEntity.badRequest().body("El apellido del cliente es obligatorio");
            }
            if (cliente.getDni() == null || cliente.getDni().isBlank()) {
                return ResponseEntity.badRequest().body("El DNI del cliente es obligatorio");
            }
            if (cliente.getEmail() == null || cliente.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body("El email del cliente es obligatorio");
            }
            
            Cliente nuevoCliente = clienteService.crearCliente(cliente);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoCliente);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear cliente: " + e.getMessage());
        }
    }
}