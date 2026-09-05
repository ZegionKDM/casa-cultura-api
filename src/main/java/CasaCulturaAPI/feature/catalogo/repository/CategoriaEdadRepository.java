package CasaCulturaAPI.feature.catalogo.repository;

import CasaCulturaAPI.shared.entity.CategoriaEdad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaEdadRepository extends JpaRepository<CategoriaEdad, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
}
