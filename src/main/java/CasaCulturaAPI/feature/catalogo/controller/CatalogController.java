package CasaCulturaAPI.feature.catalogo.controller;

import CasaCulturaAPI.feature.alumnos.dto.request.*;
import CasaCulturaAPI.feature.asistencias.dto.request.*;
import CasaCulturaAPI.feature.auth.dto.request.*;
import CasaCulturaAPI.feature.catalogo.dto.request.*;
import CasaCulturaAPI.feature.docentes.dto.request.*;
import CasaCulturaAPI.feature.inscripciones.dto.request.*;
import CasaCulturaAPI.feature.pagos.dto.request.*;
import CasaCulturaAPI.shared.dto.ApiResponse;
import CasaCulturaAPI.feature.catalogo.service.CatalogService;
import CasaCulturaAPI.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.API)
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService service;

    @PostMapping("/cursos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> crearCurso(@Valid @RequestBody CursoRequest request) { return created(service.crearCurso(request)); }
    @PutMapping("/cursos/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> actualizarCurso(@PathVariable Long id, @Valid @RequestBody CursoRequest request) {
        return ok(service.actualizarCurso(id, request));
    }
    @GetMapping("/cursos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DOCENTE', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> listarCursos() { return ok(service.listarCursos()); }
    @PostMapping("/categorias-edad")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> crearCategoria(@Valid @RequestBody CategoriaEdadRequest request) { return created(service.crearCategoria(request)); }
    @PutMapping("/categorias-edad/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> actualizarCategoria(@PathVariable Long id, @Valid @RequestBody CategoriaEdadRequest request) {
        return ok(service.actualizarCategoria(id, request));
    }
    @GetMapping("/categorias-edad")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DOCENTE', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> listarCategorias() { return ok(service.listarCategorias()); }
    @PostMapping("/ofertas")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> crearOferta(@Valid @RequestBody OfertaCursoRequest request) { return created(service.crearOferta(request)); }
    @PutMapping("/ofertas/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> actualizarOferta(@PathVariable Long id, @Valid @RequestBody OfertaCursoRequest request) {
        return ok(service.actualizarOferta(id, request));
    }
    @GetMapping("/ofertas")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DOCENTE', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> listarOfertas() { return ok(service.listarOfertas()); }
    @PostMapping("/grupos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> crearGrupo(@Valid @RequestBody GrupoRequest request) { return created(service.crearGrupo(request)); }
    @PutMapping("/grupos/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> actualizarGrupo(@PathVariable Long id, @Valid @RequestBody GrupoRequest request) {
        return ok(service.actualizarGrupo(id, request));
    }
    @GetMapping("/grupos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DOCENTE', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> listarGrupos() { return ok(service.listarGrupos()); }
    @PostMapping("/horarios")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> crearHorario(@Valid @RequestBody HorarioRequest request) { return created(service.crearHorario(request)); }
    @PutMapping("/horarios/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> actualizarHorario(@PathVariable Long id, @Valid @RequestBody HorarioRequest request) {
        return ok(service.actualizarHorario(id, request));
    }
    @GetMapping("/horarios")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DOCENTE', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> listarTodosHorarios() { return ok(service.listarTodosHorarios()); }
    @GetMapping("/grupos/{grupoId}/horarios")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DOCENTE', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<?>> listarHorarios(@PathVariable Long grupoId) { return ok(service.listarHorarios(grupoId)); }

    private ResponseEntity<ApiResponse<?>> created(Object data) { return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Registro creado correctamente.", data)); }
    private ResponseEntity<ApiResponse<?>> ok(Object data) { return ResponseEntity.ok(new ApiResponse<>(true, "Consulta realizada correctamente.", data)); }
}
