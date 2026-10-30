package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Shared service for authentication, validation and filtering used by several controllers
@org.springframework.stereotype.Service
public class Service {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public Service(TokenService tokenService,
                   AdminRepository adminRepository,
                   DoctorRepository doctorRepository,
                   PatientRepository patientRepository,
                   AppointmentRepository appointmentRepository,
                   DoctorService doctorService,
                   PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    // Empty body = valid token; otherwise 401 with an error message
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        Map<String, String> errors = new HashMap<>();
        if (!tokenService.validateToken(token, user)) {
            errors.put("error", "Invalid or expired token");
            return new ResponseEntity<>(errors, HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(errors, HttpStatus.OK);
    }

    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> response = new HashMap<>();
        try {
            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (admin == null || !admin.getPassword().equals(receivedAdmin.getPassword())) {
                response.put("error", "Invalid username or password");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            response.put("token", tokenService.generateToken(admin.getUsername()));
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.put("error", "Some internal error occurred");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Any filter may be "null" (as sent by the frontend); with no filters all doctors are returned
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        boolean hasName = isSet(name);
        boolean hasSpecialty = isSet(specialty);
        boolean hasTime = isSet(time);

        if (hasName && hasSpecialty && hasTime) {
            return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
        }
        if (hasName && hasTime) {
            return doctorService.filterDoctorByNameAndTime(name, time);
        }
        if (hasName && hasSpecialty) {
            return doctorService.filterDoctorByNameAndSpecility(name, specialty);
        }
        if (hasSpecialty && hasTime) {
            return doctorService.filterDoctorByTimeAndSpecility(specialty, time);
        }
        if (hasName) {
            return doctorService.findDoctorByName(name);
        }
        if (hasSpecialty) {
            return doctorService.filterDoctorBySpecility(specialty);
        }
        if (hasTime) {
            return doctorService.filterDoctorsByTime(time);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("doctors", doctorService.getDoctors());
        return result;
    }

    // 1 = slot available, 0 = not available, -1 = doctor not found
    @Transactional(readOnly = true)
    public int validateAppointment(Appointment appointment) {
        if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null
                || appointment.getAppointmentTime() == null) {
            return -1;
        }
        Doctor doctor = doctorRepository.findById(appointment.getDoctor().getId()).orElse(null);
        if (doctor == null) {
            return -1;
        }

        LocalDate date = appointment.getAppointmentTime().toLocalDate();
        String requestedStart = appointment.getAppointmentTime().toLocalTime().format(HH_MM);

        boolean doctorWorksThen = doctor.getAvailableTimes() != null && doctor.getAvailableTimes().stream()
                .anyMatch(slot -> slot.split("-")[0].trim().equals(requestedStart));
        if (!doctorWorksThen) {
            return 0;
        }

        // The slot is taken if another appointment (not the one being updated) starts at the same time
        List<Appointment> sameDay = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                doctor.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        boolean taken = sameDay.stream()
                .filter(a -> appointment.getId() == null || !a.getId().equals(appointment.getId()))
                .anyMatch(a -> a.getAppointmentTime().toLocalTime().format(HH_MM).equals(requestedStart));

        return taken ? 0 : 1;
    }

    // true = no patient with this email or phone yet
    public boolean validatePatient(Patient patient) {
        return patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone()) == null;
    }

    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> response = new HashMap<>();
        try {
            Patient patient = patientRepository.findByEmail(login.getIdentifier());
            if (patient == null || !patient.getPassword().equals(login.getPassword())) {
                response.put("error", "Invalid email or password");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            response.put("token", tokenService.generateToken(patient.getEmail()));
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.put("error", "Some internal error occurred");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // condition: "past"/"future"/"null"; name: doctor name or "null"
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        Patient patient = patientRepository.findByEmail(tokenService.extractIdentifier(token));
        if (patient == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Patient not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        boolean hasCondition = isSet(condition);
        boolean hasName = isSet(name);

        if (hasCondition && hasName) {
            return patientService.filterByDoctorAndCondition(condition, name, patient.getId());
        }
        if (hasCondition) {
            return patientService.filterByCondition(condition, patient.getId());
        }
        if (hasName) {
            return patientService.filterByDoctor(name, patient.getId());
        }
        return patientService.getPatientAppointment(patient.getId(), token);
    }

    private boolean isSet(String value) {
        return value != null && !value.isBlank() && !value.equalsIgnoreCase("null");
    }
}
