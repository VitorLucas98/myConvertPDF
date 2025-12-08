package vbotelho.dev.myConvertPdf.service.exception;

public class ConversionException extends  RuntimeException{
    public ConversionException() {
    }

    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
