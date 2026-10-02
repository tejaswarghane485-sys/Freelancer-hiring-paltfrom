# FreelanceHub — Freelancer Hiring Platform

A full-stack web application connecting **Clients** and **Freelancers**.

## Tech Stack

| Layer       | Technology                          |
|-------------|--------------------------------------|
| Frontend    | HTML5, CSS3, Vanilla JavaScript       |
| Backend     | Java 17, Spring Boot 3.2, Spring Security |
| Database    | MySQL 8                              |
| Auth        | JWT (JSON Web Tokens) + BCrypt       |
| Build       | Maven                                |

---

## Project Structure

```
Freelancer hiring platform/
├── backend/                     ← Spring Boot application
│   ├── pom.xml
│   └── src/main/java/com/freelancer/
│       ├── config/              ← SecurityConfig, JacksonConfig
│       ├── controller/          ← REST controllers
│       ├── dto/                 ← Request/Response DTOs
│       ├── exception/           ← GlobalExceptionHandler
│       ├── model/               ← JPA Entities
│       ├── repository/          ← Spring Data JPA repos
│       ├── security/            ← JwtUtil, JwtAuthFilter
│       └── service/             ← Business logic
│
├── database/
│   └── schema.sql               ← MySQL schema + sample data
│
└── frontend/
    ├── index.html               ← Landing page
    ├── pages/
    │   ├── login.html
    │   ├── signup.html
    │   ├── client-dashboard.html
    │   └── freelancer-dashboard.html
    ├── css/
    │   ├── style.css            ← Global styles
    │   ├── landing.css          ← Landing page
    │   ├── auth.css             ← Login/Signup
    │   └── dashboard.css        ← Dashboard layout
    └── js/
        ├── api.js               ← All API calls
        ├── auth.js              ← Login/Register logic
        ├── dashboard.js         ← Shared utilities & nav
        ├── client.js            ← Client dashboard logic
        └── freelancer.js        ← Freelancer dashboard logic
```

---

## Setup & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8+
- A browser (Chrome/Firefox/Edge)

---

### Step 1 — Database

1. Open MySQL Workbench or your MySQL client
2. Run the schema file:
   ```sql
   source path/to/database/schema.sql
   ```
   Or import it via MySQL Workbench: **Server → Data Import → Import from Self-Contained File**.
   The schema creates empty tables; accounts and projects are created through the application.

---

### Step 2 — Backend Configuration

Open `backend/src/main/resources/application.properties` and update:

```properties
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

If your MySQL runs on a different port, also update:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/freelancer_platform?...
```

---

### Step 3 — Run the Backend

```bash
cd backend
mvn spring-boot:run
```

The API will be available at: **http://localhost:8080**

You should see log output like:
```
Started FreelancerPlatformApplication in 3.x seconds
```

---

### Step 4 — Run the Frontend

**VS Code Live Server**
1. Install the [Live Server extension](https://marketplace.visualstudio.com/items?itemName=ritwickdey.LiveServer)
2. Right-click `frontend/index.html` → **Open with Live Server**
3. Open `http://127.0.0.1:5500` — this origin is allowed by the backend CORS configuration.
4. Do not open the HTML files with `file://`; the frontend must be served over HTTP to call the backend.

---

Create an account using **Sign Up** before logging in. No sample accounts or test records are preloaded.

---

## API Endpoints

### Auth (Public)
| Method | Endpoint             | Description       |
|--------|----------------------|-------------------|
| POST   | /api/auth/register   | Register new user |
| POST   | /api/auth/login      | Login             |

### Projects
| Method | Endpoint                   | Description              |
|--------|----------------------------|--------------------------|
| GET    | /api/projects/open         | All open projects (public)|
| GET    | /api/projects/my           | Client's own projects     |
| POST   | /api/projects              | Create project            |
| PUT    | /api/projects/{id}         | Update project            |
| PATCH  | /api/projects/{id}/status  | Change project status     |
| DELETE | /api/projects/{id}         | Delete project            |
| GET    | /api/projects/search       | Search by keyword/skill   |

### Freelancers
| Method | Endpoint                   | Description              |
|--------|----------------------------|--------------------------|
| GET    | /api/freelancers           | All freelancer profiles  |
| GET    | /api/freelancers/{id}      | Profile by profile ID    |
| GET    | /api/freelancers/user/{uid}| Profile by user ID       |
| POST   | /api/freelancers/profile   | Create/update own profile|
| GET    | /api/freelancers/search    | Search by skill/name     |

### Proposals
| Method | Endpoint                      | Description             |
|--------|-------------------------------|-------------------------|
| POST   | /api/proposals                | Submit proposal         |
| GET    | /api/proposals/my             | My submitted proposals  |
| GET    | /api/proposals/project/{id}   | Proposals for a project |
| PATCH  | /api/proposals/{id}/status    | Accept/Reject proposal  |
| DELETE | /api/proposals/{id}           | Withdraw proposal       |

### Hiring
| Method | Endpoint                   | Description               |
|--------|----------------------------|---------------------------|
| POST   | /api/hiring                | Send hiring request       |
| PATCH  | /api/hiring/{id}/respond   | Accept/Reject request     |
| GET    | /api/hiring/sent           | Requests sent by client   |
| GET    | /api/hiring/received       | Requests received by freelancer |

---

## Features

**Client**
- Post, edit, delete, and manage project status
- Browse and search freelancer profiles by skill/name
- View proposals for each project, accept or reject
- Send hiring requests to freelancers

**Freelancer**
- Create and update professional profile (bio, skills, experience, education, portfolio, hourly rate)
- Browse and search open projects by keyword/skill
- Submit proposals with cover letter, bid amount, and timeline
- Accept or reject incoming hiring requests
- Track active projects
