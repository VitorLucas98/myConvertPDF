package vbotelho.dev.myConvertPdf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyConvertPdfApplication {

	public static void main(String[] args) {
		// Configura o limite de arquivos multipart do Tomcat ANTES da inicialização
		// O limite padrão do Tomcat é 10 arquivos, aqui aumentamos para 200
		// Esta propriedade é reconhecida pelo Apache Commons FileUpload usado pelo Tomcat
		System.setProperty("org.apache.tomcat.util.http.fileupload.FileCountLimit", "200");
		
		SpringApplication.run(MyConvertPdfApplication.class, args);
	}

}
