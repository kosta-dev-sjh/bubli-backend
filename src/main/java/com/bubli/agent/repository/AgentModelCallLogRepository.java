package com.bubli.agent.repository;

import com.bubli.agent.entity.AgentModelCallLog;
import com.bubli.agent.type.AgentJobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AgentModelCallLogRepository extends JpaRepository<AgentModelCallLog, UUID> {

	@Query("""
			select count(log)
			from AgentModelCallLog log
			join AgentJob job on job.id = log.jobId
			where job.requestedByUserId = :userId
			  and log.createdAt >= :since
			""")
	long countByUserSince(@Param("userId") UUID userId, @Param("since") Instant since);

	@Query("""
			select count(log)
			from AgentModelCallLog log
			join AgentJob job on job.id = log.jobId
			where job.requestedByUserId = :userId
			  and job.jobType = :jobType
			  and log.createdAt >= :since
			""")
	long countByUserAndJobTypeSince(
			@Param("userId") UUID userId,
			@Param("jobType") AgentJobType jobType,
			@Param("since") Instant since
	);
}
