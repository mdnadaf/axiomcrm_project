package com.acxiom.crm.repository;

import com.acxiom.crm.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) " +
           "AND (:entityName IS NULL OR LOWER(a.entityName) = LOWER(:entityName)) " +
           "AND (:action IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%',:action,'%'))) " +
           "AND (:fromDate IS NULL OR a.createdDate >= :fromDate) " +
           "AND (:toDate IS NULL OR a.createdDate <= :toDate)")
    Page<AuditLog> search(@Param("userId") Long userId,
                          @Param("entityName") String entityName,
                          @Param("action") String action,
                          @Param("fromDate") LocalDateTime fromDate,
                          @Param("toDate") LocalDateTime toDate,
                          Pageable pageable);

    Page<AuditLog> findByEntityNameAndRecordId(String entityName, Long recordId, Pageable pageable);
}
