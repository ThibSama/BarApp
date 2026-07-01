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

    /**
     * Management listing: every category (active and inactive), ordered by
     * display order, then name, then id as a deterministic final tie-breaker.
     */
    List<Category> findAllByOrderByDisplayOrderAscNameAscIdAsc();

    /** Case-insensitive uniqueness check used on category creation. */
    boolean existsByNameIgnoreCase(String name);

    /** Case-insensitive uniqueness check on update, excluding the edited row. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
