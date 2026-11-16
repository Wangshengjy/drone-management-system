package com.drone.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.collab.common.PageResult;
import com.drone.collab.entity.Organization;
import com.drone.collab.entity.Pilot;
import com.drone.collab.entity.PilotCertificate;
import com.drone.collab.mapper.OrganizationMapper;
import com.drone.collab.mapper.PilotCertificateMapper;
import com.drone.collab.mapper.PilotMapper;
import com.drone.collab.util.DesensitizeUtil;
import com.drone.collab.vo.OrgPublicVo;
import com.drone.collab.vo.PilotPublicVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PublicService {

    private final PilotMapper pilotMapper;
    private final OrganizationMapper organizationMapper;
    private final PilotCertificateMapper certMapper;

    public PublicService(PilotMapper pilotMapper, OrganizationMapper organizationMapper, PilotCertificateMapper certMapper) {
        this.pilotMapper = pilotMapper;
        this.organizationMapper = organizationMapper;
        this.certMapper = certMapper;
    }

    public PageResult<PilotPublicVo> pilots(String keyword, String region, int page, int size) {
        LambdaQueryWrapper<Pilot> q = new LambdaQueryWrapper<Pilot>()
                .eq(Pilot::getStatus, "APPROVED")
                .orderByDesc(Pilot::getUpdatedAt);
        if (keyword != null && !keyword.isEmpty()) {
            q.and(w -> w.like(Pilot::getFullName, keyword).or().like(Pilot::getRecordNo, keyword));
        }
        if (region != null && !region.isEmpty()) {
            q.eq(Pilot::getRegion, region);
        }
        Page<Pilot> p = pilotMapper.selectPage(new Page<>(page, size), q);
        List<PilotPublicVo> list = p.getRecords().stream().map(this::toPilotVo).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), list);
    }

    public PageResult<OrgPublicVo> organizations(String keyword, String orgType, int page, int size) {
        LambdaQueryWrapper<Organization> q = new LambdaQueryWrapper<Organization>()
                .eq(Organization::getStatus, "APPROVED")
                .orderByDesc(Organization::getUpdatedAt);
        if (keyword != null && !keyword.isEmpty()) {
            q.like(Organization::getName, keyword);
        }
        if (orgType != null && !orgType.isEmpty()) {
            q.eq(Organization::getOrgType, orgType);
        }
        Page<Organization> p = organizationMapper.selectPage(new Page<>(page, size), q);
        List<OrgPublicVo> list = p.getRecords().stream().map(this::toOrgVo).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), list);
    }

    private PilotPublicVo toPilotVo(Pilot p) {
        PilotPublicVo v = new PilotPublicVo();
        v.setId(p.getId());
        v.setFullName(p.getFullName());
        v.setPhoneMasked(DesensitizeUtil.phone(p.getPhone()));
        v.setRegion(p.getRegion());
        v.setRecordNo(p.getRecordNo());
        v.setStatus(p.getStatus() != null ? p.getStatus() : "DRAFT");
        v.setFilingNo(p.getRecordNo());
        Long certCount = certMapper.selectCount(new LambdaQueryWrapper<PilotCertificate>()
                .eq(PilotCertificate::getPilotId, p.getId()));
        v.setCertCount(certCount != null ? certCount.intValue() : 0);
        return v;
    }

    private OrgPublicVo toOrgVo(Organization o) {
        OrgPublicVo v = new OrgPublicVo();
        v.setId(o.getId());
        v.setName(o.getName());
        v.setOrgType(o.getOrgType());
        v.setPhoneMasked(DesensitizeUtil.phone(o.getPhone()));
        v.setAddress(o.getAddress());
        v.setStatus(o.getStatus() != null ? o.getStatus() : "DRAFT");
        v.setContactPerson(o.getContactName());
        v.setUnifiedCreditCode(o.getCreditCode());
        return v;
    }
}
