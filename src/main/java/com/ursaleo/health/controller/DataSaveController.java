package com.ursaleo.health.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DataSaveController {

    private static final Logger logger = LoggerFactory.getLogger(DataSaveController.class);

    @Value("${save.path}")  // Ensure this is set in application.properties or application.yml
    private String savePath;

    private final ObjectMapper objectMapper;

    public DataSaveController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping("/savePartnerData")
    public ResponseEntity<String> savePartnerData(@RequestBody Map<String, Object> partnerData) {
        Map<String, Object> partnerSecureData = (Map<String, Object>) partnerData.get("partnerSecureData");

        if (partnerSecureData == null) {
            logger.error("No partnerSecureData found in the request.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No partnerSecureData found in the request.");
        }

        try {
            // Ensure the directory exists (Linux-compatible)
            Path directoryPath = Paths.get(savePath);
            Files.createDirectories(directoryPath);
            logger.info("Ensured directory exists: {}", directoryPath.toAbsolutePath());

            // Process each data key
            for (Map.Entry<String, Object> entry : partnerSecureData.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                try {
                    Path filePath = directoryPath.resolve(key + ".json");

                    // Write JSON data with UTF-8 encoding (Linux-safe)
                    Files.write(filePath, objectMapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    logger.info("Data for key '{}' saved successfully to '{}'", key, filePath.toAbsolutePath());

                    // If the key is "user_data", create "Busy.txt"
                    if ("user_data".equals(key)) {
                        Path busyFilePath = directoryPath.resolve("Busy.txt");
                        if (!Files.exists(busyFilePath)) {
                            Files.createFile(busyFilePath);
                            logger.info("File 'Busy.txt' created successfully at '{}'", busyFilePath.toAbsolutePath());
                        } else {
                            logger.warn("File 'Busy.txt' already exists at '{}'", busyFilePath.toAbsolutePath());
                        }
                    }

                } catch (IOException e) {
                    logger.error("Error saving data for key '{}'", key, e);
                }
            }

            return ResponseEntity.status(HttpStatus.OK).body("Data saved successfully!");

        } catch (IOException e) {
            logger.error("Error creating directories at '{}'", savePath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating directories.");
        }
    }
}
