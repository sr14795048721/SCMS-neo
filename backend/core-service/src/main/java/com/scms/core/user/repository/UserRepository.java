package com.scms.core.user.repository;

import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByUsernameOrEmail(String username, String email);
    Optional<UserEntity> findByIdAndRole(Long id, UserRole role);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);
    List<UserEntity> findAllByIdInAndRole(Collection<Long> ids, UserRole role);
    List<UserEntity> findAllByRole(UserRole role);
    List<UserEntity> findAllByRoleIn(Collection<UserRole> roles);
    boolean existsByRole(UserRole role);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);

    default List<UserEntity> findAllSystemAdmins() {
        return findAllByRoleIn(List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN));
    }
}
