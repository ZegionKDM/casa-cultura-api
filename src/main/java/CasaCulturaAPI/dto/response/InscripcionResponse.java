package CasaCulturaAPI.dto.response;

import CasaCulturaAPI.entity.EstadoInscripcion;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data @Builder
public class InscripcionResponse {
    private Long id;
    private Long alumnoId;
    private String matricula;
    private Long grupoId;
    private String grupo;
    private LocalDate fechaInscripcion;
    private EstadoInscripcion estado;
}
