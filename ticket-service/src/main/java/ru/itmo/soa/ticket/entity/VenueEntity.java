package ru.itmo.soa.ticket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import ru.itmo.soa.ticket.api.model.VenueType;

@Entity
@Table(name = "soa_venue")
public class VenueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Version
    public long version;

    @Column(nullable = false, columnDefinition = "text")
    public String name;

    @Column(nullable = false)
    public Long capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public VenueType type;
}
