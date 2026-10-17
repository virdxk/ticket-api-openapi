package ru.itmo.soa.person.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.soa.common.error.ApiException;
import ru.itmo.soa.common.error.Violation;
import ru.itmo.soa.common.repository.PageQuery;
import ru.itmo.soa.person.api.model.Person;
import ru.itmo.soa.person.api.model.PersonCreate;
import ru.itmo.soa.person.api.model.PersonPage;
import ru.itmo.soa.person.api.model.PersonPatch;
import ru.itmo.soa.person.entity.PersonEntity;
import ru.itmo.soa.person.repository.PersonRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class PersonService {

    private static final Set<String> FIELDS = Set.of("id", "firstName", "lastName", "birthday");

    @Autowired
    private PersonRepository repository;

    @Autowired
    private PageQuery pageQuery;

    public Person create(PersonCreate body) {
        checkBirthday(body.getBirthday());
        PersonEntity e = new PersonEntity();
        e.firstName = body.getFirstName();
        e.lastName = body.getLastName();
        e.birthday = body.getBirthday();
        return toDto(repository.saveAndFlush(e));
    }

    @Transactional(readOnly = true)
    public PersonPage list(Integer page, Integer size, List<String> sort, List<String> filter) {
        PageQuery.Result<PersonEntity> result = pageQuery.find(PersonEntity.class, FIELDS, Set.of(),
                page, size, sort, filter);
        return new PersonPage(result.items().stream().map(PersonService::toDto).toList(),
                page, size, result.totalElements(), result.totalPages());
    }

    @Transactional(readOnly = true)
    public Person get(Long id) {
        return toDto(find(id));
    }

    public Person update(Long id, PersonPatch patch) {
        if (patch.getFirstName() == null && patch.getLastName() == null && patch.getBirthday() == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "error.body.empty");
        }
        if (patch.getBirthday() != null) checkBirthday(patch.getBirthday());
        PersonEntity e = find(id);
        if (patch.getFirstName() != null) e.firstName = patch.getFirstName();
        if (patch.getLastName() != null) e.lastName = patch.getLastName();
        if (patch.getBirthday() != null) e.birthday = patch.getBirthday();
        return toDto(repository.saveAndFlush(e));
    }

    public void delete(Long id) {
        repository.delete(find(id));
    }

    private PersonEntity find(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("person.notFound", id));
    }

    private static void checkBirthday(LocalDate birthday) {
        if (birthday.isAfter(LocalDate.now())) {
            throw ApiException.invalid(List.of(Violation.of("birthday", "person.birthday.future")));
        }
    }

    private static Person toDto(PersonEntity e) {
        return new Person(e.id, e.firstName, e.lastName, e.birthday);
    }
}
