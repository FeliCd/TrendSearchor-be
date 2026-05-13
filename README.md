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
* **Synchronization Frequency:** Data updates are performed on a schedul# Scientific Journal Publication Trend Tracking System

## Overview

With the ever-increasing volume of scientific papers and academic journals, tracking research trends, prominent topics, and the evolution of academic disciplines has become a significant challenge for lecturers, students, and researchers. Current academic platforms primarily facilitate paper retrieval but lack robust tools for analyzing publication trends over time and visualizing research data.

### Problem Statement

* **Trend Tracking Difficulty:** The massive influx of scientific papers makes it hard to monitor the shifts and advancements in research topics over time.
* **Limited Analytical Tools:** Existing academic search platforms rely heavily on keyword matching and do not adequately support visual research trend analysis.
* **Time Consumption:** Lecturers, students, and researchers expend considerable time identifying emerging, prominent, or high-potential research areas.


## Setup and Configuration

This project requires environment variables for database configuration.

1. Copy the `.env.example` file and rename it to `.env` in the project root directory.
2. Update the `.env` file with your actual MySQL credentials.

Example `MYSQL_URL` format for remote databases:
`jdbc:mysql://<PUBLIC_HOST>:<PUBLIC_PORT>/<DATABASE_NAME>`
*Note: Ensure the URL does not contain the username and password inline.*

## Running the Application

### Using Maven Wrapper

**Windows:**
```cmd
mvnw.cmd spring-boot:run
```
ed, periodic basis (e.g., daily or weekly); real-time synchronization is not required.

## Technologies Used

* Java 17
* Spring Boot
* MySQL
* Maven
* Lombok

## Setup and Configuration

This project requires environment variables for database configuration.

1. Copy the `.env.example` file and rename it to `.env` in the project root directory.
2. Update the `.env` file with your actual MySQL credentials.

Example `MYSQL_URL` format for remote databases:
`jdbc:mysql://<PUBLIC_HOST>:<PUBLIC_PORT>/<DATABASE_NAME>`
*Note: Ensure the URL does not contain the username and password inline.*

## Running the Application

### Using Maven Wrapper

**Windows:**
```cmd
mvnw.cmd spring-boot:run
```

**macOS/Linux:**
```bash
./mvnw spring-boot:run
```
