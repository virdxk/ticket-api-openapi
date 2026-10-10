package ru.itmo.soa.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.soa.ticket.api.model.TicketType;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<TicketEntity, Long> {

    Optional<TicketEntity> findFirstByType(TicketType type);
}
