package com.drone.collab.service;

import com.drone.collab.mapper.StatsMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsApiService {

    private final StatsMapper statsMapper;

    public StatsApiService(StatsMapper statsMapper) {
        this.statsMapper = statsMapper;
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> m = new HashMap<>();
        m.put("pilotApprovedByMonth", statsMapper.pilotApprovedByMonth());
        m.put("orgApprovedByMonth", statsMapper.orgApprovedByMonth());
        m.put("orgTypeDistribution", statsMapper.orgTypeDistribution());
        m.put("pilotCertDistribution", statsMapper.pilotCertDistribution());
        m.put("reviewCounts", statsMapper.reviewCounts());
        m.put("pilotApprovalRate", statsMapper.pilotApprovalRate());
        m.put("orgApprovalRate", statsMapper.orgApprovalRate());
        return m;
    }

    public List<Map<String, Object>> pilotMonthly() {
        return statsMapper.pilotApprovedByMonth();
    }

    public List<Map<String, Object>> orgMonthly() {
        return statsMapper.orgApprovedByMonth();
    }
}
