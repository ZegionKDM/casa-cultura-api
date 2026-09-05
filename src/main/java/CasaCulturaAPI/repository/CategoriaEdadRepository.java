package CasaCulturaAPI.repository;

import CasaCulturaAPI.entity.CategoriaEdad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaEdadRepository extends JpaRepository<CategoriaEdad, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
}
