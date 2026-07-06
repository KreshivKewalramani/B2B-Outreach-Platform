package com.example.demo.repositories;

import com.example.demo.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByEmail(String email);
    
    @Query("SELECT DISTINCT c.industry FROM Company c WHERE c.industry IS NOT NULL AND c.industry != ''")
    List<String> findDistinctIndustries();
    
    @Query("SELECT c FROM Company c WHERE " +
           "(:query IS NULL OR :query = '' OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.contactPerson) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:industry IS NULL OR :industry = '' OR c.industry = :industry)")
    List<Company> searchAndFilter(@Param("query") String query, @Param("industry") String industry);
}
