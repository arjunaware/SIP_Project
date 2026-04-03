package com.sipapp.repository;

import com.sipapp.entity.Sip;
import com.sipapp.enums.SipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SipRepository extends JpaRepository<Sip, Long> {

    @Query("SELECT s FROM Sip s WHERE s.passbook.user.id = :userId")
    List<Sip> findAllByUserId(Long userId);

    List<Sip> findByStatus(SipStatus status);

    @Query("SELECT s FROM Sip s WHERE s.passbook.id = :passbookId")
    List<Sip> findByPassbookId(String passbookId);
}
