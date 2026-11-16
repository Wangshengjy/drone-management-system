package com.drone.collab.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatsMapper {

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m') AS month_label, COUNT(*) AS cnt
            FROM pilot WHERE status = 'APPROVED'
            AND created_at >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
            GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month_label
            """)
    List<Map<String, Object>> pilotApprovedByMonth();

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m') AS month_label, COUNT(*) AS cnt
            FROM organization WHERE status = 'APPROVED'
            AND created_at >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
            GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month_label
            """)
    List<Map<String, Object>> orgApprovedByMonth();

    @Select("SELECT org_type AS name, COUNT(*) AS value FROM organization WHERE status = 'APPROVED' GROUP BY org_type")
    List<Map<String, Object>> orgTypeDistribution();

    @Select("SELECT cert_type AS name, COUNT(*) AS value FROM pilot_certificate pc " +
            "INNER JOIN pilot p ON p.id = pc.pilot_id WHERE p.status = 'APPROVED' GROUP BY cert_type")
    List<Map<String, Object>> pilotCertDistribution();

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM pilot WHERE status = 'APPROVED') AS approved_pilots,
              (SELECT COUNT(*) FROM pilot WHERE status = 'PENDING_REVIEW') AS pending_pilots,
              (SELECT COUNT(*) FROM organization WHERE status = 'APPROVED') AS approved_orgs,
              (SELECT COUNT(*) FROM organization WHERE status = 'PENDING_REVIEW') AS pending_orgs
            """)
    Map<String, Object> reviewCounts();

    @Select("""
            SELECT
              ROUND(100 * SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 2) AS rate
            FROM pilot WHERE status IN ('APPROVED','REJECTED')
            """)
    Double pilotApprovalRate();

    @Select("""
            SELECT
              ROUND(100 * SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 2) AS rate
            FROM organization WHERE status IN ('APPROVED','REJECTED')
            """)
    Double orgApprovalRate();
}
