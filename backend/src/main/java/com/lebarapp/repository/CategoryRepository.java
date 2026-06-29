package com.lebarapp.repository;

import com.lebarapp.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Active categories ordered for menu presentation: by display order, then by
     * name (case-insensitive) as a deterministic tie-breaker.
     */
    List<Category> findByActiveTrueOrderByDisplayOrderAscNameAsc();
}
