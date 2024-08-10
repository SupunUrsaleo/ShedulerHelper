package com.ursaleo.health.controller;
import com.ursaleo.health.service.CommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
    @Autowired
    private CommandService commandService;

    @GetMapping("/getPort")
    public String getApplicationPort(@RequestParam String appName) {
        return commandService.getApplicationPort(appName);
    }

    @GetMapping("/executeBatchFile")
    public String runBatchFile() {
        return commandService.executeBatchFile();
    }
}
