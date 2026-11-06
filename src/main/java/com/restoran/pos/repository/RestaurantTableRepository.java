package com.restoran.pos.repository;

import com.restoran.pos.entity.RestaurantTable;

import java.util.List;
import java.util.Optional;

public interface RestaurantTableRepository {

    RestaurantTable save(RestaurantTable table);

    Optional<RestaurantTable> findById(Long id);

    /** Masa satırını kilitler: aynı masaya gelen eşzamanlı istekler (çift tıklama dahil) sıraya girer. */
    Optional<RestaurantTable> findByIdForUpdate(Long id);

    List<RestaurantTable> findAll();

    List<RestaurantTable> findByWaiterId(Long waiterId);

    RestaurantTable update(RestaurantTable table);

    void delete(RestaurantTable table);

    boolean existsByTableNumber(Integer tableNumber, Long excludeId);

    long countByWaiterId(Long waiterId);
}
