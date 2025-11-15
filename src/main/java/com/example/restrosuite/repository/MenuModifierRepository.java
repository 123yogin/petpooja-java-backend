package com.example.restrosuite.repository;

import com.example.restrosuite.entity.MenuModifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MenuModifierRepository extends JpaRepository<MenuModifier, UUID> {
    List<MenuModifier> findByModifierGroupId(UUID modifierGroupId);
    List<MenuModifier> findByIsActiveTrue();
}

