package ru.itmo.soa.person.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.soa.person.entity.PersonEntity;

public interface PersonRepository extends JpaRepository<PersonEntity, Long> {
}
