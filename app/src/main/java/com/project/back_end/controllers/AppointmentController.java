package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private Service service;

    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<Map<String, Object>> getAppointments(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String patientName,
            @PathVariable String token) {
        ResponseEntity<Map<String, String>> tokenCheck = service.validateToken(token, "doctor");
        if (!tokenCheck.getBody().isEmpty()) {
            return new ResponseEntity<>(new HashMap<>(tokenCheck.getBody()), tokenCheck.getStatusCode());
        }
        return ResponseEntity.ok(appointmentService.getAppointment(patientName, date, token));
    }

    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> bookAppointment(@PathVariable String token,
                                                               @RequestBody @Valid Appointment appointment) {
        ResponseEntity<Map<String, String>> tokenCheck = service.validateToken(token, "patient");
        if (!tokenCheck.getBody().isEmpty()) {
            return tokenCheck;
        }

        Map<String, String> response = new HashMap<>();
        int validation = service.validateAppointment(appointment);
        if (validation == -1) {
            response.put("message", "Invalid doctor id");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if (validation == 0) {
            response.put("message", "Appointment already booked for given time or doctor not available");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        if (appointmentService.bookAppointment(appointment) == 1) {
            response.put("message", "Appointment booked successfully");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
        response.put("message", "Some internal error occurred");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateAppointment(@PathVariable String token,
                                                                 @RequestBody @Valid Appointment appointment) {
        ResponseEntity<Map<String, String>> tokenCheck = service.validateToken(token, "patient");
        if (!tokenCheck.getBody().isEmpty()) {
            return tokenCheck;
        }
        return appointmentService.updateAppointment(appointment);
    }

    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> cancelAppointment(@PathVariable long id,
                                                                 @PathVariable String token) {
        ResponseEntity<Map<String, String>> tokenCheck = service.validateToken(token, "patient");
        if (!tokenCheck.getBody().isEmpty()) {
            return tokenCheck;
        }
        return appointmentService.cancelAppointment(id, token);
    }
}
