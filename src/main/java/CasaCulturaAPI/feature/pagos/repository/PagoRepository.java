package CasaCulturaAPI.feature.pagos.repository;

import CasaCulturaAPI.shared.entity.Pago;
import CasaCulturaAPI.shared.entity.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByInscripcionIn(Collection<Inscripcion> inscripciones);
}
