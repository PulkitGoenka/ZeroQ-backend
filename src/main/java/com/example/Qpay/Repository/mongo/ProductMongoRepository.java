package com.example.Qpay.Repository.mongo;

import com.example.Qpay.Document.ProductDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductMongoRepository extends MongoRepository<ProductDocument, String> {

    /**
     * Barcode se product dhundo — sabse common query.
     * Barcode scan hone pe ye call hogi.
     */
    Optional<ProductDocument> findByBarcodeAndActiveTrue(String barcode);

    /**
     * Brand ke saare products.A
     */
    java.util.List<ProductDocument> findByBrandSlugAndActiveTrue(String brandSlug);

    /**
     * Specific store mein available products by barcode.
     * storePrices array mein storeId aur available = true check karo.
     */
    @Query("{ 'barcode': ?0, 'storePrices': { $elemMatch: { 'storeId': ?1, 'available': true } }, 'active': true }")
    Optional<ProductDocument> findByBarcodeAndStoreIdAndAvailable(String barcode, String storeId);
}
