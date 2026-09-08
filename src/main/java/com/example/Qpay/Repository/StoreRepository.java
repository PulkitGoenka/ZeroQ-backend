package com.example.Qpay.Repository;

import com.example.Qpay.Entity.Stores;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Stores, UUID> {

    // 1. Pincode
    @Query("SELECT s FROM Stores s WHERE s.pincode = :pincode " +
            "AND s.isActive = true " +
            "AND (:brandId IS NULL OR s.brand.id = :brandId) " +
            "ORDER BY s.name")
    List<Stores> findByBrandAndPincode(
            @Param("brandId") UUID brandId,
            @Param("pincode") String pincode
    );

    // 2. State (Safe JPQL Case-Insensitive partial search)
    @Query("SELECT s FROM Stores s WHERE s.isActive = true " +
            "AND (:brandId IS NULL OR s.brand.id = :brandId) " +
            "AND LOWER(s.state) LIKE LOWER(CONCAT('%', :state, '%')) " +
            "ORDER BY s.name ASC")
    List<Stores> findByBrandAndState(
            @Param("brandId") UUID brandId,
            @Param("state") String state
    );

    // 3. District (Safe JPQL Case-Insensitive partial search)
    @Query("SELECT s FROM Stores s WHERE s.isActive = true " +
            "AND (:brandId IS NULL OR s.brand.id = :brandId) " +
            "AND LOWER(s.district) LIKE LOWER(CONCAT('%', :district, '%')) " +
            "ORDER BY s.name ASC")
    List<Stores> findByBrandAndDistrict(
            @Param("brandId") UUID brandId,
            @Param("district") String district
    );

    // 4. QR Code
    Optional<Stores> findByQrCodeAndIsActiveTrue(String qrCode);
}