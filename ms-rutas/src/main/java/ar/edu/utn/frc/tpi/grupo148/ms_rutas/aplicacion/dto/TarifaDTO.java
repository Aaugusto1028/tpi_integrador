package ar.edu.utn.frc.tpi.grupo148.ms_rutas.aplicacion.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TarifaDTO {
    private BigDecimal precioLitro;
    private BigDecimal costoEstadiaDiario;
}
