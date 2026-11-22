package com.acxiom.crm.repository;

import com.acxiom.crm.entity.Activity;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.ActivityStatus;
import com.acxiom.crm.enums.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {

    @Query("SELECT a FROM Activity a WHERE " +
           "(:type IS NULL OR a.type = :type) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:assignedTo IS NULL OR a.assignedTo = :assignedTo) " +
           "AND (:fromDate IS NULL OR a.activityDate >= :fromDate) " +
           "AND (:toDate IS NULL OR a.activityDate <= :toDate)")
    Page<Activity> search(@Param("type") ActivityType type,
                          @Param("status") ActivityStatus status,
                          @Param("assignedTo") User assignedTo,
                          @Param("fromDate") LocalDateTime fromDate,
                          @Param("toDate") LocalDateTime toDate,
                          Pageable pageable);
}
