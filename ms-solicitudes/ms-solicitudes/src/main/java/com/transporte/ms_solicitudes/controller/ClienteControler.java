package com.transporte.ms_solicitudes.controller;
import com.transporte.ms_solicitudes.model.Cliente;
import com.transporte.ms_solicitudes.service.ClienteService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
@RestController
@RequestMapping("/clientes") // Todos los endpoints aquí empiezan con /clientes
public class ClienteControler {

    @Autowired
    private ClienteService clienteService;
    // Endpoint: GET /clientes/{id}
    // Rol: Cliente
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerClientePorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarClientePorId(id));
    }
    //response Entitity es una clase generica que representa toda la respuesta HTTP
    public ResponseEntity<?> crearCliente(@RequestBody Cliente cliente) {
        return ResponseEntity.ok(clienteService.crearCliente(cliente));
    }


}