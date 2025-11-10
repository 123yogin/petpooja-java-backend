package com.example.restrosuite.repository;

import com.example.restrosuite.entity.MenuIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MenuIngredientRepository extends JpaRepository<MenuIngredient, UUID> {
}

