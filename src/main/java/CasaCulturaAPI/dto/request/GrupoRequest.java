package CasaCulturaAPI.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class GrupoRequest {
    @NotNull private Long ofertaId;
    @NotNull private Long categoriaId;
    @NotBlank @Size(max = 100) private String nombreGrupo;
}
