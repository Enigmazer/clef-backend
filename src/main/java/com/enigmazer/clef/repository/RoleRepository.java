package com.enigmazer.clef.repository;

import com.enigmazer.clef.entity.Role;
import com.enigmazer.clef.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(RoleType roleType);
}
