package com.drone.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.collab.common.BusinessException;
import com.drone.collab.common.PageResult;
import com.drone.collab.domain.Statuses;
import com.drone.collab.dto.CertificateSaveRequest;
import com.drone.collab.dto.PilotSaveRequest;
import com.drone.collab.entity.Pilot;
import com.drone.collab.entity.PilotApplication;
import com.drone.collab.entity.PilotCertificate;
import com.drone.collab.mapper.PilotApplicationMapper;
import com.drone.collab.mapper.PilotCertificateMapper;
import com.drone.collab.mapper.PilotMapper;
import com.drone.collab.security.SecurityUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class PilotService {

    private final PilotMapper pilotMapper;
    private final PilotCertificateMapper pilotCertificateMapper;
    private final PilotApplicationMapper pilotApplicationMapper;
    private final AutoReviewService autoReviewService;
    private final AuditLogService auditLogService;
    private final com.drone.collab.mapper.SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    public PilotService(
            PilotMapper pilotMapper,
            PilotCertificateMapper pilotCertificateMapper,
            PilotApplicationMapper pilotApplicationMapper,
            AutoReviewService autoReviewService,
            AuditLogService auditLogService,
            com.drone.collab.mapper.SysUserMapper sysUserMapper,
            NotificationService notificationService) {
        this.pilotMapper = pilotMapper;
        this.pilotCertificateMapper = pilotCertificateMapper;
        this.pilotApplicationMapper = pilotApplicationMapper;
        this.autoReviewService = autoReviewService;
        this.auditLogService = auditLogService;
        this.sysUserMapper = sysUserMapper;
        this.notificationService = notificationService;
    }

    public PageResult<Pilot> page(String keyword, String region, String status, int page, int size, SecurityUser user) {
        if (!"ADMIN".equals(user.getRole()) && !"PILOT".equals(user.getRole())) {
            throw new BusinessException(403, "无权限");
        }
        LambdaQueryWrapper<Pilot> q = new LambdaQueryWrapper<>();
        if ("PILOT".equals(user.getRole())) {
            q.eq(Pilot::getId, user.getPilotId());
        } else {
            if (keyword != null && !keyword.isEmpty()) {
                q.and(w -> w.like(Pilot::getFullName, keyword).or().like(Pilot::getRecordNo, keyword));
            }
            if (region != null && !region.isEmpty()) {
                q.eq(Pilot::getRegion, region);
            }
        }
        if (status != null && !status.isEmpty()) {
            q.eq(Pilot::getStatus, status);
        }
        q.orderByDesc(Pilot::getUpdatedAt);
        Page<Pilot> p = pilotMapper.selectPage(new Page<>(page, size), q);
        return new PageResult<>(p.getTotal(), p.getRecords());
    }

    public Pilot getMe(SecurityUser user) {
        if (!"PILOT".equals(user.getRole()) || user.getPilotId() == null) {
            throw new BusinessException(403, "非飞手账号");
        }
        return get(user.getPilotId(), user);
    }

    public Pilot get(Long id, SecurityUser user) {
        Pilot pilot = pilotMapper.selectById(id);
        if (pilot == null) {
            throw new BusinessException("飞手不存在");
        }
        assertPilotAccess(pilot, user);
        return pilot;
    }

    @Transactional
    public Pilot create(PilotSaveRequest req, SecurityUser user) {
        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅管理员可创建");
        }
        LocalDateTime now = LocalDateTime.now();
        Pilot p = new Pilot();
        apply(req, p);
        p.setStatus(Statuses.DRAFT);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        pilotMapper.insert(p);
        auditLogService.record("PILOT", p.getId(), "CREATE", user.getUserId(), "管理员创建", null);
        return p;
    }

    @Transactional
    public Pilot update(Long id, PilotSaveRequest req, SecurityUser user) {
        Pilot p = pilotMapper.selectById(id);
        if (p == null) {
            throw new BusinessException("飞手不存在");
        }
        assertPilotWrite(p, user);
        if (Statuses.FROZEN.equals(p.getStatus())) {
            throw new BusinessException("已冻结，不可修改");
        }
        apply(req, p);
        p.setUpdatedAt(LocalDateTime.now());
        pilotMapper.updateById(p);
        auditLogService.record("PILOT", id, "UPDATE", user.getUserId(), "资料更新", null);
        return p;
    }

    @Transactional
    public void delete(Long id, SecurityUser user) {
        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅管理员可删除");
        }
        pilotCertificateMapper.delete(new LambdaQueryWrapper<PilotCertificate>().eq(PilotCertificate::getPilotId, id));
        pilotMapper.deleteById(id);
        auditLogService.record("PILOT", id, "DELETE", user.getUserId(), "删除", null);
    }

    @Transactional
    public void submit(Long id, SecurityUser user) {
        Pilot p = pilotMapper.selectById(id);
        if (p == null) {
            throw new BusinessException("飞手不存在");
        }
        if (!"ADMIN".equals(user.getRole())) {
            if (!Objects.equals(user.getPilotId(), id)) {
                throw new BusinessException(403, "只能提交本人资料");
            }
        }
        if (!Statuses.DRAFT.equals(p.getStatus()) && !Statuses.REJECTED.equals(p.getStatus())) {
            throw new BusinessException("当前状态不可提交审核");
        }
        AutoReviewService.AutoOutcome out = autoReviewService.reviewPilot(id);
        p.setAutoResult(out.autoResult());
        p.setAutoScore(out.score());
        p.setAutoReason(out.reasonJson());
        p.setUpdatedAt(LocalDateTime.now());

        if (Statuses.AUTO_REJECT.equals(out.autoResult())) {
            p.setStatus(Statuses.REJECTED);
        } else {
            p.setStatus(Statuses.PENDING_REVIEW);
        }
        pilotMapper.updateById(p);

        PilotApplication app = new PilotApplication();
        app.setPilotId(id);
        app.setApplyType("SUBMIT");
        app.setStatus(p.getStatus());
        app.setAutoResult(out.autoResult());
        app.setAutoScore(out.score());
        app.setAutoReason(out.reasonJson());
        app.setCreatedAt(LocalDateTime.now());
        pilotApplicationMapper.insert(app);

        auditLogService.record(
                "PILOT",
                id,
                "SUBMIT_AUTO",
                user.getUserId(),
                "自动预审: " + out.autoResult(),
                out.reasonJson());
    }

    public PageResult<PilotCertificate> listCerts(Long pilotId, SecurityUser user) {
        get(pilotId, user);
        Page<PilotCertificate> p = pilotCertificateMapper.selectPage(
                new Page<>(1, 500),
                new LambdaQueryWrapper<PilotCertificate>()
                        .eq(PilotCertificate::getPilotId, pilotId)
                        .orderByDesc(PilotCertificate::getCreatedAt));
        return new PageResult<>(p.getTotal(), p.getRecords());
    }

    @Transactional
    public PilotCertificate addCert(Long pilotId, CertificateSaveRequest req, SecurityUser user) {
        Pilot pilot = get(pilotId, user);
        if (!Statuses.DRAFT.equals(pilot.getStatus()) && !Statuses.REJECTED.equals(pilot.getStatus())
                && !Statuses.APPROVED.equals(pilot.getStatus())) {
            throw new BusinessException("当前状态不可编辑资质");
        }
        PilotCertificate c = new PilotCertificate();
        c.setPilotId(pilotId);
        c.setCertType(req.getCertType());
        c.setCertNo(req.getCertNo());
        c.setValidUntil(req.getValidUntil());
        c.setAttachmentUrl(req.getAttachmentUrl());
        c.setCreatedAt(LocalDateTime.now());
        pilotCertificateMapper.insert(c);
        auditLogService.record("PILOT", pilotId, "CERT_ADD", user.getUserId(), req.getCertType(), null);

        // 上传证书后自动触发重新评分
        AutoReviewService.AutoOutcome out = autoReviewService.reviewPilot(pilotId);
        pilot.setAutoResult(out.autoResult());
        pilot.setAutoScore(out.score());
        pilot.setAutoReason(out.reasonJson());
        pilot.setUpdatedAt(LocalDateTime.now());

        if (Statuses.AUTO_PASS.equals(out.autoResult()) && out.score() >= 70) {
            // 达到70分自动通过
            pilot.setStatus(Statuses.APPROVED);
            pilotMapper.updateById(pilot);
            auditLogService.record("PILOT", pilotId, "SUBMIT_AUTO", user.getUserId(),
                    "上传证书后自动预审通过(分数:" + out.score() + ")", out.reasonJson());
            // 通知管理员
            notifyAdmins(pilotId, "飞手证书更新自动通过", pilot.getFullName() + "上传证书后自动预审通过，分数:" + out.score());
        } else {
            pilotMapper.updateById(pilot);
        }

        return c;
    }

    private void notifyAdmins(Long pilotId, String title, String content) {
        java.util.List<com.drone.collab.entity.SysUser> admins = sysUserMapper.selectList(
                new LambdaQueryWrapper<com.drone.collab.entity.SysUser>()
                        .eq(com.drone.collab.entity.SysUser::getRole, "ADMIN")
                        .eq(com.drone.collab.entity.SysUser::getEnabled, 1));
        for (com.drone.collab.entity.SysUser admin : admins) {
            notificationService.sendToUser(admin.getId(), "PILOT", pilotId, title, content, "REVIEW");
        }
    }

    @Transactional
    public PilotCertificate updateCert(Long pilotId, Long certId, CertificateSaveRequest req, SecurityUser user) {
        get(pilotId, user);
        PilotCertificate c = pilotCertificateMapper.selectById(certId);
        if (c == null || !Objects.equals(c.getPilotId(), pilotId)) {
            throw new BusinessException("证书不存在");
        }
        c.setCertType(req.getCertType());
        c.setCertNo(req.getCertNo());
        c.setValidUntil(req.getValidUntil());
        c.setAttachmentUrl(req.getAttachmentUrl());
        pilotCertificateMapper.updateById(c);
        return c;
    }

    @Transactional
    public void deleteCert(Long pilotId, Long certId, SecurityUser user) {
        get(pilotId, user);
        PilotCertificate c = pilotCertificateMapper.selectById(certId);
        if (c == null || !Objects.equals(c.getPilotId(), pilotId)) {
            throw new BusinessException("证书不存在");
        }
        pilotCertificateMapper.deleteById(certId);
    }

    private static void apply(PilotSaveRequest req, Pilot p) {
        if (req.getFullName() != null) {
            p.setFullName(req.getFullName());
        }
        if (req.getIdCard() != null) {
            p.setIdCard(req.getIdCard());
        }
        if (req.getPhone() != null) {
            p.setPhone(req.getPhone());
        }
        if (req.getEmail() != null) {
            p.setEmail(req.getEmail());
        }
        if (req.getRecordNo() != null) {
            p.setRecordNo(req.getRecordNo());
        }
        if (req.getRegion() != null) {
            p.setRegion(req.getRegion());
        }
    }

    private static void assertPilotAccess(Pilot p, SecurityUser user) {
        if ("ADMIN".equals(user.getRole())) {
            return;
        }
        if ("PILOT".equals(user.getRole()) && Objects.equals(user.getPilotId(), p.getId())) {
            return;
        }
        throw new BusinessException(403, "无权限查看");
    }

    private static void assertPilotWrite(Pilot p, SecurityUser user) {
        if ("ADMIN".equals(user.getRole())) {
            return;
        }
        if ("PILOT".equals(user.getRole()) && Objects.equals(user.getPilotId(), p.getId())) {
            return;
        }
        throw new BusinessException(403, "无权限修改");
    }
}
