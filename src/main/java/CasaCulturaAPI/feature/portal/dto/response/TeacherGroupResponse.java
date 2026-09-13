package CasaCulturaAPI.feature.portal.dto.response;

import CasaCulturaAPI.feature.catalogo.dto.response.CatalogResponse;
import CasaCulturaAPI.shared.entity.EstadoRegistro;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherGroupResponse {
    private Long id;
    private String nombreGrupo;
    private Long cursoId;
    private String curso;
    private Long categoriaId;
    private String categoria;
    private String periodo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private int totalAlumnos;
    private EstadoRegistro estado;
    private List<CatalogResponse> horarios;
}
