package ar.edu.utnfrc.backend.mscamiones.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromediosDTO {
    private BigDecimal costoPromedioPorKm;
    private BigDecimal consumoPromedioPorKm;
}
