package com.example.restrosuite.repository;

import com.example.restrosuite.entity.Outlet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutletRepository extends JpaRepository<Outlet, UUID> {
    Optional<Outlet> findByCode(String code);
    List<Outlet> findByIsActiveTrue();
    List<Outlet> findByIsActive(Boolean isActive);
}

