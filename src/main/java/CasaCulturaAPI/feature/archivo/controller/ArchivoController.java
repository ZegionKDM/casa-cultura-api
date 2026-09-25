package CasaCulturaAPI.feature.archivo.controller;

import CasaCulturaAPI.shared.dto.ApiResponse;
import CasaCulturaAPI.shared.service.FileStorageService;
import CasaCulturaAPI.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping(Constants.API + "/archivos")
@RequiredArgsConstructor
public class ArchivoController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/fotos/alumno", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<Map<String, String>>> subirFotoAlumno(
            @RequestParam("archivo") MultipartFile archivo) {
        String url = fileStorageService.guardarFoto(archivo, "alumnos");
        return ResponseEntity.ok(new ApiResponse<>(true, "Fotografía subida exitosamente.", Map.of("url", url)));
    }

    @PostMapping(value = "/fotos/docente", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<Map<String, String>>> subirFotoDocente(
            @RequestParam("archivo") MultipartFile archivo) {
        String url = fileStorageService.guardarFoto(archivo, "docentes");
        return ResponseEntity.ok(new ApiResponse<>(true, "Fotografía de docente subida exitosamente.", Map.of("url", url)));
    }
}
