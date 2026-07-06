package com.example.demo.repositories;

import com.example.demo.entities.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    long countByStatus(String status);
    List<EmailLog> findTop10ByOrderByTimestampDesc();
}
