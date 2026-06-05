package com.nlu.identity.domain.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nlu.identity.domain.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail_Value(String email);

	long countByRoleIn(Collection<String> roles);
	
}


