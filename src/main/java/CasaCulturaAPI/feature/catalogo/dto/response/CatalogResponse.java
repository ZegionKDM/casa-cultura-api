package CasaCulturaAPI.feature.catalogo.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class CatalogResponse {
    private Long id;
    private String nombre;
    private String tipo;
    private Long cursoId;
    private Long ofertaId;
    private Long categoriaId;
    private Long grupoId;
    private String nombreGrupo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private DayOfWeek dia;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}
