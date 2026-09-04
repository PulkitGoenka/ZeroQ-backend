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

    // Pincode — brandId optional: null hoga to sab brands ke stores aayenge
    @Query("SELECT s FROM Stores s WHERE s.pincode = :pincode " +
            "AND s.isActive = true " +
            "AND (:brandId IS NULL OR s.brand.id = :brandId) " +
            "ORDER BY s.name")
    List<Stores> findByBrandAndPincode(
            @Param("brandId") UUID brandId,
            @Param("pincode") String pincode
    );

    // State — ab FUZZY + PARTIAL match (pg_trgm)
    @Query(value =
            "SELECT * FROM stores s WHERE s.is_active = true " +
                    "AND (:brandId IS NULL OR s.brand_id = :brandId) " +
                    "AND (s.state ILIKE CONCAT('%', :state, '%') " +
                    "     OR similarity(s.state, :state) > 0.25) " +
                    "ORDER BY similarity(s.state, :state) DESC, s.name ASC",
            nativeQuery = true)
    List<Stores> findByBrandAndState(
            @Param("brandId") UUID brandId,
            @Param("state") String state
    );

    // District — ab FUZZY + PARTIAL match (pg_trgm)
    @Query(value =
            "SELECT * FROM stores s WHERE s.is_active = true " +
                    "AND (:brandId IS NULL OR s.brand_id = :brandId) " +
                    "AND (s.district ILIKE CONCAT('%', :district, '%') " +
                    "     OR similarity(s.district, :district) > 0.25) " +
                    "ORDER BY similarity(s.district, :district) DESC, s.name ASC",
            nativeQuery = true)
    List<Stores> findByBrandAndDistrict(
            @Param("brandId") UUID brandId,
            @Param("district") String district
    );

    // QR Code
    Optional<Stores> findByQrCodeAndIsActiveTrue(String qrCode);
}