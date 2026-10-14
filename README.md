# RFID Tag Management Service

A Java Spring Boot microservice for managing RFID tags, developed as part of the Anexya coding challenge and deployed on AWS.

## Tech Stack

- **Backend:** Java 21, Spring Boot 3
- **Database:** PostgreSQL (Amazon RDS)
- **ORM:** Spring Data JPA
- **Build:** Maven
- **Containerization:** Docker
- **AWS:** ECS Fargate, ECR, RDS, CloudWatch
- **API Documentation:** Swagger UI
- **Testing:** JUnit 5, Mockito, Testcontainers

## Project Overview

The application provides REST APIs to create, retrieve, update, and delete RFID tags, as well as track their status and location.

The project follows a layered architecture with controllers, services, and repositories. It includes request validation, global exception handling, and PostgreSQL persistence.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/tags` | Create an RFID tag |
| GET | `/api/v1/tags` | Retrieve all tags (paginated) |
| GET | `/api/v1/tags/{id}` | Retrieve a tag by ID |
| PUT | `/api/v1/tags/{id}` | Update a tag |
| DELETE | `/api/v1/tags/{id}` | Delete a tag |
| GET | `/api/v1/tags/search?status=ACTIVE` | Filter tags by status |

### Swagger UI

The API documentation can be accessed through:

**AWS:** http://rfid-alb-1689557911.eu-central-1.elb.amazonaws.com/swagger-ui/index.html

**Local:** http://localhost:8080/swagger-ui.html

## Running Locally

### 1. Start PostgreSQL

```bash
docker run --name rfid-db \
  -e POSTGRES_DB=rfid \
  -e POSTGRES_USER=rfid_user \
  -e POSTGRES_PASSWORD=rfid_pass \
  -p 5432:5432 -d postgres:16
```

### 2. Configure the database

Set the following environment variables:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/rfid
SPRING_DATASOURCE_USERNAME=rfid_user
SPRING_DATASOURCE_PASSWORD=rfid_pass
```

### 3. Run the application

```bash
./mvnw clean package
java -jar target/rfid-service-0.0.1-SNAPSHOT.jar
```

The application runs on `http://localhost:8080`.

## Testing

Run the tests using:

```bash
./mvnw test
```

The project includes unit tests with JUnit and Mockito, as well as integration tests using Testcontainers.

## AWS Deployment

The application is containerized using Docker and deployed on AWS.

- **Amazon ECR:** Stores the Docker image.
- **Amazon ECS Fargate:** Runs the Spring Boot application.
- **Amazon RDS:** Hosts the PostgreSQL database.
- **Application Load Balancer:** Routes incoming requests to the service.
- **CloudWatch:** Collects application logs and monitors service metrics.
- **AWS Secrets Manager:** Manages database credentials securely.

The `/actuator/health` endpoint is used for application health checks.

## Monitoring

Application logs are available in CloudWatch under `/ecs/rfid-service`. ECS and RDS metrics are monitored through CloudWatch, with alarms configured for service health and resource usage.

---

Developed for the Anexya Java Microservice Coding Challenge.
