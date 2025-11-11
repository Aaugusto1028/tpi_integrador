package com.transporte.ms_solicitudes.controller;

import com.transporte.ms_solicitudes.model.Cliente;
import com.transporte.ms_solicitudes.service.ClienteService; // <-- 1. Faltaba este import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
public class ClienteController { // <-- 2. Nombre de clase corregido

    @Autowired
    private ClienteService clienteService;

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerClientePorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarClientePorId(id));
    }
    
    // 3. Faltaba la anotación @PostMapping
    @PostMapping
    public ResponseEntity<?> crearCliente(@RequestBody Cliente cliente) {
        return ResponseEntity.ok(clienteService.crearCliente(cliente));
    }
}