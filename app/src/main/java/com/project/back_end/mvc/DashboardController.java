package com.project.back_end.mvc;

import com.project.back_end.services.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Controller
public class DashboardController {

    @Autowired
    private Service service;

    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        Map<String, String> errors = service.validateToken(token, "admin").getBody();
        if (errors == null || errors.isEmpty()) {
            return "admin/adminDashboard";
        }
        return "redirect:http://localhost:8080";
    }

    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        Map<String, String> errors = service.validateToken(token, "doctor").getBody();
        if (errors == null || errors.isEmpty()) {
            return "doctor/doctorDashboard";
        }
        return "redirect:http://localhost:8080";
    }
}
