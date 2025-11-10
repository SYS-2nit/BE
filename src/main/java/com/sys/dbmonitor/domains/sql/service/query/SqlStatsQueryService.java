package com.sys.dbmonitor.domains.sql.service.query;

import com.sys.dbmonitor.domains.sql.domain.Sql;
import com.sys.dbmonitor.domains.sql.dto.request.SqlGraphRequest;
import com.sys.dbmonitor.domains.sql.dto.request.SqlStatsQueryRequest;
import com.sys.dbmonitor.domains.sql.dto.response.SqlGraphSeriesResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlRowResponse;
import com.sys.dbmonitor.domains.sql.dto.response.SqlStatsPageResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class SqlStatsQueryService {

	@PersistenceContext
	private EntityManager em;

	public SqlStatsPageResponse getStats(SqlStatsQueryRequest req) {
		LocalDate start = req.startDate();
		LocalDate end = req.endDate();
		if (start == null && end == null) {
			end = LocalDate.now();
			start = end.minusDays(1);
		} else if (start == null) {
			start = Objects.requireNonNull(end).minusDays(1);
		} else if (end == null) {
			end = start.plusDays(1);
		}

		LocalDateTime from = start.atStartOfDay();
		LocalDateTime to = end.atStartOfDay();

		StringBuilder jpql = new StringBuilder("SELECT s FROM Sql s WHERE s.instanceId = :instanceId AND s.createdAt >= :from AND s.createdAt < :to AND s.isDeleted = false");
		StringBuilder countJpql = new StringBuilder("SELECT COUNT(s) FROM Sql s WHERE s.instanceId = :instanceId AND s.createdAt >= :from AND s.createdAt < :to AND s.isDeleted = false");

		if (StringUtils.hasText(req.keyword())) {
			jpql.append(" AND UPPER(s.sqlText) LIKE :kw");
			countJpql.append(" AND UPPER(s.sqlText) LIKE :kw");
		}
		if (req.minExecCount() != null) {
			jpql.append(" AND s.executionsDelta >= :minExec");
			countJpql.append(" AND s.executionsDelta >= :minExec");
		}
		if (req.maxExecCount() != null) {
			jpql.append(" AND s.executionsDelta <= :maxExec");
			countJpql.append(" AND s.executionsDelta <= :maxExec");
		}

		String orderBy = resolveOrderBy(req.orderBy());
		String direction = ("ASC".equalsIgnoreCase(req.direction())) ? "ASC" : "DESC";
		jpql.append(" ORDER BY ").append(orderBy).append(" ").append(direction);

		TypedQuery<Sql> query = em.createQuery(jpql.toString(), Sql.class)
				.setParameter("instanceId", req.instanceId())
				.setParameter("from", from)
				.setParameter("to", to);
		TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class)
				.setParameter("instanceId", req.instanceId())
				.setParameter("from", from)
				.setParameter("to", to);

		if (StringUtils.hasText(req.keyword())) {
			String like = "%" + req.keyword().toUpperCase() + "%";
			query.setParameter("kw", like);
			countQuery.setParameter("kw", like);
		}
		if (req.minExecCount() != null) {
			query.setParameter("minExec", req.minExecCount());
			countQuery.setParameter("minExec", req.minExecCount());
		}
		if (req.maxExecCount() != null) {
			query.setParameter("maxExec", req.maxExecCount());
			countQuery.setParameter("maxExec", req.maxExecCount());
		}

		int page = req.page() != null ? req.page() : 0;
		int size = req.size() != null ? req.size() : 20;
		query.setFirstResult(page * size);
		query.setMaxResults(size);

		List<SqlRowResponse> rows = query.getResultList().stream().map(SqlRowResponse::from).collect(Collectors.toList());
		long total = countQuery.getSingleResult();
		int totalPages = (int) Math.ceil((double) total / size);

		return new SqlStatsPageResponse(rows, total, totalPages, page, size);
	}

	public SqlGraphSeriesResponse getGraph(SqlGraphRequest req) {
		LocalDate base = req.baseDate() != null ? req.baseDate() : LocalDate.now();
		LocalDateTime from = base.atStartOfDay();
		LocalDateTime to = base.plusDays(1).atStartOfDay();

		// metric column mapping
		String metricCol = resolveMetricColumn(req.metric());

		// Native query: 시간단위(HH24) SUM 집계
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT TRUNC(created_at, 'HH24') AS t, SUM(").append(metricCol).append(") AS v ")
				.append("FROM SQL_DATA ")
				.append("WHERE instance_id = :instanceId AND is_deleted = 0 AND created_at >= :from AND created_at < :to ");
		if (req.sqlId() != null) {
			sql.append("AND sql_id = :sqlId ");
		}
		if (StringUtils.hasText(req.keyword())) {
			sql.append("AND UPPER(sql_text) LIKE :kw ");
		}
		sql.append("GROUP BY TRUNC(created_at, 'HH24') ORDER BY t");

		var nq = em.createNativeQuery(sql.toString())
				.setParameter("instanceId", req.instanceId())
				.setParameter("from", from)
				.setParameter("to", to);
		if (req.sqlId() != null) nq.setParameter("sqlId", req.sqlId());
		if (StringUtils.hasText(req.keyword())) nq.setParameter("kw", "%" + req.keyword().toUpperCase() + "%");

		@SuppressWarnings("unchecked")
		List<Object[]> rows = nq.getResultList();
		Map<LocalDateTime, Long> bucketToValue = new HashMap<>();
		for (Object[] r : rows) {
			LocalDateTime bucket = ((java.sql.Timestamp) r[0]).toLocalDateTime();
			Number v = (Number) r[1];
			bucketToValue.put(bucket, v != null ? v.longValue() : 0L);
		}

		List<SqlGraphSeriesResponse.Point> series = new ArrayList<>();
		ZoneId zone = ZoneId.systemDefault();
		for (int h = 0; h < 24; h++) {
			LocalDateTime bucket = from.plusHours(h);
			long v = bucketToValue.getOrDefault(bucket, 0L);
			Instant t = bucket.atZone(zone).toInstant();
			series.add(new SqlGraphSeriesResponse.Point(t, v));
		}
		return new SqlGraphSeriesResponse(req.metric() != null ? req.metric() : "elapsed", "hour", series);
	}

	private String resolveOrderBy(String orderBy) {
		if (orderBy == null) return "s.elapsedUsDelta";
		return switch (orderBy.toLowerCase()) {
			case "cpu" -> "s.cpuUsDelta";
			case "exec" -> "s.executionsDelta";
			case "logical" -> "s.bufferGetsDelta";
			case "physical" -> "s.diskReadsDelta";
			case "wait" -> "s.waitTimeUsDelta";
			case "elapsed" -> "s.elapsedUsDelta";
			default -> "s.elapsedUsDelta";
		};
	}

	private String resolveMetricColumn(String metric) {
		if (metric == null) return "elapsed_us_delta";
		return switch (metric.toLowerCase()) {
			case "cpu" -> "cpu_us_delta";
			case "exec" -> "executions_delta";
			case "logical" -> "buffer_gets_delta";
			case "physical" -> "disk_reads_delta";
			case "wait" -> "wait_time_us_delta";
			case "elapsed" -> "elapsed_us_delta";
			default -> "elapsed_us_delta";
		};
	}
}


