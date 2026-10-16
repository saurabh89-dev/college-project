# EchoLoop

## Web application

EchoLoop is a Spring Boot web chat application. It runs on port `8080`, stores data in the MySQL `echoloop` database, and uses WebSockets for real-time delivery. The database bootstrap script is in `database/schema.sql`; the active additive migration remains in `src/main/resources/schema.sql`.

Install Maven, set `ECHOLOOP_DB_USERNAME` and `ECHOLOOP_DB_PASSWORD` if needed, then run:

```powershell
mvn spring-boot:run
```

College project: a Java real-time chat application with MySQL.

## Project layout

- `src/main/java/com/echoloop/EchoLoopApplication.java` - Spring Boot application, API, and WebSocket configuration
- `src/main/resources/static/index.html` - browser user interface
- `src/main/resources/application.properties` - application and database configuration
- `src/main/resources/schema.sql` - additive database migration
- `database/schema.sql` - initial MySQL schema

