package CasaCulturaAPI.shared.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /**
     * Guarda una foto validada en el subdirectorio especificado dentro de la carpeta de uploads.
     * 
     * @param archivo Archivo multipart recibido en la petición
     * @param subdirectorio Nombre del subdirectorio (ej. "alumnos", "docentes")
     * @return Ruta URL relativa lista para servir (/uploads/fotos/{subdirectorio}/{nombreArchivo})
     */
    String guardarFoto(MultipartFile archivo, String subdirectorio);

    /**
     * Elimina un archivo físico existente si pertenece a la carpeta de uploads de manera segura.
     *
     * @param rutaRelativa Ruta relativa guardada en BD (ej. /uploads/fotos/alumnos/abc.jpg)
     */
    void eliminarArchivo(String rutaRelativa);
}
