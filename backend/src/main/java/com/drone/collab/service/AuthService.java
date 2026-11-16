package com.drone.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.collab.common.BusinessException;
import com.drone.collab.domain.Statuses;
import com.drone.collab.dto.LoginRequest;
import com.drone.collab.dto.RegisterRequest;
import com.drone.collab.entity.OrgApplication;
import com.drone.collab.entity.Organization;
import com.drone.collab.entity.Pilot;
import com.drone.collab.entity.PilotApplication;
import com.drone.collab.entity.SysUser;
import com.drone.collab.mapper.OrgApplicationMapper;
import com.drone.collab.mapper.OrganizationMapper;
import com.drone.collab.mapper.PilotApplicationMapper;
import com.drone.collab.mapper.PilotMapper;
import com.drone.collab.mapper.SysUserMapper;
import com.drone.collab.security.JwtTokenProvider;
import com.drone.collab.vo.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PilotMapper pilotMapper;
    private final OrganizationMapper organizationMapper;
    private final PilotApplicationMapper pilotApplicationMapper;
    private final OrgApplicationMapper orgApplicationMapper;
    private final AutoReviewService autoReviewService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            SysUserMapper sysUserMapper,
            PilotMapper pilotMapper,
            OrganizationMapper organizationMapper,
            PilotApplicationMapper pilotApplicationMapper,
            OrgApplicationMapper orgApplicationMapper,
            AutoReviewService autoReviewService,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.sysUserMapper = sysUserMapper;
        this.pilotMapper = pilotMapper;
        this.organizationMapper = organizationMapper;
        this.pilotApplicationMapper = pilotApplicationMapper;
        this.orgApplicationMapper = orgApplicationMapper;
        this.autoReviewService = autoReviewService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest req) {
        SysUser u = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (u == null || u.getEnabled() != null && u.getEnabled() == 0) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        String token = jwtTokenProvider.createToken(
                u.getUsername(), u.getRole(), u.getId(), u.getPilotId(), u.getOrgId());
        return new LoginResponse(token, u.getUsername(), u.getRole(), u.getId(), u.getPilotId(), u.getOrgId());
    }

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        long c = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (c > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (("PILOT".equals(req.getRole()) || "ORG".equals(req.getRole()))
                && isBlank(req.getDisplayName())) {
            throw new BusinessException("请填写姓名或单位名称");
        }

        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setUsername(req.getUsername().trim());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        user.setEnabled(1);
        user.setCreatedAt(now);

        if ("PILOT".equals(req.getRole())) {
            validatePilotRegister(req);

            Pilot p = new Pilot();
            p.setFullName(req.getDisplayName().trim());
            p.setPhone(trimToNull(req.getPhone()));
            p.setEmail(trimToNull(req.getEmail()));
            p.setIdCard(trimToNull(req.getIdCard()));
            p.setRecordNo(trimToNull(req.getRecordNo()));
            p.setRegion(trimToNull(req.getRegion()));
            p.setStatus(Statuses.DRAFT);
            p.setCreatedAt(now);
            p.setUpdatedAt(now);
            pilotMapper.insert(p);
            user.setPilotId(p.getId());
        } else if ("ORG".equals(req.getRole())) {
            validateOrgRegister(req);

            Organization o = new Organization();
            o.setName(req.getDisplayName().trim());
            o.setOrgType(isBlank(req.getOrgType()) ? "OTHER" : req.getOrgType().trim());
            o.setCreditCode(trimToNull(req.getCreditCode()));
            o.setContactName(trimToNull(req.getContactName()));
            o.setPhone(trimToNull(req.getPhone()));
            o.setAddress(trimToNull(req.getAddress()));
            o.setStatus(Statuses.DRAFT);
            o.setCreatedAt(now);
            o.setUpdatedAt(now);
            organizationMapper.insert(o);
            user.setOrgId(o.getId());
        } else if ("GUEST".equals(req.getRole())) {
            // guest keeps access only to public queries
        } else {
            throw new BusinessException("不支持的角色类型");
        }

        sysUserMapper.insert(user);

        if ("PILOT".equals(req.getRole())) {
            Pilot p = pilotMapper.selectById(user.getPilotId());
            p.setUserId(user.getId());
            submitPilotOnRegister(p, now);
        } else if ("ORG".equals(req.getRole())) {
            Organization o = organizationMapper.selectById(user.getOrgId());
            o.setUserId(user.getId());
            submitOrgOnRegister(o, now);
        }

        String token = jwtTokenProvider.createToken(
                user.getUsername(), user.getRole(), user.getId(), user.getPilotId(), user.getOrgId());
        return new LoginResponse(token, user.getUsername(), user.getRole(), user.getId(), user.getPilotId(), user.getOrgId());
    }

    private void submitPilotOnRegister(Pilot p, LocalDateTime now) {
        AutoReviewService.AutoOutcome out = autoReviewService.reviewPilot(p.getId());
        p.setAutoResult(out.autoResult());
        p.setAutoScore(out.score());
        p.setAutoReason(out.reasonJson());
        p.setUpdatedAt(now);
        p.setStatus(Statuses.AUTO_REJECT.equals(out.autoResult()) ? Statuses.REJECTED : Statuses.PENDING_REVIEW);
        pilotMapper.updateById(p);

        PilotApplication app = new PilotApplication();
        app.setPilotId(p.getId());
        app.setApplyType("REGISTER");
        app.setStatus(p.getStatus());
        app.setAutoResult(out.autoResult());
        app.setAutoScore(out.score());
        app.setAutoReason(out.reasonJson());
        app.setCreatedAt(now);
        pilotApplicationMapper.insert(app);
    }

    private void submitOrgOnRegister(Organization o, LocalDateTime now) {
        AutoReviewService.AutoOutcome out = autoReviewService.reviewOrganization(o.getId());
        o.setAutoResult(out.autoResult());
        o.setAutoScore(out.score());
        o.setAutoReason(out.reasonJson());
        o.setUpdatedAt(now);
        o.setStatus(Statuses.AUTO_REJECT.equals(out.autoResult()) ? Statuses.REJECTED : Statuses.PENDING_REVIEW);
        organizationMapper.updateById(o);

        OrgApplication app = new OrgApplication();
        app.setOrgId(o.getId());
        app.setApplyType("REGISTER");
        app.setStatus(o.getStatus());
        app.setAutoResult(out.autoResult());
        app.setAutoScore(out.score());
        app.setAutoReason(out.reasonJson());
        app.setCreatedAt(now);
        orgApplicationMapper.insert(app);
    }

    private static void validatePilotRegister(RegisterRequest req) {
        if (isBlank(req.getPhone()) || isBlank(req.getRecordNo())) {
            throw new BusinessException("飞手报名请填写 手机和备案号");
        }
    }

    private static void validateOrgRegister(RegisterRequest req) {
        if (isBlank(req.getPhone()) || isBlank(req.getCreditCode())) {
            throw new BusinessException("单位报名请填写 手机和统一社会信用代码");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
