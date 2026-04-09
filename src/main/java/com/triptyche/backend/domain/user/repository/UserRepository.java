package com.triptyche.backend.domain.user.repository;

import com.triptyche.backend.domain.user.model.User;
import com.triptyche.backend.domain.user.model.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findUserByUserEmailAndProvider(String userEmail, String provider);

  Optional<User> findByUserEmail(String userEmail);

  boolean existsByUserNickName(String userNickName);

  Optional<User> findByUserNickName(String userNickName);

  List<User> findByRoleAndCreatedAtBefore(UserRole role, LocalDateTime threshold);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM User u WHERE u.userId IN :userIds")
  void deleteAllByUserIdIn(@Param("userIds") List<Long> userIds);
}
