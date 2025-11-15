package com.example.restrosuite.repository;

import com.example.restrosuite.entity.MenuItemModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MenuItemModifierGroupRepository extends JpaRepository<MenuItemModifierGroup, UUID> {
    List<MenuItemModifierGroup> findByMenuItemId(UUID menuItemId);
    void deleteByMenuItemId(UUID menuItemId);
    
    // Custom query to fetch only IDs without triggering lazy loading
    // Using native query to directly access foreign key column
    @Query(value = "SELECT id, modifier_group_id, display_order FROM menu_item_modifier_group WHERE menu_item_id = :menuItemId", nativeQuery = true)
    List<Object[]> findLinkAndGroupIds(@Param("menuItemId") UUID menuItemId);
}

