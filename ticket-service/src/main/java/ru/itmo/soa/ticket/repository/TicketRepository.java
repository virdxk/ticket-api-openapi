package ru.itmo.soa.ticket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.soa.ticket.api.model.TicketType;
import ru.itmo.soa.ticket.entity.TicketEntity;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {

    Optional<TicketEntity> findFirstByType(TicketType type);
}
