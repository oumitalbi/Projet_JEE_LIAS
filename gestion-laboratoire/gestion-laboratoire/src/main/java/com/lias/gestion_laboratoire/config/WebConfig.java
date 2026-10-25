package com.lias.gestion_laboratoire.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration pour servir les fichiers uploadés (photos de profil)
 * en tant que ressources statiques.
 *
 * <p>Les fichiers stockés dans le dossier configuré par {@code app.upload.dir}
 * sont accessibles via l'URL {@code /uploads/photos/nomfichier.ext}.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/photos/**")
                .addResourceLocations("file:" + uploadPath.toString() + "/");
    }
}
