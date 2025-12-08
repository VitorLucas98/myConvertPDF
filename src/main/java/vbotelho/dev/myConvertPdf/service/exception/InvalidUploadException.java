package vbotelho.dev.myConvertPdf.service.exception;

public class InvalidUploadException extends RuntimeException {
    public InvalidUploadException() {
    }

    public InvalidUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidUploadException(String message) {
        super(message);
    }
}
