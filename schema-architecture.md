# Smart Clinic Management System – Architecture

## Section 1: Architecture Summary

The Smart Clinic Management System is a Spring Boot application built on a three-tier architecture: a presentation tier, an application tier, and a data tier. The presentation tier serves two kinds of clients. The Admin and Doctor dashboards are server-rendered HTML pages built with Thymeleaf. The Appointments, Patient Dashboard, and Patient Record modules talk to the backend through REST APIs that exchange JSON, so mobile apps or other frontends can reuse them.

In the application tier, Spring MVC controllers handle the Thymeleaf views and REST controllers handle the API calls. Both kinds of controllers send requests through one shared service layer. That layer holds the business rules, such as validation and checking a doctor's availability before booking an appointment. The service layer then calls the right repositories. The data tier uses two databases. MySQL, accessed through Spring Data JPA, stores structured relational data such as patients, doctors, appointments, and admins as JPA entities. MongoDB, accessed through Spring Data MongoDB, stores prescriptions as flexible document models. Because the layers are separate, the application is easier to maintain, test, containerize with Docker, and deploy through CI/CD pipelines.

## Section 2: Numbered Flow of Data and Control

1. **User Interface Layer:** The user opens a Thymeleaf dashboard (AdminDashboard or DoctorDashboard), or a REST client (Appointments, PatientDashboard, PatientRecord) sends an HTTP request to the backend.
2. **Controller Layer:** Spring routes the request by its URL path and HTTP method. A Thymeleaf controller handles it if the response should be an HTML view, and a REST controller handles it if the response should be JSON. The controller validates the input and coordinates the request.
3. **Service Layer:** The controller passes the work to the service layer. This layer applies the business rules and validations and coordinates workflows that involve several entities, for example checking a doctor's availability before scheduling an appointment.
4. **Repository Layer:** The service layer calls the right repository. A MySQL repository (Spring Data JPA) handles patients, doctors, appointments, and admins. The MongoDB repository (Spring Data MongoDB) handles prescriptions.
5. **Database Access:** Each repository runs its queries against its own database. MySQL holds the normalized relational data with its constraints. MongoDB holds flexible, document-based prescription data.
6. **Model Binding:** The data that comes back is mapped into Java model classes. MySQL rows become `@Entity` JPA classes, and MongoDB documents become `@Document` classes.
7. **Application Models in Use:** The models go back up to the controller. In MVC flows they are added to the model and rendered into dynamic HTML by Thymeleaf. In REST flows they (or DTOs built from them) are serialized to JSON and returned in the HTTP response. This completes the request-response cycle.
