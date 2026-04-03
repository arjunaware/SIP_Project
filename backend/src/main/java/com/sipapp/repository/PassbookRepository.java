package com.sipapp.repository;

import com.sipapp.entity.Passbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PassbookRepository extends JpaRepository<Passbook, String> {
    List<Passbook> findByUserId(Long userId);
    Optional<Passbook> findByIdAndUserId(String id, Long userId);
}
