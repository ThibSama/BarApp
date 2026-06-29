package com.lebarapp.repository;

import com.lebarapp.entity.CocktailPrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CocktailPriceRepository extends JpaRepository<CocktailPrice, Long> {
}
