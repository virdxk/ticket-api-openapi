package ru.itmo.soa.person.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.soa.person.api.PersonsApi;
import ru.itmo.soa.person.api.model.Person;
import ru.itmo.soa.person.api.model.PersonCreate;
import ru.itmo.soa.person.api.model.PersonPage;
import ru.itmo.soa.person.api.model.PersonPatch;
import ru.itmo.soa.person.service.PersonService;

import java.util.List;

@RestController
public class PersonController implements PersonsApi {

    private final PersonService service;

    public PersonController(PersonService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<Person> createPerson(PersonCreate body) {
        Person person = service.create(body);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(person.getId()).toUri()).body(person);
    }

    @Override
    public ResponseEntity<PersonPage> listPersons(Integer page, Integer size, List<String> sort, List<String> filter) {
        return ResponseEntity.ok(service.list(page, size, sort, filter));
    }

    @Override
    public ResponseEntity<Person> getPerson(Long personId) {
        return ResponseEntity.ok(service.get(personId));
    }

    @Override
    public ResponseEntity<Person> updatePerson(Long personId, PersonPatch patch) {
        return ResponseEntity.ok(service.update(personId, patch));
    }

    @Override
    public ResponseEntity<Void> deletePerson(Long personId) {
        service.delete(personId);
        return ResponseEntity.noContent().build();
    }
}
