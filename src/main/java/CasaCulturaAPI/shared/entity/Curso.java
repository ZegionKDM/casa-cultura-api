package CasaCulturaAPI.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "cursos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Curso extends BaseEntity {
    @Column(nullable = false, unique = true, length = 120)
    private String nombre;
}
