package CasaCulturaAPI.service.interfaces;

import CasaCulturaAPI.dto.request.*;
import CasaCulturaAPI.dto.response.CatalogResponse;

import java.util.List;

public interface CatalogService {
    CatalogResponse crearCurso(CursoRequest request);
    CatalogResponse actualizarCurso(Long id, CursoRequest request);
    List<CatalogResponse> listarCursos();
    CatalogResponse crearCategoria(CategoriaEdadRequest request);
    CatalogResponse actualizarCategoria(Long id, CategoriaEdadRequest request);
    List<CatalogResponse> listarCategorias();
    CatalogResponse crearOferta(OfertaCursoRequest request);
    CatalogResponse actualizarOferta(Long id, OfertaCursoRequest request);
    List<CatalogResponse> listarOfertas();
    CatalogResponse crearGrupo(GrupoRequest request);
    CatalogResponse actualizarGrupo(Long id, GrupoRequest request);
    List<CatalogResponse> listarGrupos();
    CatalogResponse crearHorario(HorarioRequest request);
    CatalogResponse actualizarHorario(Long id, HorarioRequest request);
    List<CatalogResponse> listarHorarios(Long grupoId);
}
