package ru.itmo.soa.person;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.soa.common.ApiError;
import ru.itmo.soa.common.ApiException;
import ru.itmo.soa.common.PageQuery;
import ru.itmo.soa.person.api.PersonsApi;
import ru.itmo.soa.person.api.model.Person;
import ru.itmo.soa.person.api.model.PersonCreate;
import ru.itmo.soa.person.api.model.PersonPage;
import ru.itmo.soa.person.api.model.PersonPatch;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@Transactional
public class PersonController implements PersonsApi {

    private static final Set<String> FIELDS = Set.of("id", "firstName", "lastName", "birthday");

    @PersistenceContext
    private EntityManager em;

    @Override
    public ResponseEntity<Person> createPerson(PersonCreate body) {
        checkBirthday(body.getBirthday());
        PersonEntity e = new PersonEntity();
        e.firstName = body.getFirstName();
        e.lastName = body.getLastName();
        e.birthday = body.getBirthday();
        em.persist(e);
        em.flush();
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(e.id).toUri()).body(toDto(e));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<PersonPage> listPersons(Integer page, Integer size, List<String> sort, List<String> filter) {
        PageQuery.Result<PersonEntity> result = PageQuery.find(em, PersonEntity.class, FIELDS, Set.of(),
                page, size, sort, filter);
        return ResponseEntity.ok(new PersonPage(result.items().stream().map(PersonController::toDto).toList(),
                page, size, result.totalElements(), result.totalPages()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Person> getPerson(Long personId) {
        return ResponseEntity.ok(toDto(find(personId)));
    }

    @Override
    public ResponseEntity<Person> updatePerson(Long personId, PersonPatch patch) {
        if (patch.getFirstName() == null && patch.getLastName() == null && patch.getBirthday() == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "JSON запроса должен содержать хотя бы одно поле");
        }
        if (patch.getBirthday() != null) checkBirthday(patch.getBirthday());
        PersonEntity e = find(personId);
        if (patch.getFirstName() != null) e.firstName = patch.getFirstName();
        if (patch.getLastName() != null) e.lastName = patch.getLastName();
        if (patch.getBirthday() != null) e.birthday = patch.getBirthday();
        return ResponseEntity.ok(toDto(e));
    }

    @Override
    public ResponseEntity<Void> deletePerson(Long personId) {
        em.remove(find(personId));
        return ResponseEntity.noContent().build();
    }

    private PersonEntity find(Long id) {
        PersonEntity e = em.find(PersonEntity.class, id);
        if (e == null) {
            throw ApiException.notFound("Человек с id=" + id + " не найден");
        }
        return e;
    }

    // The "not later than today" rule lives only in the field description, so codegen cannot enforce it.
    private static void checkBirthday(LocalDate birthday) {
        if (birthday.isAfter(LocalDate.now())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "JSON запроса нарушает ограничения модели",
                    List.of(new ApiError.Detail("birthday", "дата рождения не может быть в будущем")));
        }
    }

    private static Person toDto(PersonEntity e) {
        return new Person(e.id, e.firstName, e.lastName, e.birthday);
    }
}
