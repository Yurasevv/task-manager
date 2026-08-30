# Task Manager API

REST API сервис для управления задачами.

## Стек технологий
- Java 17
- Spring Boot 3.5.3 (Web, Data JPA, Security, Validation)
- PostgreSQL / H2
- Flyway
- MapStruct (Records)
- JWT
- Swagger / OpenAPI
- JUnit 5 & Mockito
- Docker & Docker Compose

## Запуск приложения

Для запуска приложения и базы данных PostgreSQL в контейнерах:
```bash
# Скопировать файл с переменными окружения и при необходимости изменить значения
cp .env.example .env

docker-compose up --build
```
Приложение будет доступно на порту `8080`. (Секреты и конфигурация БД подтягиваются из файла `.env`).

Запуск локально с in-memory базой H2 (без Docker):
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

## Документация API

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Коллекция для Postman находится в директории проекта:
`postman/Task_Manager_API.postman_collection.json`

## Запуск тестов

Тесты используют профиль H2. Для запуска выполните:
```bash
./mvnw clean test
```
