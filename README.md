# Scientific Journal Publication Trend Tracking System

## Overview

With the ever-increasing volume of scientific papers and academic journals, tracking research trends, prominent topics, and the evolution of academic disciplines has become a significant challenge for lecturers, students, and researchers. Current academic platforms primarily facilitate paper retrieval but lack robust tools for analyzing publication trends over time and visualizing research data.

### Problem Statement

* **Trend Tracking Difficulty:** The massive influx of scientific papers makes it hard to monitor the shifts and advancements in research topics over time.
* **Limited Analytical Tools:** Existing academic search platforms rely heavily on keyword matching and do not adequately support visual research trend analysis.
* **Time Consumption:** Lecturers, students, and researchers expend considerable time identifying emerging, prominent, or high-potential research areas.

## Target Audience

* **Researcher:** Analyzes deep research trends, tracks specific journals and keywords, discovers emerging topics, and views temporal publication statistics.
* **Lecturer / Student:** Searches for reference materials, explores popular subjects, bookmarks papers or keywords of interest, and views basic trend dashboards.
* **System Administrator:** Manages user accounts, configures API data sources, oversees paper data synchronization, and maintains system operations.

## Key Features

* User authentication and authorization
* Search research papers by keyword, author, or journal
* View comprehensive paper details and publication metadata
* Track publication trends based on keywords or specific research topics
* Display interactive charts and dashboard statistics
* Identify and view trending research topics
* Bookmark functionality for papers and keywords
* Follow capabilities for journals and research topics
* Automated notifications for newly published papers
* Generate simple analytical reports
* Periodic data synchronization from external academic APIs
* System and user management configuration for administrators

## Core Entities

* User
* Research Paper
* Journal
* Keyword
* Research Topic
* Publication Trend
* Author
* Bookmark
* Notification
* Dashboard Report
* API Data Source

## Technical Constraints and Assumptions

* **Data Sources:** The system utilizes public metadata from academic repositories such as Semantic Scholar, OpenAlex, or Crossref via free APIs.
* **Metadata Only:** To comply with copyright restrictions and manage storage limits, the system only collects metadata (title, abstract, keywords, publication year, authors, and journal), excluding full-text processing.
* **Data Reliability:** Third-party API data is assumed to be valid, uniformly structured, and consistently available.
* **Domain Scope:** To manage complexity, trend analysis is restricted to pre-selected academic domains (e.g., Computer Science or Artificial Intelligence).
* **Synchronization Frequency:** Data updates are performed on a scheduled, periodic basis (e.g., daily or weekly); real-time synchronization is not required.

## Technologies Used

* Java 21
* Spring Boot 3
* MySQL 8
* Maven
* Docker
* Flyway (database migrations)
* Lombok

---

## Prerequisites

Before running this project, ensure you have the following installed:

| Tool | Version | Notes |
|------|---------|-------|
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | Latest | Required for local MySQL |
| [Java JDK](https://adoptium.net/) | 21 | Required to run the application |
| [Maven](https://maven.apache.org/) | 3.9+ | Or use the included `mvnw` wrapper |

---

## Setup Guide

### Step 1: Setup MySQL with Docker

1. **Start Docker Desktop** on your machine.

2. **Create and start a MySQL container:**
   ```bash
   docker run -d --name trendsearchor_mysql \
     -p 3306:3306 \
     -e MYSQL_ROOT_PASSWORD=your_password \
     -e MYSQL_DATABASE=trendsearchor \
     mysql:8
   ```
   > Replace `your_password` with a secure password of your choice.

3. **Verify MySQL is running:**
   ```bash
   docker ps
   ```
   You should see `trendsearchor_mysql` in the list with status `Up`.

4. **(Optional) Connect to MySQL CLI:**
   ```bash
   docker exec -it trendsearchor_mysql mysql -u root -p
   ```

---

### Step 2: Configure Environment Variables

1. Copy `.env.example` to `.env` in the project root:
   ```bash
   copy .env.example .env
   ```

2. Open `.env` and update the following values:

   ```properties
   MYSQL_USER=root
   MYSQL_PASSWORD=your_password        # Same password as Step 1
   MYSQL_URL=jdbc:mysql://localhost:3306/trendsearchor
   SPRING_PROFILES_ACTIVE=dev
   JWT_SECRET=your_base64_encoded_secret
   ```

   Generate a JWT secret (PowerShell):
   ```powershell
   # Generate a 256-bit random key, base64 encoded
   $bytes = New-Object byte[] 32
   (New-Object System.Security.Cryptography.RNGCryptoServiceProvider).GetBytes($bytes)
   [Convert]::ToBase64String($bytes)
   ```

---

### Step 3: Run Database Migrations

Flyway migrations run automatically on startup. The following migrations will be executed:

| Version | Description |
|---------|-------------|
| V1 | Initial database schema |
| V2 | Notifications and sync log tables |

On first run, you should see logs similar to:
```
Flyway Community Edition 9.22.3 by Redgate
Database: jdbc:mysql://localhost:3306/trendsearchor (MySQL 8.x)
Successfully validated 2 migrations
Migrating schema "trendsearchor" to version "1 - init schema"
Migrating schema "trendsearchor" to version "2 - add notifications and synclog"
Successfully applied 2 migrations to schema "trendsearchor"
```

---

### Step 4: Run the Application

Using Maven wrapper (recommended):

**Windows:**
```bash
mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev"
```

**macOS / Linux:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Or with Maven installed globally:
```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev"
```

The application will start on **http://localhost:8080**

---

### Step 5: Access the Application

| Service | URL |
|---------|-----|
| API Base URL | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Default Admin | Username: `admin`, Password: `admin123` |

---

## Useful Docker Commands

```bash
# Stop MySQL container
docker stop trendsearchor_mysql

# Start MySQL container again
docker start trendsearchor_mysql

# Remove MySQL container (WARNING: deletes all data)
docker rm -f trendsearchor_mysql

# View MySQL logs
docker logs trendsearchor_mysql

# Reset database (delete container + start fresh)
docker stop trendsearchor_mysql
docker rm trendsearchor_mysql
# Then run the docker run command from Step 1 again
```

---

## Troubleshooting

### Port 8080 already in use
```bash
# Find and kill the process on port 8080
netstat -ano | findstr :8080
taskkill /PID <PROCESS_ID> /F
```

### MySQL connection refused
* Ensure Docker Desktop is running.
* Verify the container is up: `docker ps`
* Check the port mapping: `docker port trendsearchor_mysql`

### Flyway migration failures
* Ensure the `trendsearchor` database exists inside the container.
* To recreate the database:
  ```bash
  docker exec -it trendsearchor_mysql mysql -u root -p -e "DROP DATABASE IF EXISTS trendsearchor; CREATE DATABASE trendsearchor;"
  ```
  Then restart the application.
