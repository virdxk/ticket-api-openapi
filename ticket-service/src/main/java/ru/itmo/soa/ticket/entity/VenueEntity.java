package ru.itmo.soa.ticket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.itmo.soa.ticket.api.model.VenueType;

@Entity
@Table(name = "soa_venue")
public class VenueEntity {

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
