# User Stories – Smart Clinic Management System

## User Story Template

**Title:**
_As a [user role], I want [feature/goal], so that [reason]._

**Acceptance Criteria:**
1. [Criteria 1]
2. [Criteria 2]
3. [Criteria 3]

**Priority:** [High/Medium/Low]
**Story Points:** [Estimated Effort in Points]
**Notes:**
- [Additional information or edge cases]

---

## Admin User Stories

### A1. Admin login to the portal

**Title:**
_As an admin, I want to log into the portal with my username and password, so that I can manage the platform securely._

**Acceptance Criteria:**
1. The admin can enter a username and password on the admin login page.
2. Valid credentials redirect the admin to the admin dashboard.
3. Invalid credentials show a clear error message and do not grant access.

**Priority:** High
**Story Points:** 3
**Notes:**
- Passwords must never be stored in plain text.
- A token (JWT) is issued on successful login and used for later requests.

### A2. Admin logout from the portal

**Title:**
_As an admin, I want to log out of the portal, so that I can protect system access when I am done._

**Acceptance Criteria:**
1. A logout button is visible on every admin page.
2. Clicking logout clears the session/token.
3. The admin is redirected to the login page and cannot reach admin pages without logging in again.

**Priority:** High
**Story Points:** 1
**Notes:**
- Using the browser back button after logout must not show protected data.

### A3. Admin adds a doctor to the portal

**Title:**
_As an admin, I want to add doctors to the portal, so that patients can find them and book appointments._

**Acceptance Criteria:**
1. The admin can open an "Add Doctor" form from the dashboard.
2. The form requires name, specialty, email, password, phone, and available times.
3. After saving, the new doctor appears in the doctor list.

**Priority:** High
**Story Points:** 3
**Notes:**
- A doctor with an email that already exists must be rejected with an error message.

### A4. Admin deletes a doctor's profile

**Title:**
_As an admin, I want to delete a doctor's profile from the portal, so that the doctor list stays accurate._

**Acceptance Criteria:**
1. Each doctor card/row has a delete option for the admin.
2. The admin is asked to confirm before the doctor is deleted.
3. After deletion, the doctor no longer appears in the list or in patient searches.

**Priority:** Medium
**Story Points:** 2
**Notes:**
- Decide how existing appointments of the deleted doctor are handled (e.g. removed or cancelled).

### A5. Admin runs a stored procedure to get monthly appointment statistics

**Title:**
_As an admin, I want to run a stored procedure in the MySQL CLI to get the number of appointments per month, so that I can track usage statistics._

**Acceptance Criteria:**
1. A stored procedure exists in the MySQL database for appointment statistics.
2. The admin can execute it from the MySQL CLI with a single CALL statement.
3. The result shows the number of appointments grouped by month.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Related procedures: GetDailyAppointmentReportByDoctor, GetDoctorWithMostPatientsByMonth, GetDoctorWithMostPatientsByYear.

---

## Patient User Stories

### P1. Patient views the doctor list without logging in

**Title:**
_As a patient, I want to view a list of doctors without logging in, so that I can explore options before registering._

**Acceptance Criteria:**
1. The doctor list is visible on the public landing page without login.
2. Each doctor shows name, specialty, and available times.
3. The patient can search or filter doctors by name and specialty.

**Priority:** High
**Story Points:** 2
**Notes:**
- Booking an appointment from this page must ask the patient to log in first.

### P2. Patient signs up with email and password

**Title:**
_As a patient, I want to sign up using my email and password, so that I can book appointments._

**Acceptance Criteria:**
1. The sign-up form asks for name, email, password, phone, and address.
2. Required fields and email format are validated before submission.
3. A successful sign-up creates the account and lets the patient log in.

**Priority:** High
**Story Points:** 3
**Notes:**
- Signing up with an email that is already registered must show an error.

### P3. Patient logs into the portal

**Title:**
_As a patient, I want to log into the portal, so that I can manage my bookings._

