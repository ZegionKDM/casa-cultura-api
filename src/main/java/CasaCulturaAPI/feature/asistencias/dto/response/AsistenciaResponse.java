package CasaCulturaAPI.feature.asistencias.dto.response;

import CasaCulturaAPI.shared.entity.EstadoAsistencia;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AsistenciaResponse {
    private Long id;
    private String matricula;
    private String alumno;
    private String grupo;
    private LocalDate fecha;
    private LocalDateTime horaRegistro;
    private EstadoAsistencia estado;
}
