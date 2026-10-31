# Ticket API conventions

- Java 17, Spring Boot 3.5.16, Maven reactor: `common`, `ticket-service`, `person-service`, `booking-service`. Resolve versions from the POMs before using newer APIs.
- This application uses Spring Data JPA and PostgreSQL. Do not apply Spring Data JDBC aggregate rules or add a second persistence stack.
- Root `ticket-service.yaml`, `person-service.yaml`, and `booking-service.yaml` own the OpenAPI contracts. Maven generates server interfaces/models and Booking Service clients under module `target/` directories. Never hand-edit generated sources or add a parallel API/controller to bypass them.
- Follow the existing controller -> service -> repository structure within each service. Keep shared error handling and query mechanics in `common` when they already own the behavior. Avoid unrelated extraction into shared abstractions.
- Use constructor injection with final dependency fields. Do not add `@Autowired` to a single constructor. Preserve public entity fields and manual DTO conversion; do not add MapStruct/Lombok or mapper/base-service interfaces without a requirement.
- Ticket Service and Person Service remain independent. Booking Service owns their relationship. Preserve this boundary and the generated remote-client contracts.
- Preserve the decisions in `README.md`: pages start at 1; page size is capped at 100; repeated `sort` and `filter` parameters; JSON Merge Patch absent/null semantics; the shared error shape and documented status codes. Check the relevant YAML schema before changing request validation or responses.
- Service transactions already define atomic operations. Preserve association lifecycle, error semantics, and existing transaction boundaries when changing a persistence path.

## Focused verification

- Compile the changed service and required upstream modules, e.g. `mvn -B -pl ticket-service -am compile`. This also checks the relevant OpenAPI generation. Select the affected module rather than building every service by default.
- Run relevant existing checks with Maven. Write new or changed tests only after implementation and the user's explicit agreement; ask then unless already requested or declined. Do not add test dependencies or standalone verification programs without that agreement. Compilation does not prove runtime behavior; report that limit.
- No repository-wide Java formatter is configured. Match the surrounding style and avoid unrelated reformatting.
- Service startup reads database connection settings from environment and currently uses `ddl-auto=update`. Runtime/database checks need a known isolated database; compilation does not require starting a service or changing database settings.

Do not stage pre-existing edits, commit, push, or publish as a side effect of implementation, review, or verification.
