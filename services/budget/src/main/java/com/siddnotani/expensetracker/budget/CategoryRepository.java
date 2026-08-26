package com.siddnotani.expensetracker.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByUserIdOrderByNameAsc(UUID userId);

    Optional<Category> findByUserIdAndId(UUID userId, UUID id);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);
}
