package com.drone.collab.service;

import com.drone.collab.common.BusinessException;
import com.drone.collab.domain.Statuses;
import com.drone.collab.dto.ManualReviewRequest;
import com.drone.collab.entity.OrgStatus;
import com.drone.collab.entity.Organization;
import com.drone.collab.entity.Pilot;
import com.drone.collab.entity.PilotStatus;
import com.drone.collab.mapper.OrgStatusMapper;
import com.drone.collab.mapper.OrganizationMapper;
import com.drone.collab.mapper.PilotMapper;
import com.drone.collab.mapper.PilotStatusMapper;
import com.drone.collab.security.SecurityUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReviewService {

    private final PilotMapper pilotMapper;
    private final OrganizationMapper organizationMapper;
    private final AuditLogService auditLogService;
    private final PilotStatusMapper pilotStatusMapper;
    private final OrgStatusMapper orgStatusMapper;

    public ReviewService(
            PilotMapper pilotMapper,
            OrganizationMapper organizationMapper,
            AuditLogService auditLogService,
            PilotStatusMapper pilotStatusMapper,
            OrgStatusMapper orgStatusMapper) {
        this.pilotMapper = pilotMapper;
        this.organizationMapper = organizationMapper;
        this.auditLogService = auditLogService;
        this.pilotStatusMapper = pilotStatusMapper;
        this.orgStatusMapper = orgStatusMapper;
    }

    @Transactional
    public void reviewPilot(Long id, ManualReviewRequest req, SecurityUser user) {
        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅管理员可复核");
        }
        Pilot p = pilotMapper.selectById(id);
        if (p == null) {
            throw new BusinessException("飞手不存在");
        }
        if (!Statuses.PENDING_REVIEW.equals(p.getStatus())) {
            throw new BusinessException("当前状态不可人工复核");
        }
        String d = req.getDecision().toUpperCase();
        if ("APPROVE".equals(d) || "PASS".equals(d)) {
            p.setStatus(Statuses.APPROVED);
            ensurePilotStatus(p.getId());
        } else if ("REJECT".equals(d)) {
            p.setStatus(Statuses.REJECTED);
        } else {
            throw new BusinessException("decision 需为 APPROVE 或 REJECT");
        }
        p.setUpdatedAt(LocalDateTime.now());
        pilotMapper.updateById(p);
        auditLogService.record(
                "PILOT",
                id,
                "MANUAL_REVIEW",
                user.getUserId(),
                req.getComment(),
                "{\"decision\":\"" + d + "\"}");
    }

    @Transactional
    public void reviewOrg(Long id, ManualReviewRequest req, SecurityUser user) {
        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅管理员可复核");
        }
        Organization o = organizationMapper.selectById(id);
        if (o == null) {
            throw new BusinessException("单位不存在");
        }
        if (!Statuses.PENDING_REVIEW.equals(o.getStatus())) {
            throw new BusinessException("当前状态不可人工复核");
        }
        String d = req.getDecision().toUpperCase();
        if ("APPROVE".equals(d) || "PASS".equals(d)) {
            o.setStatus(Statuses.APPROVED);
            ensureOrgStatus(o.getId());
        } else if ("REJECT".equals(d)) {
            o.setStatus(Statuses.REJECTED);
        } else {
            throw new BusinessException("decision 需为 APPROVE 或 REJECT");
        }
        o.setUpdatedAt(LocalDateTime.now());
        organizationMapper.updateById(o);
        auditLogService.record(
                "ORG",
                id,
                "MANUAL_REVIEW",
                user.getUserId(),
                req.getComment(),
                "{\"decision\":\"" + d + "\"}");
    }

    @Transactional
    public void freezePilot(Long id, SecurityUser user) {
        requireAdmin(user);
        Pilot p = pilotMapper.selectById(id);
        if (p == null) {
            throw new BusinessException("飞手不存在");
        }
        p.setStatus(Statuses.FROZEN);
        p.setUpdatedAt(LocalDateTime.now());
        pilotMapper.updateById(p);
        auditLogService.record("PILOT", id, "FREEZE", user.getUserId(), "冻结", null);
    }

    @Transactional
    public void freezeOrg(Long id, SecurityUser user) {
        requireAdmin(user);
        Organization o = organizationMapper.selectById(id);
        if (o == null) {
            throw new BusinessException("单位不存在");
        }
        o.setStatus(Statuses.FROZEN);
        o.setUpdatedAt(LocalDateTime.now());
        organizationMapper.updateById(o);
        auditLogService.record("ORG", id, "FREEZE", user.getUserId(), "冻结", null);
    }

    private static void requireAdmin(SecurityUser user) {
        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅管理员可操作");
        }
    }

    private void ensurePilotStatus(Long pilotId) {
        PilotStatus existing = pilotStatusMapper.selectOne(
                new LambdaQueryWrapper<PilotStatus>().eq(PilotStatus::getPilotId, pilotId));
        if (existing == null) {
            PilotStatus ps = new PilotStatus();
            ps.setPilotId(pilotId);
            ps.setAvailability("IDLE");
            ps.setMissionCount(0);
            ps.setUpdatedAt(LocalDateTime.now());
            ps.setCreatedAt(LocalDateTime.now());
            pilotStatusMapper.insert(ps);
        }
    }

    private void ensureOrgStatus(Long orgId) {
        OrgStatus existing = orgStatusMapper.selectOne(
                new LambdaQueryWrapper<OrgStatus>().eq(OrgStatus::getOrgId, orgId));
        if (existing == null) {
            OrgStatus os = new OrgStatus();
            os.setOrgId(orgId);
            os.setAvailability("IDLE");
            os.setMissionCount(0);
            os.setUpdatedAt(LocalDateTime.now());
            os.setCreatedAt(LocalDateTime.now());
            orgStatusMapper.insert(os);
        }
    }
}
