package com.lias.gestion_laboratoire.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class PrivateAdhesionCvStorage {
    private static final long MAX_CV_BYTES = 5L * 1024 * 1024;

    @Value("${app.upload.private-dir:private-data/adhesions}")
    private String privateDirectory;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le CV est obligatoire.");
        }
        if (file.getSize() > MAX_CV_BYTES) {
            throw new IllegalArgumentException("Le CV ne doit pas dépasser 5 Mo.");
        }
        try {
            byte[] header = file.getInputStream().readNBytes(5);
            if (header.length != 5 || header[0] != '%' || header[1] != 'P'
                    || header[2] != 'D' || header[3] != 'F' || header[4] != '-') {
                throw new IllegalArgumentException("Le CV doit être un véritable fichier PDF.");
            }
            Path directory = Paths.get(privateDirectory).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String filename = UUID.randomUUID() + ".pdf";
            Path destination = directory.resolve(filename).normalize();
            if (!destination.startsWith(directory)) {
                throw new IllegalStateException("Le chemin de stockage du CV est invalide.");
            }
            file.transferTo(destination);
            return destination.toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible d’enregistrer le CV privé.", exception);
        }
    }

    public Resource load(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new IllegalArgumentException("Aucun CV n’est associé à cette demande.");
        }
        Path directory = Paths.get(privateDirectory).toAbsolutePath().normalize();
        Path path = Paths.get(storedPath).toAbsolutePath().normalize();
        if (!path.startsWith(directory) || !Files.isRegularFile(path)) {
            throw new IllegalStateException("Le fichier CV est introuvable dans le stockage privé.");
        }
        return new FileSystemResource(path);
    }

    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        Path directory = Paths.get(privateDirectory).toAbsolutePath().normalize();
        Path path = Paths.get(storedPath).toAbsolutePath().normalize();
        if (!path.startsWith(directory)) {
            throw new IllegalStateException("Le chemin du CV à nettoyer est invalide.");
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de nettoyer le CV privé non enregistré.", exception);
        }
    }
}
