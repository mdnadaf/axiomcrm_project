package com.acxiom.crm.repository;

import com.acxiom.crm.entity.Opportunity;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.OpportunityStage;
import com.acxiom.crm.enums.OpportunityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {

    @Query("SELECT o FROM Opportunity o WHERE " +
           "(:search IS NULL OR LOWER(o.name) LIKE LOWER(CONCAT('%',:search,'%'))) " +
           "AND (:stage IS NULL OR o.stage = :stage) " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:assignedTo IS NULL OR o.assignedTo = :assignedTo)")
    Page<Opportunity> search(@Param("search") String search,
                             @Param("stage") OpportunityStage stage,
                             @Param("status") OpportunityStatus status,
                             @Param("assignedTo") User assignedTo,
                             Pageable pageable);

    @Query("SELECT SUM(o.amount) FROM Opportunity o WHERE o.status = 'OPEN' AND (:assignedTo IS NULL OR o.assignedTo = :assignedTo)")
    BigDecimal sumOpenPipeline(@Param("assignedTo") User assignedTo);

    @Query("SELECT SUM(o.weightedValue) FROM Opportunity o WHERE o.status = 'OPEN' AND (:assignedTo IS NULL OR o.assignedTo = :assignedTo)")
    BigDecimal sumWeightedPipeline(@Param("assignedTo") User assignedTo);

    @Query("SELECT o.stage, COUNT(o), SUM(o.amount) FROM Opportunity o WHERE (:assignedTo IS NULL OR o.assignedTo = :assignedTo) GROUP BY o.stage")
    List<Object[]> pipelineByStage(@Param("assignedTo") User assignedTo);

    @Query("SELECT COUNT(o) FROM Opportunity o WHERE o.status = :status AND (:assignedTo IS NULL OR o.assignedTo = :assignedTo)")
    long countByStatusAndAssignedTo(@Param("status") OpportunityStatus status, @Param("assignedTo") User assignedTo);

    @Query("SELECT MONTH(o.updatedDate), SUM(o.amount) FROM Opportunity o WHERE o.status = 'WON' " +
           "AND YEAR(o.updatedDate) = :year AND (:assignedTo IS NULL OR o.assignedTo = :assignedTo) GROUP BY MONTH(o.updatedDate)")
    List<Object[]> monthlySales(@Param("year") int year, @Param("assignedTo") User assignedTo);
}
