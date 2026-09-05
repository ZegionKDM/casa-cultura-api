package CasaCulturaAPI.feature.catalogo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CursoRequest {
    @NotBlank @Size(max = 120)
    private String nombre;
}
