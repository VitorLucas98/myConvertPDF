package vbotelho.dev.myConvertPdf.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;

@Configuration
public class AppConfig {

    @Value("${app.upload.temp-dir}")
    private String tempDir;

    @Value("${app.upload.max-files:100}")
    private int maxFiles;

    @PostConstruct
    public void init() {
        try {
            Path tempPath = Paths.get(tempDir);
            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath);
                System.out.println("✅ Diretório temporário criado: " + tempPath);
            }
            
        } catch (IOException e) {
            System.err.println("❌ Erro ao criar diretório temporário: " + e.getMessage());
        }
    }

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("pdf-converter-");
        executor.initialize();
        return executor;
    }
}

