package com.nlu.recruitment.domain.repository;

import com.nlu.recruitment.domain.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
}
