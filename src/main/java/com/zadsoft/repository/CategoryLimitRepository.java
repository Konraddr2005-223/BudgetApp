package com.zadsoft.repository;

import com.zadsoft.model.CategoryLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryLimitRepository extends JpaRepository<CategoryLimit, Long> {
    Optional<CategoryLimit> findByCategoryIgnoreCase(String category);
}
