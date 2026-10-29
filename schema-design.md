# Smart Clinic Management System – Schema Design

This document describes the data design for the Smart Clinic Management System.
Core operational data that is structured and related is stored in **MySQL**.
Flexible data that may change shape over time is stored in **MongoDB**.

---

## MySQL Database Design

> Table names follow the JPA entity names used later in the Spring Boot app (`Patient`, `Doctor`, `Appointment`, `Admin`).
> Email and phone formats are validated in the application code (Bean Validation, e.g. `@Email`, `@Pattern`), and the database enforces `NOT NULL` / `UNIQUE`.

### Table: admin
- id: BIGINT, Primary Key, Auto Increment
- username: VARCHAR(50), Not Null, Unique
- password: VARCHAR(255), Not Null  <!-- stored as a hash, never plain text -->

### Table: doctor
- id: BIGINT, Primary Key, Auto Increment
- name: VARCHAR(100), Not Null
- specialty: VARCHAR(50), Not Null
- email: VARCHAR(100), Not Null, Unique
- password: VARCHAR(255), Not Null
- phone: VARCHAR(10), Not Null  <!-- 10 digits, validated in code -->

### Table: doctor_available_times
- doctor_id: BIGINT, Foreign Key → doctor(id), On Delete Cascade
- available_times: VARCHAR(20), Not Null  <!-- e.g. "09:00-10:00" -->

<!-- Each doctor has their own list of available hourly slots.
     This is a separate table because one doctor has many slots (one-to-many). -->

### Table: patient
- id: BIGINT, Primary Key, Auto Increment
- name: VARCHAR(100), Not Null
- email: VARCHAR(100), Not Null, Unique
- password: VARCHAR(255), Not Null
- phone: VARCHAR(10), Not Null
- address: VARCHAR(255), Not Null
- date_of_birth: DATE, Null

### Table: appointment
- id: BIGINT, Primary Key, Auto Increment
- doctor_id: BIGINT, Foreign Key → doctor(id), Not Null
- patient_id: BIGINT, Foreign Key → patient(id), Not Null
- appointment_time: DATETIME, Not Null  <!-- start time; every appointment lasts 1 hour -->
- status: INT, Not Null, Default 0 (0 = Scheduled, 1 = Completed, 2 = Cancelled)
- Unique constraint: (doctor_id, appointment_time)

<!-- Design decisions:
     - The UNIQUE (doctor_id, appointment_time) constraint stops a doctor from having two
       appointments in the same slot. The service layer also checks for overlaps before saving.
     - Appointment history is kept as medical records. Instead of deleting appointments,
       their status is set to Cancelled or Completed.
     - When a patient or doctor is deleted, their appointments are deleted too (ON DELETE CASCADE),
       so no appointment is left without a patient or doctor. In production a soft delete
       (an "active" flag) would be safer, to keep the history. -->

### Table: clinic_location (optional)
- id: BIGINT, Primary Key, Auto Increment
- name: VARCHAR(100), Not Null
- address: VARCHAR(255), Not Null
- phone: VARCHAR(10), Null

### Table: payment (optional)
- id: BIGINT, Primary Key, Auto Increment
- appointment_id: BIGINT, Foreign Key → appointment(id), Unique
- amount: DECIMAL(10,2), Not Null
- method: VARCHAR(20), Not Null  <!-- CASH, CARD, INSURANCE -->
- status: INT, Not Null (0 = Pending, 1 = Paid, 2 = Refunded)
- paid_at: DATETIME, Null

### Relationships Summary
- One **doctor** has many **appointments** (1 : N)
- One **patient** has many **appointments** (1 : N)
- One **doctor** has many **available times** (1 : N)
- One **appointment** has at most one **payment** (1 : 1)

---

## MongoDB Collection Design

> MongoDB is used for data whose structure varies or may change often. Prescriptions can contain
> several medicines, optional notes, and pharmacy information, which fits a document model better
> than rigid tables.

### Collection: prescriptions
```json
{
  "_id": "ObjectId('64abc123456def7890123456')",
  "patientName": "John Smith",
  "appointmentId": 51,
  "medication": "Paracetamol",
  "dosage": "500mg",
  "doctorNotes": "Take 1 tablet every 6 hours after meals.",
  "refillCount": 2,
  "pharmacy": {
    "name": "Walgreens SF",
    "location": "Market Street"
  },
  "medications": [
    { "name": "Paracetamol", "dosage": "500mg", "frequency": "every 6 hours", "durationDays": 5 },
    { "name": "Vitamin C", "dosage": "1000mg", "frequency": "once a day", "durationDays": 14 }
  ],
  "tags": ["fever", "flu"],
  "metadata": {
    "createdBy": "doctor:12",
    "createdAt": "2026-10-29T10:15:00Z",
    "version": 1
  }
}
```

<!-- Design decisions:
     - The prescription stores only `appointmentId` (a reference to MySQL), not the full patient
       or doctor object, so data is not duplicated and cannot get out of sync. `patientName` is
       copied in only for quick display.
     - A prescription is tied to an appointment, because a doctor writes it during a consultation.
     - New fields (e.g. `medications`, `tags`) can be added later without a migration, and older
       documents still work. The `metadata.version` field shows which document format is used. -->

### Collection: feedback (optional)
```json
{
  "_id": "ObjectId('64abc999888777666555444')",
  "appointmentId": 51,
  "patientId": 7,
  "doctorId": 12,
  "rating": 5,
  "comment": "The doctor explained everything clearly.",
  "anonymous": false,
  "createdAt": "2026-10-30T08:00:00Z"
}
```

### Collection: messages (optional – chat between patient and doctor)
```json
{
  "_id": "ObjectId('64abc111222333444555666')",
  "conversationId": "p7-d12",
  "participants": { "patientId": 7, "doctorId": 12 },
  "messages": [
    { "from": "patient", "text": "Can I take this medicine with coffee?", "sentAt": "2026-10-29T12:00:00Z" },
    { "from": "doctor", "text": "Better to wait 1 hour after taking it.", "sentAt": "2026-10-29T12:05:00Z", "attachments": [] }
  ]
}
```
