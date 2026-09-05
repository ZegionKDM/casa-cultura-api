package CasaCulturaAPI.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "asignacion_docente")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AsignacionDocente extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;
    @Column(nullable = false)
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}
