package com.acxiom.crm.repository;

import com.acxiom.crm.entity.Lead;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(l.leadCode, 6) AS int)), 0) FROM Lead l")
    int findMaxLeadCodeSequence();

    @Query("SELECT l FROM Lead l WHERE " +
           "(:search IS NULL OR LOWER(l.name) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "OR LOWER(l.company) LIKE LOWER(CONCAT('%',:search,'%'))) " +
           "AND (:status IS NULL OR l.status = :status) " +
           "AND (:assignedTo IS NULL OR l.assignedTo = :assignedTo)")
    Page<Lead> search(@Param("search") String search,
                      @Param("status") LeadStatus status,
                      @Param("assignedTo") User assignedTo,
                      Pageable pageable);

    List<Lead> findByStatus(LeadStatus status);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.status = :status AND (:assignedTo IS NULL OR l.assignedTo = :assignedTo)")
    long countByStatusAndAssignedTo(@Param("status") LeadStatus status, @Param("assignedTo") User assignedTo);

    @Query("SELECT l.status, COUNT(l) FROM Lead l WHERE (:assignedTo IS NULL OR l.assignedTo = :assignedTo) GROUP BY l.status")
    List<Object[]> countGroupByStatus(@Param("assignedTo") User assignedTo);
}
