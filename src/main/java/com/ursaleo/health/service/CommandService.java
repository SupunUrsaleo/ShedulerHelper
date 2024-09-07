package com.ursaleo.health.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;

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
            }else{
                // Step 2: Get the port associated with the PID
                 port = getPorts(pid);
                if (port == null || port.isEmpty()) {
                    port = "Port not found for PID: "+pid;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            port = "Exception when getting port : "+e.getMessage();
        }

        return port;
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
        String portLine;

        while ((portLine = portReader.readLine()) != null) {
            return portLine.trim();
        }

        return null;
    }

    public String executeBatchFile(String publicIp, String privateIp) {
        try {

         /*   String[] command = {"cmd.exe", "/K", "start.bat", publicIp, privateIp};
            log.info("Executing command {}", Arrays.toString(command));
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(batchFileLocation));
            processBuilder.start();*/

            CommandLine cmd = new CommandLine("cmd.exe ");
            cmd.addArgument("/c");
            String command = String.format("cmd.exe /K start.bat %s %s",publicIp, privateIp);
            cmd.addArgument(command,false);
            DefaultExecutor.builder().setWorkingDirectory(new File(batchFileLocation)).get().execute(cmd);
            return "SUCCESS";

        } catch (Exception e) {
            log.error("Error executing batch file: {}", e.getMessage(), e);
            return "Error executing batch file: " + e.getMessage();
        }
    }
}
