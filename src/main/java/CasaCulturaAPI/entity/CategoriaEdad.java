package CasaCulturaAPI.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categorias_edad")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoriaEdad extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    private String nombre;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;
}
