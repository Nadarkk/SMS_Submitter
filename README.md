[README.md](https://github.com/user-attachments/files/31890461/README.md)
# SMS Submitter

A Spring Boot + Thymeleaf web application for managing SMS campaigns across multiple companies — contacts, message templates, message composition, and delivery tracking through an integrated SMS simulator.

Built as part of an internship project at Edafa.

## Features

- **Multi-company, multi-role access** — `ADMIN`, `MANAGER`, and `USER` roles, with company-scoped data access enforced at the service layer (not just the UI)
- **Contact management** — add, edit, and organize recipient contacts per company
- **Message templates** — reusable message bodies for common campaigns
- **Message composition & sending** — select contacts, write or load a template, and send in bulk
- **SMS delivery tracking** — messages are submitted to a Web2SMS simulator and polled asynchronously for final delivery status (`PENDING` → `SUBMITTED` →`DELIVERED` / `FAILED`)
- **Dashboard** — KPI cards (total messages, recipients, contacts, templates, delivery rate) and a delivery success/failure chart
- **CSV reports** — export daily SMS stats (messages sent, delivered, failed, delivery rate) for a selected date range and company
- **Session-based authentication** via Spring Security, with JWT-based auth wired in for stateless requests

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot, Spring Security, Spring Data JPA |
| Templating | Thymeleaf (server-rendered, no JavaScript frameworks) |
| Database | MySQL |
| Build | Maven |
| Other | Lombok, Jakarta Bean Validation, RestTemplate, `@Scheduled` polling |

## Architecture Notes

- **Vertical slice development** — each feature (Contacts, Templates, Messages) was built end-to-end before moving to the next.
- **Company scoping is never taken from client input.** All create/read/update/delete operations resolve or verify the acting company through `AccessService`, so a request can't be crafted to access another company's data by editing form/query parameters.
- **Admins operate across companies.** Where an admin performs a company-scoped action without specifying which company, the app redirects to a company-selection screen rather than guessing or defaulting.
- **SMS delivery is asynchronous.** Sending a message immediately creates `PENDING` SMS records and submits them to the simulator; a scheduled background job polls the simulator afterward and updates each record to `DELIVERED` or `FAILED`.
- **No JavaScript dependency for core functionality.** Multi-select, template prefill, and form flows use native HTML/Thymeleaf patterns instead of client-side scripting. Chart.js is used only for the dashboard's optional visual chart.

## Getting Started

### Prerequisites

- Java 21+
- Maven
- MySQL 8+

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/Nadarkk/sms_submitter.git
   cd sms_submitter
   ```

2. Create the database:
   ```sql
   CREATE DATABASE SMS_System;
   ```

3. Copy the example config and fill in your local values:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
   Set your MySQL username/password and a JWT secret key in `application.properties`.

4. Run the schema (see `drawSQL-mysql-export.sql` in the repo) against `SMS_System`.

5. (Optional) Load sample data — see `seed-data.sql` in the repo for a small set of companies, users, contacts, templates, and messages to explore the app with. All seeded users share the password `password123`.

6. Run the application:
   ```bash
   mvn spring-boot:run
   ```

7. Visit `http://localhost:8080` and log in.

## Project Structure

```
src/main/java/com/edafa/sms_submitter/
├── controller/     # MVC controllers (Contact, Template, Message, Dashboard, Report, Auth)
├── service/        # Business logic and company-access enforcement
├── repository/     # Spring Data JPA repositories
├── entity/         # JPA entities (Company, User, Contact, Template, Message, Sms)
├── dto/            # Request/response DTOs with validation
├── mapper/         # Entity <-> DTO mapping
├── security/       # Spring Security config, JWT filter
├── integration/    # Web2SMS simulator client and DTOs
└── exception/      # Custom exceptions and global exception handling

src/main/resources/
├── templates/      # Thymeleaf views
└── application.properties
```

## Roles at a Glance

| Role | Scope |
|---|---|
| **USER** | Compose and send messages within their own company |
| **MANAGER** | Manage contacts, templates, and messages within their own company; export reports |
| **ADMIN** | All of the above across any company, plus company and user management  |


## License

Internal project — not currently licensed for external distribution.