**Acceptance Criteria:**
1. The patient can log in with email and password.
2. Valid credentials open the patient dashboard.
3. Invalid credentials show an error message.

**Priority:** High
**Story Points:** 2
**Notes:**
- The login token is stored on the client and sent with later requests.

### P4. Patient logs out of the portal

**Title:**
_As a patient, I want to log out of the portal, so that my account stays secure._

**Acceptance Criteria:**
1. A logout button is visible on patient pages.
2. Logging out removes the stored token.
3. The patient is returned to the home page and cannot open patient pages without logging in.

**Priority:** High
**Story Points:** 1
**Notes:**
- Important on shared or public computers.

### P5. Patient books an hour-long appointment

**Title:**
_As a patient, I want to log in and book an hour-long appointment, so that I can consult with a doctor._

**Acceptance Criteria:**
1. The patient can choose a doctor, a date, and an available time slot.
2. Each appointment lasts one hour.
3. A confirmation is shown after booking, and the slot is no longer available to others.

**Priority:** High
**Story Points:** 5
**Notes:**
- Booking a slot that is already taken or marked unavailable must be rejected.

### P6. Patient views upcoming appointments

**Title:**
_As a patient, I want to view my upcoming appointments, so that I can prepare accordingly._

**Acceptance Criteria:**
1. The patient dashboard lists upcoming appointments.
2. Each appointment shows doctor name, date, time, and status.
3. Appointments are sorted by date, nearest first.

**Priority:** Medium
**Story Points:** 2
**Notes:**
- Past appointments can be shown separately as history.

---

## Doctor User Stories

### D1. Doctor logs into the portal

**Title:**
_As a doctor, I want to log into the portal, so that I can manage my appointments._

**Acceptance Criteria:**
1. The doctor can log in with email and password.
2. Valid credentials open the doctor dashboard.
3. Invalid credentials show an error message.

**Priority:** High
**Story Points:** 2
**Notes:**
- Access is limited to the doctor's own data.

### D2. Doctor logs out of the portal

**Title:**
_As a doctor, I want to log out of the portal, so that I can protect my data._

**Acceptance Criteria:**
1. A logout button is visible on doctor pages.
2. Logging out clears the token/session.
3. Doctor pages cannot be opened again without logging in.

**Priority:** High
**Story Points:** 1
**Notes:**
- Patient data must not be visible after logout.

### D3. Doctor views appointment calendar

**Title:**
_As a doctor, I want to view my appointment calendar, so that I can stay organized._

**Acceptance Criteria:**
1. The doctor dashboard shows all appointments for the selected date.
2. The doctor can change the date to see other days.
3. Each appointment shows the patient name, time, and status.

**Priority:** High
**Story Points:** 3
**Notes:**
- Default view shows today's appointments.

### D4. Doctor marks unavailability

**Title:**
_As a doctor, I want to mark my unavailability, so that patients only see my available slots._

**Acceptance Criteria:**
1. The doctor can mark specific dates or time slots as unavailable.
2. Unavailable slots are hidden from patients when booking.
3. The doctor can remove the unavailability later.

**Priority:** High
**Story Points:** 3
**Notes:**
- Slots that already have a booking need a warning before being marked unavailable.

### D5. Doctor updates profile with specialization and contact info

**Title:**
_As a doctor, I want to update my profile with my specialization and contact information, so that patients have up-to-date information._

**Acceptance Criteria:**
1. The doctor can edit specialization, phone number, and email.
2. Input is validated (e.g. valid email and phone format).
3. Changes are saved and shown immediately to patients.

**Priority:** Medium
**Story Points:** 2
**Notes:**
- Email must remain unique across doctors.

### D6. Doctor views patient details for upcoming appointments

**Title:**
_As a doctor, I want to view the patient details for upcoming appointments, so that I can be prepared._

**Acceptance Criteria:**
1. The doctor can open an appointment to see the patient's name, contact, and details.
2. The doctor can see the patient's previous prescriptions, if any.
3. Only doctors with an appointment with that patient can see these details.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Patient data is sensitive and must stay private.
