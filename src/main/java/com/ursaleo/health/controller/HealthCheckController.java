package com.ursaleo.health.controller;

import com.ursaleo.health.service.CommandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
    @Autowired
    private CommandService commandService;

    @GetMapping("/getPort")
    public String getApplicationPort() {
        return commandService.getApplicationPort();
    }

    @GetMapping("/executeBatchFile/{publicIp}/{privateIp}")
    public String runBatchFile(@PathVariable String publicIp, @PathVariable String privateIp) {
        return commandService.executeBatchFile(publicIp, privateIp);
    }
}
