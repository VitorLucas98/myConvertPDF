package vbotelho.dev.myConvertPdf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionResponse {
    private boolean success;
    private String message;
    private String downloadUrl;
    private String filename;
    private int totalImages;
    private int processedImages;
    private long processingTimeMs;
    private String errorDetails;
}
