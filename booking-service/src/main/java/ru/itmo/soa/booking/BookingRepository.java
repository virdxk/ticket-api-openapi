package ru.itmo.soa.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    @Modifying
    @Query("delete from BookingEntity b where b.personId = :personId")
    int deleteByPersonId(Long personId);
}
