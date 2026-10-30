package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService;
    private final Service service;

    // @Lazy breaks the AppointmentService <-> Service dependency cycle
    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              TokenService tokenService,
                              @Lazy Service service) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
        this.service = service;
    }

    // Returns 1 on success, 0 on error
    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            // Attach the managed doctor and patient so the saved record (and response) is complete
            doctorRepository.findById(appointment.getDoctor().getId()).ifPresent(appointment::setDoctor);
            patientRepository.findById(appointment.getPatient().getId()).ifPresent(appointment::setPatient);
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        Map<String, String> response = new HashMap<>();

        Optional<Appointment> existing = appointmentRepository.findById(appointment.getId());
        if (existing.isEmpty()) {
            response.put("message", "Appointment not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        int validation = service.validateAppointment(appointment);
        if (validation == -1) {
            response.put("message", "Invalid doctor id");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if (validation == 0) {
            response.put("message", "Appointment already booked for given time or doctor not available");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        try {
            Appointment toUpdate = existing.get();
            doctorRepository.findById(appointment.getDoctor().getId()).ifPresent(toUpdate::setDoctor);
            toUpdate.setAppointmentTime(appointment.getAppointmentTime());
            toUpdate.setStatus(appointment.getStatus());
            appointmentRepository.save(toUpdate);
            response.put("message", "Appointment updated successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.put("message", "Some internal error occurred");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Only the patient who booked the appointment may cancel it
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Map<String, String> response = new HashMap<>();

        Optional<Appointment> existing = appointmentRepository.findById(id);
        if (existing.isEmpty()) {
            response.put("message", "Appointment not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        Appointment appointment = existing.get();
        Patient patient = patientRepository.findByEmail(tokenService.extractIdentifier(token));
        if (patient == null || !patient.getId().equals(appointment.getPatient().getId())) {
            response.put("message", "Unauthorized to cancel this appointment");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        try {
            appointmentRepository.delete(appointment);
            response.put("message", "Appointment cancelled successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.put("message", "Some internal error occurred");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Appointments of the logged-in doctor on a date, optionally filtered by patient name ("null" = no filter)
    @Transactional(readOnly = true)
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        Map<String, Object> result = new HashMap<>();

        Doctor doctor = doctorRepository.findByEmail(tokenService.extractIdentifier(token));
        if (doctor == null) {
            result.put("appointments", List.of());
            return result;
        }

        List<Appointment> appointments;
        if (pname == null || pname.isBlank() || pname.equalsIgnoreCase("null")) {
            appointments = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                    doctor.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        } else {
            appointments = appointmentRepository.findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                    doctor.getId(), pname, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        }

        result.put("appointments", appointments);
        return result;
    }
}
