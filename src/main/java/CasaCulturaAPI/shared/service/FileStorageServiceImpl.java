package CasaCulturaAPI.shared.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public String guardarFoto(MultipartFile archivo, String subdirectorio) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("No se ha proporcionado ningún archivo para subir.");
        }

        if (archivo.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("La fotografía no debe superar los 5 MB.");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Formato no válido. Solo se admiten imágenes en formato JPG, PNG o WEBP.");
        }

        // Sanitizar subdirectorio para evitar path traversal
        String safeSubdir = subdirectorio != null ? subdirectorio.replaceAll("[^a-zA-Z0-9_-]", "") : "general";
        if (safeSubdir.isBlank()) {
            safeSubdir = "general";
        }

        String extension = determineExtension(contentType, archivo.getOriginalFilename());
        String fileName = "foto_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        try {
            Path targetFolder = Paths.get(uploadDir, "fotos", safeSubdir).toAbsolutePath().normalize();
            Files.createDirectories(targetFolder);

            Path targetPath = targetFolder.resolve(fileName).normalize();
            // Prevenir escape del directorio destino
            if (!targetPath.startsWith(targetFolder)) {
                throw new IllegalArgumentException("Ruta de archivo no permitida.");
            }

            Files.copy(archivo.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Foto almacenada con éxito en: {}", targetPath);

            return "/uploads/fotos/" + safeSubdir + "/" + fileName;
        } catch (IOException e) {
            log.error("Error al guardar archivo en disco: {}", e.getMessage(), e);
            throw new IllegalStateException("No fue posible guardar la fotografía en el servidor. Intenta de nuevo.");
        }
    }

    @Override
    public void eliminarArchivo(String rutaRelativa) {
        if (rutaRelativa == null || !rutaRelativa.startsWith("/uploads/")) {
            return;
        }

        try {
            // Remueve el prefijo /uploads/ para resolver en la ruta base configurada
            String subPath = rutaRelativa.substring("/uploads/".length());
            Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = basePath.resolve(subPath).normalize();

            // Garantizar que la ruta no salga del directorio de uploads
            if (filePath.startsWith(basePath) && Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Archivo eliminado correctamente: {}", filePath);
            }
        } catch (Exception e) {
            log.warn("No se pudo eliminar el archivo {}: {}", rutaRelativa, e.getMessage());
        }
    }

    private String determineExtension(String contentType, String originalFilename) {
        if (contentType != null) {
            switch (contentType.toLowerCase()) {
                case "image/png":
                    return ".png";
                case "image/webp":
                    return ".webp";
                case "image/jpeg":
                case "image/jpg":
                    return ".jpg";
            }
        }
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (ext.equals(".png") || ext.equals(".webp") || ext.equals(".jpg") || ext.equals(".jpeg")) {
                return ext;
            }
        }
        return ".jpg";
    }
}
