package com.ursaleo.health.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class CommandService {

    @Value("${batch.file.location}")
    private String batchFileLocation;

    public String getApplicationPort(String appName) {
        try {
            // Step 1: Get the PID of the application
            String pid = getPid(appName);
            if (pid == null || pid.isEmpty()) {
                return "Application not found";
            }

            // Step 2: Get the port associated with the PID
            String ports = getPorts(pid);
            if (ports == null || ports.isEmpty()) {
                return "No ports found for PID: " + pid;
            }

            return ports;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred: " + e.getMessage();
        }
    }

    private String getPid(String appName) throws Exception {
        String command = "cmd.exe /c tasklist | findstr " + appName;
        Process pidProcess = Runtime.getRuntime().exec(command);
        BufferedReader pidReader = new BufferedReader(new InputStreamReader(pidProcess.getInputStream()));
        String pidLine = pidReader.readLine();

        if (pidLine == null || pidLine.isEmpty()) {
            return null;
        }

        String[] pidParts = pidLine.split("\\s+");
        return pidParts[1]; // Assuming the PID is the second element
    }

    private static String getPorts(String pid) throws Exception {
        String command = "cmd.exe /c for /f \"tokens=2 delims=:\" %i in ('netstat -ano ^| findstr /r /c:\"TCP.*" + pid + "\"') do @echo %i | for /f \"tokens=1 delims= \" %j in ('more') do @echo %j";
        Process portProcess = Runtime.getRuntime().exec(command);
        BufferedReader portReader = new BufferedReader(new InputStreamReader(portProcess.getInputStream()));
        StringBuilder ports = new StringBuilder();
        String portLine;

        while ((portLine = portReader.readLine()) != null) {
            return portLine.trim();
        }

        return null; // or return an appropriate message if no port is found
    }

    public String executeBatchFile() {
        try {
            String[] command = {"cmd.exe", "/c", "start", batchFileLocation};
            Process process = Runtime.getRuntime().exec(command);
            return "SUCCESS";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error executing batch file: " + e.getMessage();
        }
    }
}
