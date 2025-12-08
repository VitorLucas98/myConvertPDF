package vbotelho.dev.myConvertPdf.service;


import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.AreaBreakType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vbotelho.dev.myConvertPdf.service.exception.ConversionException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Slf4j
@Service
public class PdfConversionService {
    @Value("${app.upload.temp-dir}")
    private String tempDir;

    @Value("${app.processing.batch-size}")
    private int batchSize;

    /**
     * Converte múltiplas imagens em um único PDF
     *
     * @param images Lista de imagens
     * @return Caminho do arquivo PDF gerado
     */
    public Path convertToPdf(List<MultipartFile> images) {
        String filename = "converted_" + UUID.randomUUID() + ".pdf";
        Path outputPath = Paths.get(tempDir, filename);

        log.info("Iniciando conversão de {} imagens para PDF único", images.size());
        long startTime = System.currentTimeMillis();

        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile());
             PdfWriter writer = new PdfWriter(fos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            int processedCount = 0;

            for (MultipartFile imageFile : images) {
                try {
                    addImageToDocument(document, pdfDoc, imageFile, processedCount > 0);
                    processedCount++;

                    // Log de progresso a cada batch
                    if (processedCount % batchSize == 0) {
                        log.debug("Processadas {} de {} imagens", processedCount, images.size());
                        // Sugestão de garbage collection para liberar memória
                        System.gc();
                    }

                } catch (Exception e) {
                    log.error("Erro ao processar imagem: {}", imageFile.getOriginalFilename(), e);
                    // Continua processando as demais imagens
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Conversão concluída: {} imagens processadas em {}ms", processedCount, duration);

            return outputPath;

        } catch (Exception e) {
            log.error("Erro ao criar PDF", e);
            throw new ConversionException("Erro ao criar PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Converte cada imagem em um PDF separado e compacta tudo em ZIP
     *
     * @param images Lista de imagens
     * @return Caminho do arquivo ZIP gerado
     */
    public Path convertToMultiplePdfsZip(List<MultipartFile> images) {
        String zipFilename = "converted_pdfs_" + UUID.randomUUID() + ".zip";
        Path zipPath = Paths.get(tempDir, zipFilename);

        log.info("Iniciando conversão de {} imagens para PDFs individuais", images.size());
        long startTime = System.currentTimeMillis();

        List<Path> pdfFiles = new ArrayList<>();

        try {
            // Criar PDFs individuais
            int processedCount = 0;
            for (MultipartFile imageFile : images) {
                try {
                    Path pdfPath = createSingleImagePdf(imageFile);
                    pdfFiles.add(pdfPath);
                    processedCount++;

                    if (processedCount % batchSize == 0) {
                        log.debug("Processadas {} de {} imagens", processedCount, images.size());
                        System.gc();
                    }

                } catch (Exception e) {
                    log.error("Erro ao processar imagem: {}", imageFile.getOriginalFilename(), e);
                }
            }

            // Criar arquivo ZIP
            createZipFile(pdfFiles, zipPath);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Conversão concluída: {} PDFs criados e compactados em {}ms",
                    pdfFiles.size(), duration);

            // Limpar arquivos temporários PDF
            cleanupTempFiles(pdfFiles);

            return zipPath;

        } catch (Exception e) {
            log.error("Erro ao criar PDFs múltiplos", e);
            cleanupTempFiles(pdfFiles);
            throw new ConversionException("Erro ao criar PDFs múltiplos: " + e.getMessage(), e);
        }
    }

    /**
     * Adiciona uma imagem ao documento PDF
     */
    private void addImageToDocument(Document document, PdfDocument pdfDoc,
                                    MultipartFile imageFile, boolean addPageBreak) throws IOException {

        // Adicionar quebra de página se não for a primeira imagem
        if (addPageBreak) {
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        }

        byte[] imageBytes = imageFile.getBytes();
        Image image = new Image(ImageDataFactory.create(imageBytes));

        // Ajustar imagem ao tamanho da página (A4)
        PageSize pageSize = PageSize.A4;
        float pageWidth = pageSize.getWidth() - 72; // Margem de 36 pontos de cada lado
        float pageHeight = pageSize.getHeight() - 72;

        // Calcular escala mantendo proporção
        float imageWidth = image.getImageWidth();
        float imageHeight = image.getImageHeight();

        float scaleWidth = pageWidth / imageWidth;
        float scaleHeight = pageHeight / imageHeight;
        float scale = Math.min(scaleWidth, scaleHeight);

        image.setWidth(imageWidth * scale);
        image.setHeight(imageHeight * scale);

        // Centralizar imagem
        image.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

        document.add(image);
    }

    /**
     * Cria um PDF contendo uma única imagem
     */
    private Path createSingleImagePdf(MultipartFile imageFile) throws IOException {
        String originalName = imageFile.getOriginalFilename();
        String baseName = originalName != null ?
                originalName.substring(0, originalName.lastIndexOf('.')) : "image";
        String pdfFilename = baseName + "_" + UUID.randomUUID() + ".pdf";
        Path outputPath = Paths.get(tempDir, pdfFilename);

        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile());
             PdfWriter writer = new PdfWriter(fos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            addImageToDocument(document, pdfDoc, imageFile, false);
            return outputPath;

        } catch (Exception e) {
            throw new ConversionException("Erro ao criar PDF individual: " + e.getMessage(), e);
        }
    }

    /**
     * Cria arquivo ZIP com os PDFs gerados
     */
    private void createZipFile(List<Path> pdfFiles, Path zipPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (Path pdfFile : pdfFiles) {
                ZipEntry zipEntry = new ZipEntry(pdfFile.getFileName().toString());
                zos.putNextEntry(zipEntry);

                Files.copy(pdfFile, zos);
                zos.closeEntry();
            }

            log.debug("Arquivo ZIP criado com {} arquivos", pdfFiles.size());
        }
    }

    /**
     * Remove arquivos temporários
     */
    private void cleanupTempFiles(List<Path> files) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warn("Não foi possível excluir arquivo temporário: {}", file, e);
            }
        }
    }

    /**
     * Remove um arquivo específico
     */
    public void deleteFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
            log.debug("Arquivo removido: {}", filePath);
        } catch (IOException e) {
            log.warn("Erro ao remover arquivo: {}", filePath, e);
        }
    }
}
