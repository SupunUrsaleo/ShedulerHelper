package com.ursaleo.health.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileWriter;


@Service
@Slf4j
public class CommandService {

    @Value("${batch.file.location}")
    private String batchFileLocation;

    @Value("${server.app.name}")
    private String appName;

    public String getApplicationPort() {
        String port = "";
        try {
            // Step 1: Get the PID of the application
            String pid = getPid(appName);
            if (pid == null || pid.isEmpty()) {
                port = "NOT_FOUND";
            } else {
                // Step 2: Get the port associated with the PID
                port = getPorts(pid);
                if (port == null || port.isEmpty()) {
                    port = "Port not found for PID: " + pid;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            port = "Exception when getting port: " + e.getMessage();
        }

        return port;
    }

    private String getPid(String appName) throws Exception {
        String command = "ps aux | grep " + appName + " | grep -v grep";
        Process pidProcess = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", command});
        BufferedReader pidReader = new BufferedReader(new InputStreamReader(pidProcess.getInputStream()));
        String pidLine = pidReader.readLine();

        if (pidLine == null || pidLine.isEmpty()) {
            return null;
        }

        String[] pidParts = pidLine.split("\\s+");
        return pidParts[1]; // Assuming the PID is the second element
    }

    private static String getPorts(String pid) throws Exception {
        String command = "netstat -tuln | grep " + pid;
        Process portProcess = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", command});
        BufferedReader portReader = new BufferedReader(new InputStreamReader(portProcess.getInputStream()));
        String portLine;

        while ((portLine = portReader.readLine()) != null) {
            return portLine.trim();
        }

        return null;
    }

    public String executeBatchFile(String publicIp, String privateIp) {
        try {
            // Desired image to select
            String desiredImage = "8211_49200:latest";

            // Command to launch the container directly
            String[] command = {
                "/bin/bash",
                "-c",
                "sudo ./repo.sh launch --container --image \"" + desiredImage + "\""
            };

            // Set up the ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File("/home/ubuntu/-gemini-kit-106.3")); // Replace with actual directory
            processBuilder.redirectErrorStream(true);

            // Start the process
            Process process = processBuilder.start();

            // Read the output from the process
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line); // Log process output
                }
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return "SUCCESS";
            } else {
                return "Process exited with error code: " + exitCode;
            }

        } catch (Exception e) {
            log.error("Error executing repo.sh: {}", e.getMessage(), e);
            return "Error executing repo.sh: " + e.getMessage();
        }
    }


}
