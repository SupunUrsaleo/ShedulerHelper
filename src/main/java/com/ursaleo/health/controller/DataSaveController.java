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
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;

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

                    if ("user_data".equals(key)) {
                        String scriptPath = "/home/ubuntu/create_busy_file.sh";  // Change to your actual script path
                        String dockerImage = "8011_49100";  // Modify this if needed

                        try {
                            executeShellScript(scriptPath, dockerImage);
                            logger.info("✅ Shell script executed successfully: {}", scriptPath);
                        } catch (IOException | InterruptedException e) {
                            logger.error("❌ Error executing shell script: {}", scriptPath, e);
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

    private void executeShellScript(String scriptPath, String dockerImage) throws IOException, InterruptedException {
    File scriptFile = new File(scriptPath);

    // Ensure the script exists before executing
    if (!scriptFile.exists() || !scriptFile.canExecute()) {
        logger.error("❌ Shell script '{}' does not exist or is not executable.", scriptPath);
        return;
    }

    // Build the process to execute the script with the Docker image name
    ProcessBuilder processBuilder = new ProcessBuilder(scriptPath, dockerImage);
    processBuilder.redirectErrorStream(true); // Merge error and output streams

    Process process = processBuilder.start();

    // Read the shell script output
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    String line;
    while ((line = reader.readLine()) != null) {
        logger.info("[Shell Output] {}", line);
    }

    // Wait for the process to complete
    int exitCode = process.waitFor();
    if (exitCode == 0) {
        logger.info("✅ Shell script '{}' executed successfully.", scriptPath);
    } else {
        logger.error("❌ Shell script '{}' failed with exit code {}.", scriptPath, exitCode);
    }
    }

}
