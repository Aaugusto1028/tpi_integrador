// Interfaz CamionRepository.java
package ar.edu.utnfrc.backend.mscamiones.repositories;
import java.util.List;
import ar.edu.utnfrc.backend.mscamiones.models.Camion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// La interfaz extiende JpaRepository:
// 1. Camion es la Entidad con la que trabajaremos.
// 2. String es el tipo de dato de la Clave Primaria (PK), que es 'patente'.
@Repository 
public interface CamionRepository extends JpaRepository<Camion, String> {
    
    // Con solo esta línea, Spring ya te da métodos como:
    // .save(camion): para crear o actualizar.
    // .findAll(): para listar todos.
    // .findById(patente): para buscar por PK.
    // .deleteById(patente): para eliminar.
    
    // --- Ejemplo de un método personalizado (Opcional) ---
    // Spring puede entender lo que quieres si nombras bien el método:
    List<Camion> findByDisponibilidad(Boolean disponibilidad); 
    // ^ Esto genera la consulta SQL: SELECT * FROM CAMIONES WHERE disponibilidad = ?
}
