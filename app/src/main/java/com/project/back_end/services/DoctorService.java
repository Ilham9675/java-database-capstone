package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class DoctorService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // Available slots on a date = the doctor's slots minus slots already booked that day
    @Transactional(readOnly = true)
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        if (doctor == null || doctor.getAvailableTimes() == null) {
            return new ArrayList<>();
        }

        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                doctorId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        Set<String> bookedStarts = appointments.stream()
                .map(a -> a.getAppointmentTime().toLocalTime().format(HH_MM))
                .collect(Collectors.toSet());

        return doctor.getAvailableTimes().stream()
                .filter(slot -> !bookedStarts.contains(slotStart(slot)))
                .collect(Collectors.toList());
    }

    // Returns 1 on success, -1 if a doctor with this email exists, 0 on error
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctorRepository.findByEmail(doctor.getEmail()) != null) {
                return -1;
            }
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // Returns 1 on success, -1 if not found, 0 on error
    public int updateDoctor(Doctor doctor) {
        try {
            if (doctor.getId() == null || !doctorRepository.existsById(doctor.getId())) {
                return -1;
            }
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }

    // Returns 1 on success, -1 if not found, 0 on error
    @Transactional
    public int deleteDoctor(long id) {
        try {
            if (!doctorRepository.existsById(id)) {
                return -1;
            }
            appointmentRepository.deleteAllByDoctorId(id);
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        Map<String, String> response = new HashMap<>();
        try {
            Doctor doctor = doctorRepository.findByEmail(login.getIdentifier());
            if (doctor == null || !doctor.getPassword().equals(login.getPassword())) {
                response.put("error", "Invalid email or password");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            response.put("token", tokenService.generateToken(doctor.getEmail()));
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.put("error", "Some internal error occurred");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> findDoctorByName(String name) {
        return doctorsResult(doctorRepository.findByNameLike(name));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);
        return doctorsResult(filterDoctorByTime(doctors, amOrPm));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findByNameLike(name);
        return doctorsResult(filterDoctorByTime(doctors, amOrPm));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specilty) {
        return doctorsResult(doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specilty));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specilty, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specilty);
        return doctorsResult(filterDoctorByTime(doctors, amOrPm));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorBySpecility(String specilty) {
        return doctorsResult(doctorRepository.findBySpecialtyIgnoreCase(specilty));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        return doctorsResult(filterDoctorByTime(doctorRepository.findAll(), amOrPm));
    }

    // Keeps doctors with at least one slot matching the time filter.
    // "AM"/"PM" match by the slot's start hour; any other value (e.g. "09:00-10:00") must match a slot exactly.
    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        return doctors.stream()
                .filter(doctor -> doctor.getAvailableTimes() != null
                        && doctor.getAvailableTimes().stream().anyMatch(slot -> slotMatches(slot, amOrPm)))
                .collect(Collectors.toList());
    }

    private boolean slotMatches(String slot, String time) {
        if (time.equalsIgnoreCase("AM") || time.equalsIgnoreCase("PM")) {
            int startHour = Integer.parseInt(slotStart(slot).split(":")[0]);
            boolean isAm = startHour < 12;
            return time.equalsIgnoreCase("AM") == isAm;
        }
        return slot.replace(" ", "").equalsIgnoreCase(time.replace(" ", ""));
    }

    // "09:00-10:00" -> "09:00"
    private String slotStart(String slot) {
        return slot.split("-")[0].trim();
    }

    private Map<String, Object> doctorsResult(List<Doctor> doctors) {
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", doctors);
        return result;
    }
}
