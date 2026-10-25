package com.lias.gestion_laboratoire.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PrivateAdhesionCvStorageTests {
    @TempDir
    Path privateDirectory;

    private PrivateAdhesionCvStorage storage;

    @BeforeEach
    void setUp() {
        storage = new PrivateAdhesionCvStorage();
        ReflectionTestUtils.setField(storage, "privateDirectory", privateDirectory.toString());
    }

    @Test
    void storesLoadsAndDeletesCvOnlyInPrivateDirectory() throws Exception {
        MockMultipartFile file = pdf("cv.pdf", "%PDF-1.7\ncontenu");

        String storedPath = storage.store(file);
        Path path = Path.of(storedPath);

        assertTrue(path.startsWith(privateDirectory));
        assertTrue(storage.load(storedPath).exists());
        storage.delete(storedPath);
        assertFalse(Files.exists(path));
    }

    @Test
    void rejectsFileWithoutPdfSignature() {
        assertThrows(IllegalArgumentException.class,
                () -> storage.store(new MockMultipartFile("cv", "cv.pdf", "application/pdf", "not a pdf".getBytes())));
    }

    @Test
    void rejectsPathOutsidePrivateDirectory() {
        assertThrows(IllegalStateException.class, () -> storage.load(privateDirectory.getParent().toString()));
        assertThrows(IllegalStateException.class, () -> storage.delete(privateDirectory.getParent().toString()));
    }

    private MockMultipartFile pdf(String filename, String content) {
        return new MockMultipartFile("cv", filename, "application/pdf", content.getBytes());
    }
}
