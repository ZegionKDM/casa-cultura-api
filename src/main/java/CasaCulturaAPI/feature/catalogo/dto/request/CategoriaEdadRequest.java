package CasaCulturaAPI.feature.catalogo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoriaEdadRequest {
    @NotBlank @Size(max = 80)
    private String nombre;
}
