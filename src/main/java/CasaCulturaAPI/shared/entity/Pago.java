package CasaCulturaAPI.shared.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "pagos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pago extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPago tipoPago;
    @Column(length = 20)
    private String periodo;
    private LocalDate fechaVencimiento;
    private LocalDate fechaPago;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;
    private Double monto;
}
