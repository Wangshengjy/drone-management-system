package com.drone.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.collab.common.BusinessException;
import com.drone.collab.entity.Organization;
import com.drone.collab.entity.Pilot;
import com.drone.collab.entity.PilotOrg;
import com.drone.collab.entity.SysUser;
import com.drone.collab.mapper.OrganizationMapper;
import com.drone.collab.mapper.PilotMapper;
import com.drone.collab.mapper.PilotOrgMapper;
import com.drone.collab.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PilotOrgService {

    private final PilotOrgMapper pilotOrgMapper;
    private final PilotMapper pilotMapper;
    private final OrganizationMapper orgMapper;
    private final SysUserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public PilotOrg invitePilot(Long orgId, Long pilotId) {
        Pilot pilot = pilotMapper.selectById(pilotId);
        if (pilot == null) {
            throw new BusinessException(404, "飞手不存在");
        }
        Organization org = orgMapper.selectById(orgId);
        if (org == null) {
            throw new BusinessException(404, "单位不存在");
        }

        // 检查飞手是否已在其他单位
        PilotOrg activeInOther = pilotOrgMapper.selectOne(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getPilotId, pilotId)
                        .eq(PilotOrg::getStatus, "ACTIVE"));
        if (activeInOther != null && !activeInOther.getOrgId().equals(orgId)) {
            throw new BusinessException(400, "该飞手已在其他单位，不可重复加入");
        }

        PilotOrg existing = pilotOrgMapper.selectOne(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getPilotId, pilotId)
                        .eq(PilotOrg::getOrgId, orgId));
        if (existing != null) {
            if ("ACTIVE".equals(existing.getStatus())) {
                throw new BusinessException(400, "该飞手已在本单位");
            }
            if ("PENDING".equals(existing.getStatus())) {
                throw new BusinessException(400, "已存在待处理的关联记录");
            }
            existing.setStatus("PENDING");
            existing.setInitiator("ORG");
            pilotOrgMapper.updateById(existing);
            notifyPilot(pilotId, "单位邀请", org.getName() + "邀请您加入", existing.getId());
            return existing;
        }

        PilotOrg po = new PilotOrg();
        po.setPilotId(pilotId);
        po.setOrgId(orgId);
        po.setStatus("PENDING");
        po.setInitiator("ORG");
        pilotOrgMapper.insert(po);

        notifyPilot(pilotId, "单位邀请", org.getName() + "邀请您加入", po.getId());
        return po;
    }

    @Transactional
    public PilotOrg applyToJoin(Long pilotId, Long orgId) {
        Pilot pilot = pilotMapper.selectById(pilotId);
        if (pilot == null) {
            throw new BusinessException(404, "飞手不存在");
        }
        Organization org = orgMapper.selectById(orgId);
        if (org == null) {
            throw new BusinessException(404, "单位不存在");
        }

        // 检查飞手是否已在其他单位
        PilotOrg activeInOther = pilotOrgMapper.selectOne(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getPilotId, pilotId)
                        .eq(PilotOrg::getStatus, "ACTIVE"));
        if (activeInOther != null && !activeInOther.getOrgId().equals(orgId)) {
            throw new BusinessException(400, "您已在其他单位，不可重复加入");
        }

        PilotOrg existing = pilotOrgMapper.selectOne(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getPilotId, pilotId)
                        .eq(PilotOrg::getOrgId, orgId));
        if (existing != null) {
            if ("ACTIVE".equals(existing.getStatus())) {
                throw new BusinessException(400, "您已在该单位");
            }
            if ("PENDING".equals(existing.getStatus())) {
                throw new BusinessException(400, "已存在待处理的关联记录");
            }
            existing.setStatus("PENDING");
            existing.setInitiator("PILOT");
            pilotOrgMapper.updateById(existing);
            notifyOrg(orgId, "飞手申请加入", pilot.getFullName() + "申请加入您的单位", existing.getId());
            return existing;
        }

        PilotOrg po = new PilotOrg();
        po.setPilotId(pilotId);
        po.setOrgId(orgId);
        po.setStatus("PENDING");
        po.setInitiator("PILOT");
        pilotOrgMapper.insert(po);

        notifyOrg(orgId, "飞手申请加入", pilot.getFullName() + "申请加入您的单位", po.getId());
        return po;
    }

    @Transactional
    public void acceptInvitation(Long pilotId, Long relationId) {
        PilotOrg po = pilotOrgMapper.selectById(relationId);
        if (po == null) {
            throw new BusinessException(404, "关联记录不存在");
        }
        if (!po.getPilotId().equals(pilotId)) {
            throw new BusinessException(403, "无权操作");
        }
        if (!"PENDING".equals(po.getStatus())) {
            throw new BusinessException(400, "当前状态不可接受");
        }
        if (!"ORG".equals(po.getInitiator())) {
            throw new BusinessException(400, "该记录非单位邀请，无法接受邀请");
        }
        // 检查飞手是否已在其他单位
        PilotOrg activeInOther = pilotOrgMapper.selectOne(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getPilotId, pilotId)
                        .eq(PilotOrg::getStatus, "ACTIVE"));
        if (activeInOther != null) {
            throw new BusinessException(400, "您已在其他单位，请先退出后再接受邀请");
        }
        po.setStatus("ACTIVE");
        pilotOrgMapper.updateById(po);

        Pilot pilot = pilotMapper.selectById(pilotId);
        String pilotName = pilot != null ? pilot.getFullName() : "飞手#" + pilotId;
        notifyOrg(po.getOrgId(), "飞手已接受邀请", pilotName + "已接受您的邀请", po.getId());
    }

    @Transactional
    public void rejectInvitation(Long pilotId, Long relationId) {
        PilotOrg po = pilotOrgMapper.selectById(relationId);
        if (po == null) {
            throw new BusinessException(404, "关联记录不存在");
        }
        if (!po.getPilotId().equals(pilotId)) {
            throw new BusinessException(403, "无权操作");
        }
        if (!"PENDING".equals(po.getStatus())) {
            throw new BusinessException(400, "当前状态不可拒绝");
        }
        if (!"ORG".equals(po.getInitiator())) {
            throw new BusinessException(400, "该记录非单位邀请，无法拒绝邀请");
        }
        po.setStatus("REJECTED");
        pilotOrgMapper.updateById(po);

        Pilot pilot = pilotMapper.selectById(pilotId);
        String pilotName = pilot != null ? pilot.getFullName() : "飞手#" + pilotId;
        notifyOrg(po.getOrgId(), "飞手已拒绝邀请", pilotName + "已拒绝您的邀请", po.getId());
    }

    @Transactional
    public void acceptApplication(Long orgId, Long relationId) {
        PilotOrg po = pilotOrgMapper.selectById(relationId);
        if (po == null) {
            throw new BusinessException(404, "关联记录不存在");
        }
        if (!po.getOrgId().equals(orgId)) {
            throw new BusinessException(403, "无权操作");
        }
        if (!"PENDING".equals(po.getStatus())) {
            throw new BusinessException(400, "当前状态不可接受");
        }
        if (!"PILOT".equals(po.getInitiator())) {
            throw new BusinessException(400, "该记录非飞手申请，无法接受申请");
        }
        // 检查飞手是否已在其他单位
        PilotOrg activeInOther = pilotOrgMapper.selectOne(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getPilotId, po.getPilotId())
                        .eq(PilotOrg::getStatus, "ACTIVE"));
        if (activeInOther != null) {
            throw new BusinessException(400, "该飞手已在其他单位，不可重复加入");
        }
        po.setStatus("ACTIVE");
        pilotOrgMapper.updateById(po);

        Organization org = orgMapper.selectById(orgId);
        String orgName = org != null ? org.getName() : "单位#" + orgId;
        notifyPilot(po.getPilotId(), "申请已通过", orgName + "已接受您的加入申请", po.getId());
    }

    @Transactional
    public void rejectApplication(Long orgId, Long relationId) {
        PilotOrg po = pilotOrgMapper.selectById(relationId);
        if (po == null) {
            throw new BusinessException(404, "关联记录不存在");
        }
        if (!po.getOrgId().equals(orgId)) {
            throw new BusinessException(403, "无权操作");
        }
        if (!"PENDING".equals(po.getStatus())) {
            throw new BusinessException(400, "当前状态不可拒绝");
        }
        if (!"PILOT".equals(po.getInitiator())) {
            throw new BusinessException(400, "该记录非飞手申请，无法拒绝申请");
        }
        po.setStatus("REJECTED");
        pilotOrgMapper.updateById(po);

        Organization org = orgMapper.selectById(orgId);
        String orgName = org != null ? org.getName() : "单位#" + orgId;
        notifyPilot(po.getPilotId(), "申请已拒绝", orgName + "已拒绝您的加入申请", po.getId());
    }

    @Transactional
    public void leaveOrg(Long pilotId, Long orgId) {
        PilotOrg po = pilotOrgMapper.selectOne(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getPilotId, pilotId)
                        .eq(PilotOrg::getOrgId, orgId)
                        .eq(PilotOrg::getStatus, "ACTIVE"));
        if (po == null) {
            throw new BusinessException(404, "您不在该单位中");
        }
        po.setStatus("LEFT");
        pilotOrgMapper.updateById(po);

        Pilot pilot = pilotMapper.selectById(pilotId);
        String pilotName = pilot != null ? pilot.getFullName() : "飞手#" + pilotId;
        notifyOrg(orgId, "飞手已退出", pilotName + "已退出您的单位", po.getId());
    }

    @Transactional
    public void removePilot(Long orgId, Long pilotId) {
        PilotOrg po = pilotOrgMapper.selectOne(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getPilotId, pilotId)
                        .eq(PilotOrg::getOrgId, orgId)
                        .eq(PilotOrg::getStatus, "ACTIVE"));
        if (po == null) {
            throw new BusinessException(404, "该飞手不在本单位中");
        }
        po.setStatus("LEFT");
        pilotOrgMapper.updateById(po);

        Organization org = orgMapper.selectById(orgId);
        String orgName = org != null ? org.getName() : "单位#" + orgId;
        notifyPilot(pilotId, "已被移除", "您已被" + orgName + "移除", po.getId());
    }

    public List<PilotOrg> getOrgPilots(Long orgId) {
        List<PilotOrg> relations = pilotOrgMapper.selectList(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getOrgId, orgId)
                        .eq(PilotOrg::getStatus, "ACTIVE"));
        for (PilotOrg po : relations) {
            Pilot p = pilotMapper.selectById(po.getPilotId());
            if (p != null) {
                po.setPilotName(p.getFullName());
                po.setPilotPhone(p.getPhone());
                po.setPilotStatus(p.getStatus());
            }
        }
        return relations;
    }

    public PilotOrg getPilotOrg(Long pilotId) {
        PilotOrg po = pilotOrgMapper.selectOne(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getPilotId, pilotId)
                        .eq(PilotOrg::getStatus, "ACTIVE")
                        .last("LIMIT 1"));
        if (po != null) {
            Organization org = orgMapper.selectById(po.getOrgId());
            if (org != null) {
                po.setOrgName(org.getName());
                po.setOrgType(org.getOrgType());
                po.setOrgStatus(org.getStatus());
                po.setOrgContactPerson(org.getContactName());
            }
        }
        return po;
    }

    public List<PilotOrg> getPendingForPilot(Long pilotId) {
        List<PilotOrg> list = pilotOrgMapper.selectList(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getPilotId, pilotId)
                        .eq(PilotOrg::getStatus, "PENDING")
                        .eq(PilotOrg::getInitiator, "ORG"));
        for (PilotOrg po : list) {
            Organization org = orgMapper.selectById(po.getOrgId());
            if (org != null) {
                po.setOrgName(org.getName());
            }
        }
        return list;
    }

    public List<PilotOrg> getPendingForOrg(Long orgId) {
        List<PilotOrg> list = pilotOrgMapper.selectList(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getOrgId, orgId)
                        .eq(PilotOrg::getStatus, "PENDING")
                        .eq(PilotOrg::getInitiator, "PILOT"));
        for (PilotOrg po : list) {
            Pilot p = pilotMapper.selectById(po.getPilotId());
            if (p != null) {
                po.setPilotName(p.getFullName());
            }
        }
        return list;
    }

    private void notifyPilot(Long pilotId, String title, String content, Long targetId) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getPilotId, pilotId)
                        .eq(SysUser::getEnabled, 1)
                        .last("LIMIT 1"));
        if (user != null) {
            notificationService.sendToUser(user.getId(), "PILOT_ORG", targetId, title, content, "ORG_MEMBER");
        }
    }

    private void notifyOrg(Long orgId, String title, String content, Long targetId) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getOrgId, orgId)
                        .eq(SysUser::getEnabled, 1)
                        .last("LIMIT 1"));
        if (user != null) {
            notificationService.sendToUser(user.getId(), "PILOT_ORG", targetId, title, content, "ORG_MEMBER");
        }
    }
}
