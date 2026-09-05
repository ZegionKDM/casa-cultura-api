package CasaCulturaAPI.shared.repository;

import CasaCulturaAPI.shared.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository extends JpaRepository<Persona, Long> {
}
