package CasaCulturaAPI.repository;

import CasaCulturaAPI.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository extends JpaRepository<Persona, Long> {
}
