package com.example.restrosuite.repository;

import com.example.restrosuite.entity.MenuItemModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MenuItemModifierGroupRepository extends JpaRepository<MenuItemModifierGroup, UUID> {
    List<MenuItemModifierGroup> findByMenuItemId(UUID menuItemId);
    void deleteByMenuItemId(UUID menuItemId);
}

