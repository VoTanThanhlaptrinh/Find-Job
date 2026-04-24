package com.job_web.data;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.job_web.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);

	long countByRoleIn(Collection<String> roles);
	
	Optional<User> findByEmailAndPassword(String email, String password);
}


