package CasaCulturaAPI.feature.catalogo.repository;

import CasaCulturaAPI.shared.entity.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {
}
