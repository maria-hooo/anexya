# RFID Tag Management Service

A Java / Spring Boot microservice for managing RFID tags, built for the anexya coding challenge and deployed on AWS (ECS, RDS, CloudWatch).

> Items in `[brackets]` are placeholders. Update them to match the final implementation.

---

## Table of Contents

1. [Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [Architecture](#architecture)
4. [Project Structure](#project-structure)
5. [Prerequisites](#prerequisites)
6. [Configuration](#configuration)
7. [Running Locally](#running-locally)
8. [API Endpoints](#api-endpoints)
9. [Testing](#testing)
10. [AWS Deployment](#aws-deployment)
11. [Monitoring and Logging](#monitoring-and-logging)

---

## Overview

`rfid-service` exposes a REST API to register, retrieve, update and delete RFID tags, and to [record tag reads / track tag status / associate tags with assets]. Data is stored in a relational database (PostgreSQL on Amazon RDS), and the service runs as a container on Amazon ECS with logs and metrics sent to Amazon CloudWatch.

## Tech Stack

| Layer            | Technology                                  |
|------------------|---------------------------------------------|
| Language         | Java [17 / 21]                              |
| Framework        | Spring Boot [3.x] (Web, Data JPA, Validation, Actuator) |
| Build            | Maven                                       |
| Database         | PostgreSQL (Amazon RDS in AWS)              |
| Migrations       | [Flyway / Liquibase / JPA ddl-auto]         |
| Containerization | Docker                                      |
| Hosting          | Amazon ECS ([Fargate / EC2]), Amazon ECR    |
| Observability    | Amazon CloudWatch Logs and Metrics, Spring Boot Actuator |
| Testing          | JUnit 5, Mockito, [Testcontainers]          |

## Architecture

```
            Client / RFID gateway
                     |
                     v
        [Application Load Balancer]
                     |
                     v
     +-------------------------------+
     |  ECS service: rfid-service    |
     |  (Spring Boot container)      |
     +-------------------------------+
           |                   |
           v                   v
   Amazon RDS (PostgreSQL)   Amazon CloudWatch
                             (logs, metrics, alarms)
```

The service follows a layered design:

- **Controller**: REST endpoints, request validation, HTTP status mapping
- **Service**: business logic
- **Repository**: data access via Spring Data JPA
- **Exception handling**: a global `@ControllerAdvice` returns consistent error responses

## Project Structure

```
rfid-service/
├── src/main/java/[com/anexya/rfid]/
│   ├── controller/      # REST controllers
│   ├── service/         # Business logic
│   ├── repository/      # Spring Data JPA repositories
│   ├── model/           # JPA entities
│   ├── dto/             # Request / response objects
│   ├── exception/       # Custom exceptions and global handler
│   └── RfidServiceApplication.java
├── src/main/resources/
│   ├── application.yml
│   └── [db/migration/]  # Database migrations
├── src/test/java/       # Unit and integration tests
├── Dockerfile
├── [docker-compose.yml]
├── [infra/]             # [Task definition / Terraform / CloudFormation]
└── pom.xml
```

## Prerequisites

- Java [17] or later
- Maven 3.9+ (or use the included `./mvnw` wrapper)
- Docker
- For deployment: AWS CLI configured with access to ECR, ECS, RDS and CloudWatch

## Configuration

The application is configured through environment variables, so no credentials are stored in the repository.

| Variable                       | Description                  | Example                                     |
|--------------------------------|------------------------------|---------------------------------------------|
| `SPRING_DATASOURCE_URL`        | JDBC URL of the database     | `jdbc:postgresql://localhost:5432/rfid`     |
| `SPRING_DATASOURCE_USERNAME`   | Database user                | `rfid_user`                                 |
| `SPRING_DATASOURCE_PASSWORD`   | Database password            | *(set via environment or AWS Secrets Manager)* |
| `SERVER_PORT`                  | HTTP port                    | `8080`                                      |
| `SPRING_PROFILES_ACTIVE`       | Active profile               | `local` / `aws`                             |

## Running Locally

**1. Start PostgreSQL**

```bash
docker run --name rfid-db -e POSTGRES_DB=rfid -e POSTGRES_USER=rfid_user \
  -e POSTGRES_PASSWORD=rfid_pass -p 5432:5432 -d postgres:16
```

[Or, if `docker-compose.yml` is included: `docker compose up -d`]

**2. Build and run the service**

```bash
./mvnw clean package
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/rfid \
SPRING_DATASOURCE_USERNAME=rfid_user \
SPRING_DATASOURCE_PASSWORD=rfid_pass \
java -jar target/rfid-service-0.0.1-SNAPSHOT.jar
```

**3. Check that it is running**

```bash
curl http://localhost:8080/actuator/health
```

**Run with Docker**

```bash
docker build -t rfid-service .
docker run -p 8080:8080 --env-file .env rfid-service
```

## API Endpoints

Base path: `/api/v1/tags`

| Method | Endpoint                 | Description                 |
|--------|--------------------------|-----------------------------|
| POST   | `/api/v1/tags`           | Create a new tag            |
| GET    | `/api/v1/tags`           | List tags (paginated)       |
| GET    | `/api/v1/tags/{id}`      | Get a tag by ID             |
| PUT    | `/api/v1/tags/{id}`      | Update a tag                |
| DELETE | `/api/v1/tags/{id}`      | Delete a tag                |
| [GET]  | [`/api/v1/tags/search?status=ACTIVE`] | [Filter tags]  |

**Example request**

```bash
curl -X POST http://localhost:8080/api/v1/tags \
  -H "Content-Type: application/json" \
  -d '{
        "epc": "E2801160600002045678ABCD",
        "status": "ACTIVE",
        "location": "Warehouse A"
      }'
```

**Example response** (`201 Created`)

```json
{
  "id": 1,
  "epc": "E2801160600002045678ABCD",
  "status": "ACTIVE",
  "location": "Warehouse A",
  "createdAt": "2026-10-14T09:00:00Z"
}
```

**Error responses** follow a consistent format:

```json
{
  "timestamp": "2026-10-14T09:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Tag with id 99 not found"
}
```

[If Swagger / springdoc-openapi is included: interactive API docs are available at `http://localhost:8080/swagger-ui.html`.]

## Testing

```bash
./mvnw test
```

- **Unit tests** cover the service layer, with the repository mocked using Mockito.
- **Integration tests** cover controllers and persistence [using Testcontainers with PostgreSQL].

## AWS Deployment

**1. Build and push the image to Amazon ECR**

```bash
aws ecr create-repository --repository-name rfid-service
aws ecr get-login-password --region [region] | docker login --username AWS \
  --password-stdin [account-id].dkr.ecr.[region].amazonaws.com
docker build -t rfid-service .
docker tag rfid-service:latest [account-id].dkr.ecr.[region].amazonaws.com/rfid-service:latest
docker push [account-id].dkr.ecr.[region].amazonaws.com/rfid-service:latest
```

**2. Create the database on Amazon RDS**

- Engine: PostgreSQL
- Placed in private subnets
- Security group allows inbound 5432 only from the ECS service's security group

**3. Deploy on Amazon ECS**

- Create a task definition that uses the ECR image, exposes port 8080, and sets the datasource environment variables. The password comes from [AWS Secrets Manager / SSM Parameter Store].
- Use the `awslogs` log driver to send container logs to CloudWatch.
- Create an ECS service ([Fargate]) behind [an Application Load Balancer], with the health check on `/actuator/health`.

[Infrastructure is defined in `infra/` using [Terraform / CloudFormation / CDK]. See that folder for details.]

## Monitoring and Logging

- **Logs**: application logs go to the CloudWatch log group `/ecs/rfid-service`.
- **Metrics**: ECS CPU and memory utilization, RDS metrics, [custom application metrics via Micrometer].
- **Health**: Spring Boot Actuator `/actuator/health` is used by the load balancer and ECS.
- [**Alarms**: CloudWatch alarms on high CPU, unhealthy targets, and 5xx error rate.]

