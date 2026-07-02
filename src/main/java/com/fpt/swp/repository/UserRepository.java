package com.fpt.swp.repository;

import com.fpt.swp.model.Role;
import com.fpt.swp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByMail(String mail);
    Boolean existsByMail(String mail);
    List<User> findByRole(Role role);
    List<User> findByRoleIn(List<Role> roles);
}
