package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/patient")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @Autowired
    private Service service;

    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> getPatient(@PathVariable String token) {
        ResponseEntity<Map<String, Object>> invalid = checkToken(token, "patient");
        if (invalid != null) return invalid;
        return patientService.getPatientDetails(token);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createPatient(@RequestBody @Valid Patient patient) {
        Map<String, String> response = new HashMap<>();
        if (!service.validatePatient(patient)) {
            response.put("message", "Patient with email id or phone no already exist");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
        if (patientService.createPatient(patient) == 1) {
            response.put("message", "Signup successful");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
        response.put("message", "Internal server error");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Login login) {
        return service.validatePatientLogin(login);
    }

    // Patient reading their own appointments
    @GetMapping("/{id}/{token}")
    public ResponseEntity<Map<String, Object>> getPatientAppointment(@PathVariable Long id,
                                                                     @PathVariable String token) {
        return getPatientAppointment(id, "patient", token);
    }

    // Shared by the patient dashboard (user = "patient") and the doctor's patient record page (user = "doctor")
    @GetMapping("/{id}/{user}/{token}")
    public ResponseEntity<Map<String, Object>> getPatientAppointment(@PathVariable Long id,
                                                                     @PathVariable String user,
                                                                     @PathVariable String token) {
        ResponseEntity<Map<String, Object>> invalid = checkToken(token, user);
        if (invalid != null) return invalid;
        return patientService.getPatientAppointment(id, token);
    }

    @GetMapping("/filter/{condition}/{name}/{token}")
    public ResponseEntity<Map<String, Object>> filterPatientAppointment(@PathVariable String condition,
                                                                        @PathVariable String name,
                                                                        @PathVariable String token) {
        ResponseEntity<Map<String, Object>> invalid = checkToken(token, "patient");
        if (invalid != null) return invalid;
        return service.filterPatient(condition, name, token);
    }

    // Returns an error response when the token is invalid, otherwise null
    private ResponseEntity<Map<String, Object>> checkToken(String token, String user) {
        ResponseEntity<Map<String, String>> tokenCheck = service.validateToken(token, user);
        if (tokenCheck.getBody().isEmpty()) {
            return null;
        }
        return new ResponseEntity<>(new HashMap<>(tokenCheck.getBody()), tokenCheck.getStatusCode());
    }
}
