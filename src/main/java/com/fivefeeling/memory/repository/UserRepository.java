package com.fivefeeling.memory.repository;

import com.fivefeeling.memory.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findUserByUserEmailAndProvider(String userEmail, String provider);

  Optional<User> findByUserEmail(String userEmail);
}
