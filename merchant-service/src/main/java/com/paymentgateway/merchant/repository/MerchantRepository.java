package com.paymentgateway.merchant.repository;

import com.paymentgateway.merchant.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByEmail(String email);
    Optional<Merchant> findByApiKey(String apiKey);
    boolean existsByEmail(String email);
    boolean existsByApiKey(String apiKey);
}
