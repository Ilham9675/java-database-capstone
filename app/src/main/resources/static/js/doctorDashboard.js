// doctorDashboard.js
import { getAllAppointments } from "./services/appointmentRecordService.js";
import { createPatientRow } from "./components/patientRows.js";

const tableBody = document.getElementById("patientTableBody");
let selectedDate = getToday();
const token = localStorage.getItem("token");
let patientName = null;

// Today's date in YYYY-MM-DD using local time
function getToday() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${now.getFullYear()}-${month}-${day}`;
}

document.getElementById("searchBar").addEventListener("input", (e) => {
  const value = e.target.value.trim();
  // The backend expects the string "null" when there is no name filter
  patientName = value.length > 0 ? value : "null";
  loadAppointments();
});

document.getElementById("todayButton").addEventListener("click", () => {
  selectedDate = getToday();
  document.getElementById("datePicker").value = selectedDate;
  loadAppointments();
});

document.getElementById("datePicker").addEventListener("change", (e) => {
  selectedDate = e.target.value;
  loadAppointments();
});

async function loadAppointments() {
  try {
    const data = await getAllAppointments(selectedDate, patientName, token);
    const appointments = data.appointments || [];

    tableBody.innerHTML = "";

    if (appointments.length === 0) {
      tableBody.innerHTML = `<tr><td colspan="5" class="noPatientRecord">No Appointments found for today.</td></tr>`;
      return;
    }

    appointments.forEach(appointment => {
      // Support both a nested patient object and flattened DTO fields
      const patient = {
        id: appointment.patient?.id ?? appointment.patientId,
        name: appointment.patient?.name ?? appointment.patientName,
        phone: appointment.patient?.phone ?? appointment.patientPhone,
        email: appointment.patient?.email ?? appointment.patientEmail,
      };
      const doctorId = appointment.doctor?.id ?? appointment.doctorId;
      tableBody.appendChild(createPatientRow(patient, appointment.id, doctorId));
    });
  } catch (error) {
    console.error("Error loading appointments:", error);
    tableBody.innerHTML = `<tr><td colspan="5" class="noPatientRecord">Error loading appointments. Try again later.</td></tr>`;
  }
}

document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("datePicker").value = selectedDate;
  loadAppointments();
});
