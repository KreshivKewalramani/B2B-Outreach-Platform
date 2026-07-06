package com.example.demo.repositories;

import com.example.demo.entities.SMTPAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SMTPAccountRepository extends JpaRepository<SMTPAccount, Long> {
    List<SMTPAccount> findByActive(boolean active);
    Optional<SMTPAccount> findByEmail(String email);
}
