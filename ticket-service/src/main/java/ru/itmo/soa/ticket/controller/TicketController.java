package ru.itmo.soa.ticket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.soa.ticket.api.TicketOperationsApi;
import ru.itmo.soa.ticket.api.TicketsApi;
import ru.itmo.soa.ticket.api.model.Ticket;
import ru.itmo.soa.ticket.api.model.TicketCreate;
import ru.itmo.soa.ticket.api.model.TicketPage;
import ru.itmo.soa.ticket.api.model.TicketPatch;
import ru.itmo.soa.ticket.api.model.TicketType;
import ru.itmo.soa.ticket.service.TicketService;

import java.util.List;

@RestController
public class TicketController implements TicketsApi, TicketOperationsApi {

    @Autowired
    private TicketService service;

    @Override
    public ResponseEntity<Ticket> createTicket(TicketCreate body) {
        Ticket ticket = service.create(body);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(ticket.getId()).toUri()).body(ticket);
    }

    @Override
    public ResponseEntity<TicketPage> listTickets(Integer page, Integer size, List<String> sort, List<String> filter) {
        return ResponseEntity.ok(service.list(page, size, sort, filter));
    }

    @Override
    public ResponseEntity<Ticket> getTicket(Long ticketId) {
        return ResponseEntity.ok(service.get(ticketId));
    }

    @Override
    public ResponseEntity<Ticket> updateTicket(Long ticketId, TicketPatch patch) {
        return ResponseEntity.ok(service.update(ticketId, patch));
    }

    @Override
    public ResponseEntity<Void> deleteTicket(Long ticketId) {
        service.delete(ticketId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteAnyTicketByType(TicketType type) {
        service.deleteAnyByType(type);
        return ResponseEntity.noContent().build();
    }
}
