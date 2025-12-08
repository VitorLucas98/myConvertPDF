package vbotelho.dev.myConvertPdf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vbotelho.dev.myConvertPdf.service.exception.InvalidUploadException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serviço responsável pela validação de arquivos de upload
 */
@Slf4j
@Service
public class ValidationService {

    @Value("${app.upload.max-files}")
    private int maxFiles;

    @Value("${app.upload.allowed-extensions}")
    private String allowedExtensionsStr;

    private Set<String> allowedExtensions;

    /**
     * Valida lista de arquivos enviados
     */
    public void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new InvalidUploadException("Nenhum arquivo foi enviado");
        }

        // Validar quantidade de arquivos
        if (files.size() > maxFiles) {
            throw new InvalidUploadException(
                    String.format("Número máximo de arquivos excedido. Máximo: %d, Enviados: %d",
                            maxFiles, files.size()));
        }

        // Inicializar extensões permitidas se necessário
        if (allowedExtensions == null) {
            allowedExtensions = Arrays.stream(allowedExtensionsStr.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        }

        // Validar cada arquivo
        for (MultipartFile file : files) {
            validateFile(file);
        }

        log.info("Validação concluída: {} arquivos válidos", files.size());
    }

    /**
     * Valida um arquivo individual
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidUploadException("Arquivo vazio detectado");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new InvalidUploadException("Nome de arquivo inválido");
        }

        // Validar extensão
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new InvalidUploadException(
                    String.format("Formato de arquivo não suportado: %s. Formatos permitidos: %s",
                            extension, allowedExtensionsStr));
        }

        // Validar tamanho (adicional à configuração do Spring)
        long maxSize = 50 * 1024 * 1024; // 50MB
        if (file.getSize() > maxSize) {
            throw new InvalidUploadException(
                    String.format("Arquivo muito grande: %s (%.2f MB). Tamanho máximo: 50 MB",
                            originalFilename, file.getSize() / (1024.0 * 1024.0)));
        }

        // Validar tipo MIME
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new InvalidUploadException(
                    String.format("Tipo de arquivo inválido: %s. Esperado: imagem", contentType));
        }
    }

    /**
     * Extrai a extensão do arquivo
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}
