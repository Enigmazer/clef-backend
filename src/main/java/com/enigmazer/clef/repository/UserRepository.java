package com.enigmazer.clef.repository;

import com.enigmazer.clef.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByEmail(String email);

    @SuppressWarnings("NullableProblems")
    @EntityGraph(attributePaths = {"role"})
    Optional<User> findById(@NonNull Long id);
}
