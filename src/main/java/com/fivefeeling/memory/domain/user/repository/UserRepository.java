package com.fivefeeling.memory.domain.user.repository;

import com.fivefeeling.memory.domain.user.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findUserByUserEmailAndProvider(String userEmail, String provider);

  Optional<User> findByUserEmail(String userEmail);

  boolean existsByUserNickName(String userNickName);
}
