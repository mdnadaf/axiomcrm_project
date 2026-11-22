package com.acxiom.crm.repository;

import com.acxiom.crm.entity.FollowUp;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.FollowUpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FollowUpRepository extends JpaRepository<FollowUp, Long>, JpaSpecificationExecutor<FollowUp> {

    @Query("SELECT f FROM FollowUp f WHERE " +
           "(:status IS NULL OR f.status = :status) " +
           "AND (:assignedTo IS NULL OR f.assignedTo = :assignedTo) " +
           "AND (:fromDate IS NULL OR f.followUpDate >= :fromDate) " +
           "AND (:toDate IS NULL OR f.followUpDate <= :toDate)")
    Page<FollowUp> search(@Param("status") FollowUpStatus status,
                          @Param("assignedTo") User assignedTo,
                          @Param("fromDate") LocalDate fromDate,
                          @Param("toDate") LocalDate toDate,
                          Pageable pageable);

    @Query("SELECT f FROM FollowUp f WHERE f.status = 'PLANNED' AND f.followUpDate < :today")
    List<FollowUp> findOverduePlanned(@Param("today") LocalDate today);

    @Query("SELECT COUNT(f) FROM FollowUp f WHERE f.status = 'PLANNED' AND f.followUpDate >= :today AND (:assignedTo IS NULL OR f.assignedTo = :assignedTo)")
    long countPendingFromDate(@Param("today") LocalDate today, @Param("assignedTo") User assignedTo);
}
