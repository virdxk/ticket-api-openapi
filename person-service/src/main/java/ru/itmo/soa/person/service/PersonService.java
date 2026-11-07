package ru.itmo.soa.person.service;

import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.soa.common.error.ApiException;
import ru.itmo.soa.common.query.QueryParameters;
import ru.itmo.soa.person.api.model.Person;
import ru.itmo.soa.person.api.model.PersonCreate;
import ru.itmo.soa.person.api.model.PersonPage;
import ru.itmo.soa.person.api.model.PersonPatch;
import ru.itmo.soa.person.entity.PersonEntity;
import ru.itmo.soa.person.repository.PersonRepository;
import ru.itmo.soa.person.utils.Constants;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static ru.itmo.soa.common.utils.Constants.ERROR_BODY_EMPTY;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository repository;
    private final EntityManager entityManager;

    @Transactional
    public Person create(PersonCreate body) {
        checkBirthday(body.getBirthday());
        PersonEntity e = new PersonEntity();
        e.firstName = body.getFirstName();
        e.lastName = body.getLastName();
        e.birthday = body.getBirthday();
        return toDto(repository.save(e));
    }

    @Transactional(readOnly = true)
    public PersonPage list(Integer page, Integer size, List<String> sort, List<String> filter) {
        Map<String, Object> parameters = new HashMap<>();
        String where = QueryParameters.whereClause(
                PersonEntity.class,
                filter,
                field -> "e." + field,
                parameters
        );
        String orderBy = QueryParameters.orderBy(PersonEntity.class, sort, field -> "e." + field);

        TypedQuery<Long> countQuery = entityManager.createQuery(
                """
                select count(e)
                from PersonEntity e
                """ + where,
                Long.class
        );
        parameters.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();
        long offset = (long) (page - 1) * size;
        List<Person> items = List.of();
        if (offset < total) {
            TypedQuery<PersonEntity> query = entityManager.createQuery(
                    """
                    select e
                    from PersonEntity e
                    """ + where + " order by " + orderBy,
                    PersonEntity.class
            );
            parameters.forEach(query::setParameter);
            items = query.setFirstResult((int) offset)
                    .setMaxResults(size)
                    .getResultList()
                    .stream()
                    .map(PersonService::toDto)
                    .toList();
        }
        return new PersonPage(items, page, size, total, (int) ((total + size - 1) / size));
    }

    @Transactional(readOnly = true)
    public Person get(Long id) {
        return toDto(find(id));
    }

    @Transactional
    public Person update(Long id, PersonPatch patch) {
        if (patch.getFirstName() == null && patch.getLastName() == null && patch.getBirthday() == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ERROR_BODY_EMPTY);
        }
        if (patch.getBirthday() != null) checkBirthday(patch.getBirthday());
        PersonEntity e = find(id);
        if (patch.getFirstName() != null) e.firstName = patch.getFirstName();
        if (patch.getLastName() != null) e.lastName = patch.getLastName();
        if (patch.getBirthday() != null) e.birthday = patch.getBirthday();
        return toDto(e);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private PersonEntity find(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound(Constants.PERSON_NOT_FOUND, id));
    }

    private static void checkBirthday(LocalDate birthday) {
        if (birthday.isAfter(LocalDate.now(ZoneOffset.UTC))) {
            throw ApiException.invalid("birthday", Constants.PERSON_BIRTHDAY_FUTURE);
        }
    }

    private static Person toDto(PersonEntity e) {
        return new Person(e.id, e.firstName, e.lastName, e.birthday);
    }
}
