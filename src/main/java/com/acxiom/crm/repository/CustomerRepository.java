package com.acxiom.crm.repository;

import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByPhoneAndIdNot(String phone, Long id);
    Optional<Customer> findByEmail(String email);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(c.customerCode, 6) AS int)), 0) FROM Customer c")
    int findMaxCustomerCodeSequence();

    @Query("SELECT c FROM Customer c WHERE " +
           "(:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "OR LOWER(c.phone) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "OR LOWER(c.company) LIKE LOWER(CONCAT('%',:search,'%'))) " +
           "AND (:assignedTo IS NULL OR c.assignedTo = :assignedTo) " +
           "AND (:active IS NULL OR c.active = :active)")
    Page<Customer> search(@Param("search") String search,
                          @Param("assignedTo") User assignedTo,
                          @Param("active") Boolean active,
                          Pageable pageable);
}
