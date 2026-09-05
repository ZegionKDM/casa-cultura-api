package CasaCulturaAPI.feature.docentes.repository;

import CasaCulturaAPI.shared.entity.Docente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocenteRepository extends JpaRepository<Docente, Long> {
    Optional<Docente> findByPersonaId(Long personaId);
}
