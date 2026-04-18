package com.enigmazer.clef.repository;

import com.enigmazer.clef.entity.PhoneNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhoneNumberRepository extends JpaRepository<PhoneNumber, Long> {

    List<PhoneNumber> findAllByUserId(Long userId);

    int countByUserId(Long userId);

    boolean existsByPhoneNumber(String normalized);

    boolean existsByPhoneNumberAndUserId(String normalized, Long userId);

    Optional<PhoneNumber> findByPhoneNumberAndUserId(String phoneNumber, Long userId);

    Optional<PhoneNumber> findByUserIdAndIsPrimaryTrue(Long userId);

    Optional<PhoneNumber> findByUserIdAndIsPrimaryFalse(Long userId);

    @Transactional
    @Modifying
    @Query("UPDATE PhoneNumber p SET p.isPrimary = false WHERE p.user.id = :userId")
    void clearPrimaryByUserId(@Param("userId") Long userId);
}
