package com.enigmazer.clef.repository;

import com.enigmazer.clef.entity.SocialAccount;
import com.enigmazer.clef.enums.SocialAccountProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderId(SocialAccountProvider provider, String providerId);
}
