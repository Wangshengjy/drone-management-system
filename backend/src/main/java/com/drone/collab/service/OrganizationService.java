package com.drone.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.collab.common.BusinessException;
import com.drone.collab.common.PageResult;
import com.drone.collab.domain.Statuses;
import com.drone.collab.dto.OrgSaveRequest;
import com.drone.collab.entity.OrgApplication;
import com.drone.collab.entity.Organization;
import com.drone.collab.mapper.OrgApplicationMapper;
import com.drone.collab.mapper.OrganizationMapper;
import com.drone.collab.security.SecurityUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class OrganizationService {

    private final OrganizationMapper organizationMapper;
    private final OrgApplicationMapper orgApplicationMapper;
    private final AutoReviewService autoReviewService;
    private final AuditLogService auditLogService;

    public OrganizationService(
            OrganizationMapper organizationMapper,
            OrgApplicationMapper orgApplicationMapper,
            AutoReviewService autoReviewService,
            AuditLogService auditLogService) {
        this.organizationMapper = organizationMapper;
        this.orgApplicationMapper = orgApplicationMapper;
        this.autoReviewService = autoReviewService;
        this.auditLogService = auditLogService;
    }

    public PageResult<Organization> page(String keyword, String orgType, String status, int page, int size, SecurityUser user) {
        if (!"ADMIN".equals(user.getRole()) && !"ORG".equals(user.getRole())) {
            throw new BusinessException(403, "无权限");
        }
        LambdaQueryWrapper<Organization> q = new LambdaQueryWrapper<>();
        if ("ORG".equals(user.getRole())) {
            q.eq(Organization::getId, user.getOrgId());
        } else {
            if (keyword != null && !keyword.isEmpty()) {
                q.and(w -> w.like(Organization::getName, keyword).or().like(Organization::getCreditCode, keyword));
            }
            if (orgType != null && !orgType.isEmpty()) {
                q.eq(Organization::getOrgType, orgType);
            }
        }
        if (status != null && !status.isEmpty()) {
            q.eq(Organization::getStatus, status);
        }
        q.orderByDesc(Organization::getUpdatedAt);
        Page<Organization> p = organizationMapper.selectPage(new Page<>(page, size), q);
        return new PageResult<>(p.getTotal(), p.getRecords());
    }

    public Organization get(Long id, SecurityUser user) {
        Organization o = organizationMapper.selectById(id);
        if (o == null) {
            throw new BusinessException("单位不存在");
        }
        assertOrgAccess(o, user);
        return o;
    }

    @Transactional
    public Organization create(OrgSaveRequest req, SecurityUser user) {
        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅管理员可创建");
        }
        LocalDateTime now = LocalDateTime.now();
        Organization o = new Organization();
        apply(req, o);
        o.setStatus(Statuses.DRAFT);
        o.setCreatedAt(now);
        o.setUpdatedAt(now);
        organizationMapper.insert(o);
        auditLogService.record("ORG", o.getId(), "CREATE", user.getUserId(), "管理员创建", null);
        return o;
    }

    @Transactional
    public Organization update(Long id, OrgSaveRequest req, SecurityUser user) {
        Organization o = organizationMapper.selectById(id);
        if (o == null) {
            throw new BusinessException("单位不存在");
        }
        assertOrgWrite(o, user);
        if (Statuses.FROZEN.equals(o.getStatus())) {
            throw new BusinessException("已冻结，不可修改");
        }
        apply(req, o);
        o.setUpdatedAt(LocalDateTime.now());
        organizationMapper.updateById(o);
        auditLogService.record("ORG", id, "UPDATE", user.getUserId(), "资料更新", null);
        return o;
    }

    @Transactional
    public void delete(Long id, SecurityUser user) {
        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅管理员可删除");
        }
        organizationMapper.deleteById(id);
        auditLogService.record("ORG", id, "DELETE", user.getUserId(), "删除", null);
    }

    @Transactional
    public void submit(Long id, SecurityUser user) {
        Organization o = organizationMapper.selectById(id);
        if (o == null) {
            throw new BusinessException("单位不存在");
        }
        if (!"ADMIN".equals(user.getRole())) {
            if (!Objects.equals(user.getOrgId(), id)) {
                throw new BusinessException(403, "只能提交本单位资料");
            }
        }
        if (!Statuses.DRAFT.equals(o.getStatus()) && !Statuses.REJECTED.equals(o.getStatus())) {
            throw new BusinessException("当前状态不可提交审核");
        }
        AutoReviewService.AutoOutcome out = autoReviewService.reviewOrganization(id);
        o.setAutoResult(out.autoResult());
        o.setAutoScore(out.score());
        o.setAutoReason(out.reasonJson());
        o.setUpdatedAt(LocalDateTime.now());
        if (Statuses.AUTO_REJECT.equals(out.autoResult())) {
            o.setStatus(Statuses.REJECTED);
        } else {
            o.setStatus(Statuses.PENDING_REVIEW);
        }
        organizationMapper.updateById(o);

        OrgApplication app = new OrgApplication();
        app.setOrgId(id);
        app.setApplyType("SUBMIT");
        app.setStatus(o.getStatus());
        app.setAutoResult(out.autoResult());
        app.setAutoScore(out.score());
        app.setAutoReason(out.reasonJson());
        app.setCreatedAt(LocalDateTime.now());
        orgApplicationMapper.insert(app);

        auditLogService.record("ORG", id, "SUBMIT_AUTO", user.getUserId(), "自动预审: " + out.autoResult(), out.reasonJson());
    }

    private static void apply(OrgSaveRequest req, Organization o) {
        if (req.getName() != null) {
            o.setName(req.getName());
        }
        if (req.getOrgType() != null) {
            o.setOrgType(req.getOrgType());
        }
        if (req.getCreditCode() != null) {
            o.setCreditCode(req.getCreditCode());
        }
        if (req.getContactName() != null) {
            o.setContactName(req.getContactName());
        }
        if (req.getPhone() != null) {
            o.setPhone(req.getPhone());
        }
        if (req.getAddress() != null) {
            o.setAddress(req.getAddress());
        }
    }

    private static void assertOrgAccess(Organization o, SecurityUser user) {
        if ("ADMIN".equals(user.getRole())) {
            return;
        }
        if ("ORG".equals(user.getRole()) && Objects.equals(user.getOrgId(), o.getId())) {
            return;
        }
        throw new BusinessException(403, "无权限查看");
    }

    private static void assertOrgWrite(Organization o, SecurityUser user) {
        if ("ADMIN".equals(user.getRole())) {
            return;
        }
        if ("ORG".equals(user.getRole()) && Objects.equals(user.getOrgId(), o.getId())) {
            return;
        }
        throw new BusinessException(403, "无权限修改");
    }
}
