package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // Returns 1 on success, 0 on error
    public int createPatient(Patient patient) {
        try {
            patientRepository.save(patient);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // A patient token may only read its own appointments; doctor tokens are checked by the controller
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            Patient requester = patientRepository.findByEmail(tokenService.extractIdentifier(token));
            if (requester != null && !requester.getId().equals(id)) {
                response.put("error", "Unauthorized");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            response.put("appointments", toDTOs(appointmentRepository.findByPatientId(id)));
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.put("error", "Some internal error occurred");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // "past" = completed (status 1), "future" = scheduled (status 0)
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        Map<String, Object> response = new HashMap<>();
        Integer status = statusFor(condition);
        if (status == null) {
            response.put("error", "Invalid condition. Use 'past' or 'future'");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        response.put("appointments",
                toDTOs(appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status)));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        Map<String, Object> response = new HashMap<>();
        response.put("appointments",
                toDTOs(appointmentRepository.filterByDoctorNameAndPatientId(name, patientId)));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(String condition, String name, long patientId) {
        Map<String, Object> response = new HashMap<>();
        Integer status = statusFor(condition);
        if (status == null) {
            response.put("error", "Invalid condition. Use 'past' or 'future'");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        response.put("appointments",
                toDTOs(appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(name, patientId, status)));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            Patient patient = patientRepository.findByEmail(tokenService.extractIdentifier(token));
            if (patient == null) {
                response.put("error", "Patient not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            response.put("patient", patient);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.put("error", "Some internal error occurred");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Integer statusFor(String condition) {
        if (condition.equalsIgnoreCase("past")) return 1;
        if (condition.equalsIgnoreCase("future")) return 0;
        return null;
    }

    private List<AppointmentDTO> toDTOs(List<Appointment> appointments) {
        return appointments.stream()
                .map(a -> new AppointmentDTO(
                        a.getId(),
                        a.getDoctor().getId(),
                        a.getDoctor().getName(),
                        a.getPatient().getId(),
                        a.getPatient().getName(),
                        a.getPatient().getEmail(),
                        a.getPatient().getPhone(),
                        a.getPatient().getAddress(),
                        a.getAppointmentTime(),
                        a.getStatus()))
                .collect(Collectors.toList());
    }
}
