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
            int targetPort = 8011; // Change this to the port you want to check
            String command = "nc -zv localhost " + targetPort;
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", command});
            
            // Read from error stream instead of input stream
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream())); 
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("succeeded")) {
                    port = String.valueOf(targetPort);
                    break;
                }
            }
            
            if (port.isEmpty()) {
                port = "NOT_FOUND";
            }
        } catch (Exception e) {
            System.err.println("Exception when checking port: " + e.getMessage());
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
            String desiredImage = "8011_49100:latest";

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
