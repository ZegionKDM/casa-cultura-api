package CasaCulturaAPI.controller;

import CasaCulturaAPI.dto.request.RolRequest;
import CasaCulturaAPI.dto.response.ApiResponse;
import CasaCulturaAPI.service.interfaces.RolService;
import CasaCulturaAPI.util.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.API + "/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService service;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> crear(
            @Valid @RequestBody RolRequest request){

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        true,
                        "Rol creado correctamente.",
                        service.crear(request)
                ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> listar(){

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Consulta realizada correctamente.",
                        service.obtenerTodos()
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> obtener(@PathVariable Long id){

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Rol encontrado.",
                        service.obtenerPorId(id)
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody RolRequest request){

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Rol actualizado.",
                        service.actualizar(id,request)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> eliminar(
            @PathVariable Long id){

        service.eliminar(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Rol eliminado.",
                        null
                )
        );
    }

}