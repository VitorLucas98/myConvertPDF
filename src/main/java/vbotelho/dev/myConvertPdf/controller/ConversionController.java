package vbotelho.dev.myConvertPdf.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vbotelho.dev.myConvertPdf.dto.ConversionResponse;
import vbotelho.dev.myConvertPdf.enums.ConversionType;
import vbotelho.dev.myConvertPdf.service.PdfConversionService;
import vbotelho.dev.myConvertPdf.service.ValidationService;
import vbotelho.dev.myConvertPdf.service.exception.ConversionException;
import vbotelho.dev.myConvertPdf.service.exception.InvalidUploadException;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/convert")
@RequiredArgsConstructor
public class ConversionController {

    private final PdfConversionService pdfConversionService;
    private final ValidationService validationService;

    /**
     * Endpoint para conversão de imagens em PDF
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConversionResponse> convertImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("conversionType") String conversionTypeStr) {

        long startTime = System.currentTimeMillis();

        try {
            List<MultipartFile> imageFiles = Arrays.asList(files);
            ConversionType conversionType = ConversionType.valueOf(conversionTypeStr);

            log.info("Recebida requisição de conversão: {} arquivos, tipo: {}",
                    imageFiles.size(), conversionType);

            // Validar arquivos
            validationService.validateFiles(imageFiles);

            // Processar conversão
            Path resultPath;
            String filename;

            if (conversionType == ConversionType.SINGLE_PDF) {
                resultPath = pdfConversionService.convertToPdf(imageFiles);
                filename = resultPath.getFileName().toString();
            } else {
                resultPath = pdfConversionService.convertToMultiplePdfsZip(imageFiles);
                filename = resultPath.getFileName().toString();
            }

            long processingTime = System.currentTimeMillis() - startTime;

            ConversionResponse response = ConversionResponse.builder()
                    .success(true)
                    .message("Conversão realizada com sucesso")
                    .downloadUrl("/api/convert/download/" + filename)
                    .filename(filename)
                    .totalImages(imageFiles.size())
                    .processedImages(imageFiles.size())
                    .processingTimeMs(processingTime)
                    .build();

            return ResponseEntity.ok(response);

        } catch (InvalidUploadException e) {
            log.error("Erro de validação", e);
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (ConversionException e) {
            log.error("Erro de conversão", e);
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            log.error("Erro inesperado", e);
            return createErrorResponse("Erro inesperado: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint para download do arquivo convertido
     */
    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            String tempDir = System.getProperty("java.io.tmpdir") + "/image-to-pdf-temp";
            Path filePath = Path.of(tempDir, filename);

            if (!filePath.toFile().exists()) {
                log.error("Arquivo não encontrado: {}", filename);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath);

            // Determinar tipo de conteúdo
            String contentType = filename.endsWith(".zip") ?
                    "application/zip" : "application/pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);

            log.info("Download iniciado: {}", filename);

            // Agendar remoção do arquivo após download (em thread separada)
            scheduleFileCleanup(filePath);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(filePath.toFile().length())
                    .body(resource);

        } catch (Exception e) {
            log.error("Erro ao fazer download", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Cria resposta de erro padronizada
     */
    private ResponseEntity<ConversionResponse> createErrorResponse(String message, HttpStatus status) {
        ConversionResponse response = ConversionResponse.builder()
                .success(false)
                .message("Erro na conversão")
                .errorDetails(message)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Agenda limpeza do arquivo após download
     */
    private void scheduleFileCleanup(Path filePath) {
        new Thread(() -> {
            try {
                Thread.sleep(30000); // Aguarda 30 segundos
                pdfConversionService.deleteFile(filePath);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}