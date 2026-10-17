package ru.itmo.soa.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.itmo.soa.booking.entity.BookingEntity;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    @Modifying
    @Query("delete from BookingEntity b where b.personId = :personId")
    int deleteByPersonId(Long personId);
}
