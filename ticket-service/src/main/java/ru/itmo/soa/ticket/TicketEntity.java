package ru.itmo.soa.ticket;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import ru.itmo.soa.ticket.api.model.TicketType;
import ru.itmo.soa.ticket.api.model.VenueType;

import java.time.OffsetDateTime;

@Entity
@Table(name = "soa_ticket")
public class TicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String name;

    @Embedded
    public Coordinates coordinates;

    @Column(nullable = false)
    public OffsetDateTime creationDate;

    public Float price;

    @Column(nullable = false, length = 681)
    public String comment;

    @Enumerated(EnumType.STRING)
    public TicketType type;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "venue_id")
    public Venue venue;

    @Embeddable
    public static class Coordinates {
        @Column(name = "x", nullable = false)
        public Float x;
        @Column(name = "y", nullable = false)
        public Double y;
    }

    @Entity
    @Table(name = "soa_venue")
    public static class Venue {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Integer id;
        @Column(nullable = false)
        public String name;
        @Column(nullable = false)
        public Long capacity;
        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        public VenueType type;
    }
}
