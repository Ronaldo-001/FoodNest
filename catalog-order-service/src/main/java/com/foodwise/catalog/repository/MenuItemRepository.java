package com.foodwise.catalog.repository;

import com.foodwise.catalog.model.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    Page<MenuItem> findByRestaurantIdAndAvailableTrue(Long restaurantId, Pageable pageable);

    Page<MenuItem> findByAvailableTrue(Pageable pageable);

    Optional<MenuItem> findByIdAndRestaurantId(Long id, Long restaurantId);
}
