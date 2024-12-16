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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DataSaveController {

    private static final Logger logger = LoggerFactory.getLogger(DataSaveController.class);

    @Value("${save.path}")
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
            // Ensure the directory exists
            Files.createDirectories(Paths.get(savePath));

            partnerSecureData.forEach((key, value) -> {
                try {
                    File file = new File(savePath + File.separator + key + ".json");
                    objectMapper.writeValue(file, value);
                    logger.info("Data for key '{}' saved successfully to '{}'", key, file.getPath());
            
                    // Check if the key is "user_data" before creating Busy.txt
                    if ("user_data".equals(key)) {
                        // Create an additional empty file named "Busy.txt"
                        File busyFile = new File(savePath + File.separator + "Busy.txt");
                        if (busyFile.createNewFile()) {
                            logger.info("File 'Busy.txt' created successfully at '{}'", busyFile.getPath());
                        } else {
                            logger.warn("File 'Busy.txt' already exists at '{}'", busyFile.getPath());
                        }
                    }
                } catch (IOException e) {
                    logger.error("Error saving data for key '{}'", key, e);
                }
            });                    

            return ResponseEntity.status(HttpStatus.OK).body("Data saved successfully!");

        } catch (IOException e) {
            logger.error("Error creating directories at '{}'", savePath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating directories.");
        }
    }
}
