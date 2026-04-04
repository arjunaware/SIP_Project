package com.sipapp.repository;

import com.sipapp.entity.Sip;
import com.sipapp.enums.SipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// repository/SipRepository.java
@Repository
public interface SipRepository extends JpaRepository<Sip, Long> {

    // Finds all active SIPs where execution time has come
    List<Sip> findByStatusAndNextExecutionDateLessThanEqual(
            SipStatus status,
            LocalDateTime dateTime
    );

    // Existing queries...
    @Query("SELECT s FROM Sip s WHERE s.passbook.user.id = :userId")
    List<Sip> findAllByUserId(Long userId);


    /**
     * THE KEY SCHEDULER QUERY.
     * Fetches only SIPs that are:
     *   - ACTIVE (skip PAUSED and COMPLETED)
     *   - Due today or overdue (nextExecutionDate <= today)
     *
     * This means the scheduler never touches PAUSED or COMPLETED SIPs.
     */
    @Query("SELECT s FROM Sip s WHERE s.status = :status AND s.nextExecutionDate <= :today")
    List<Sip> findDueSips(@Param("status") SipStatus status, @Param("today") LocalDate today);


    // All SIPs by status (admin / reports)
    List<Sip> findByStatus(SipStatus status);

    // SIPs for a specific passbook
    @Query("SELECT s FROM Sip s WHERE s.passbook.id = :passbookId")
    List<Sip> findByPassbookId(@Param("passbookId") String passbookId);
}
