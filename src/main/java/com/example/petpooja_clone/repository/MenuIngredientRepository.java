package com.example.petpooja_clone.repository;

import com.example.petpooja_clone.entity.MenuIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MenuIngredientRepository extends JpaRepository<MenuIngredient, UUID> {
}

