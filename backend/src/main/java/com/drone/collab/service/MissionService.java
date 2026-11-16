package com.drone.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.collab.common.BusinessException;
import com.drone.collab.common.PageResult;
import com.drone.collab.entity.*;
import com.drone.collab.entity.SysUser;
import com.drone.collab.mapper.*;
import com.drone.collab.mapper.SysUserMapper;
import com.drone.collab.security.SecurityUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionMapper missionMapper;
    private final MissionAssignmentMapper assignmentMapper;
    private final MissionLogMapper logMapper;
    private final PilotStatusMapper pilotStatusMapper;
    private final OrgStatusMapper orgStatusMapper;
    private final PilotMapper pilotMapper;
    private final PilotCertificateMapper certMapper;
    private final OrganizationMapper orgMapper;
    private final SysUserMapper userMapper;
    private final PilotOrgMapper pilotOrgMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public Mission create(Mission mission, SecurityUser user) {
        mission.setCreatorId(user.getUserId());
        mission.setStatus("PUBLISHED");
        mission.setCreatedAt(LocalDateTime.now());
        mission.setUpdatedAt(LocalDateTime.now());
        missionMapper.insert(mission);
        addLog(mission.getId(), "CREATE", user.getUserId(), "创建任务", null);
        return mission;
    }

    public Mission getById(Long id) {
        Mission m = missionMapper.selectById(id);
        if (m == null) throw new BusinessException(404, "任务不存在");
        return m;
    }

    public PageResult<Mission> page(String status, String missionType, String urgency, int pageNum, int pageSize) {
        LambdaQueryWrapper<Mission> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq(Mission::getStatus, status);
        if (missionType != null && !missionType.isEmpty()) qw.eq(Mission::getMissionType, missionType);
        if (urgency != null && !urgency.isEmpty()) qw.eq(Mission::getUrgency, urgency);
        qw.orderByDesc(Mission::getCreatedAt);
        Page<Mission> p = missionMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return new PageResult<>(p.getTotal(), p.getRecords());
    }

    public List<Mission> myMissions(SecurityUser user) {
        List<Long> missionIds = new ArrayList<>();
        if ("PILOT".equals(user.getRole()) && user.getPilotId() != null) {
            List<MissionAssignment> assigns = assignmentMapper.selectList(
                    new LambdaQueryWrapper<MissionAssignment>()
                            .eq(MissionAssignment::getAssigneeType, "PILOT")
                            .eq(MissionAssignment::getAssigneeId, user.getPilotId())
                            .ne(MissionAssignment::getStatus, "REJECTED"));
            missionIds = assigns.stream().map(MissionAssignment::getMissionId).distinct().collect(Collectors.toList());
        } else if ("ORG".equals(user.getRole()) && user.getOrgId() != null) {
            List<MissionAssignment> assigns = assignmentMapper.selectList(
                    new LambdaQueryWrapper<MissionAssignment>()
                            .eq(MissionAssignment::getAssigneeType, "ORG")
                            .eq(MissionAssignment::getAssigneeId, user.getOrgId())
                            .ne(MissionAssignment::getStatus, "REJECTED"));
            missionIds = assigns.stream().map(MissionAssignment::getMissionId).distinct().collect(Collectors.toList());
        }

        if ("PILOT".equals(user.getRole()) || "ORG".equals(user.getRole())) {
            List<Mission> dispatched = missionMapper.selectList(
                    new LambdaQueryWrapper<Mission>().eq(Mission::getStatus, "DISPATCHED"));
            for (Mission m : dispatched) {
                if (!missionIds.contains(m.getId())) {
                    missionIds.add(m.getId());
                }
            }
        }

        if (missionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return missionMapper.selectList(
                new LambdaQueryWrapper<Mission>()
                        .in(Mission::getId, missionIds)
                        .orderByDesc(Mission::getCreatedAt));
    }

    public List<MissionAssignment> getAssignments(Long missionId) {
        return assignmentMapper.selectList(
                new LambdaQueryWrapper<MissionAssignment>()
                        .eq(MissionAssignment::getMissionId, missionId)
                        .orderByAsc(MissionAssignment::getRankOrder));
    }

    public List<Map<String, Object>> getAssignmentDetail(Long missionId) {
        List<MissionAssignment> assignments = assignmentMapper.selectList(
                new LambdaQueryWrapper<MissionAssignment>()
                        .eq(MissionAssignment::getMissionId, missionId)
                        .orderByAsc(MissionAssignment::getRankOrder));
        List<Map<String, Object>> result = new ArrayList<>();
        for (MissionAssignment ma : assignments) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", ma.getId());
            item.put("assigneeType", ma.getAssigneeType());
            item.put("assigneeId", ma.getAssigneeId());
            item.put("status", ma.getStatus());
            item.put("matchScore", ma.getMatchScore());
            item.put("respondedAt", ma.getRespondedAt());
            item.put("completedAt", ma.getCompletedAt());
            item.put("rankOrder", ma.getRankOrder());

            if ("PILOT".equals(ma.getAssigneeType())) {
                Pilot p = pilotMapper.selectById(ma.getAssigneeId());
                if (p != null) {
                    item.put("assigneeName", p.getFullName());
                    item.put("assigneePhone", p.getPhone() != null ? p.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2") : null);
                }
            } else {
                Organization o = orgMapper.selectById(ma.getAssigneeId());
                if (o != null) {
                    item.put("assigneeName", o.getName());
                    item.put("assigneePhone", o.getPhone() != null ? o.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2") : null);
                }
            }
            result.add(item);
        }
        return result;
    }

    public List<MissionLog> getLogs(Long missionId) {
        return logMapper.selectList(
                new LambdaQueryWrapper<MissionLog>()
                        .eq(MissionLog::getMissionId, missionId)
                        .orderByAsc(MissionLog::getCreatedAt));
    }

    @Transactional
    public List<MissionAssignment> smartDispatch(Long missionId, SecurityUser user) {
        Mission mission = getById(missionId);
        if (!"PUBLISHED".equals(mission.getStatus())) {
            throw new BusinessException(400, "只有PUBLISHED状态的任务可以派单");
        }

        int reqPilots = mission.getRequiredPilotCount() != null ? mission.getRequiredPilotCount() : 1;
        int reqOrgs = mission.getRequiredOrgCount() != null ? mission.getRequiredOrgCount() : 1;

        List<Map<String, Object>> pilotScores = scorePilots(mission);
        List<Map<String, Object>> orgScores = scoreOrgs(mission);

        List<MissionAssignment> assignments = new ArrayList<>();
        int rank = 1;

        for (Map<String, Object> ps : pilotScores) {
            MissionAssignment ma = new MissionAssignment();
            ma.setMissionId(missionId);
            ma.setAssigneeType("PILOT");
            ma.setAssigneeId(((Number) ps.get("pilotId")).longValue());
            ma.setMatchScore(((Number) ps.get("totalScore")).doubleValue());
            try {
                ma.setMatchDetail(objectMapper.writeValueAsString(ps));
            } catch (Exception ignored) {
            }
            ma.setStatus("PENDING");
            ma.setRankOrder(rank++);
            ma.setCreatedAt(LocalDateTime.now());
            assignmentMapper.insert(ma);
            assignments.add(ma);
        }

        rank = 1;
        for (Map<String, Object> os : orgScores) {
            MissionAssignment ma = new MissionAssignment();
            ma.setMissionId(missionId);
            ma.setAssigneeType("ORG");
            ma.setAssigneeId(((Number) os.get("orgId")).longValue());
            ma.setMatchScore(((Number) os.get("totalScore")).doubleValue());
            try {
                ma.setMatchDetail(objectMapper.writeValueAsString(os));
            } catch (Exception ignored) {
            }
            ma.setStatus("PENDING");
            ma.setRankOrder(rank++);
            ma.setCreatedAt(LocalDateTime.now());
            assignmentMapper.insert(ma);
            assignments.add(ma);
        }

        mission.setStatus("DISPATCHED");
        int deadlineMinutes = "RED".equals(mission.getUrgency()) ? 5 : "YELLOW".equals(mission.getUrgency()) ? 15 : 30;
        mission.setConfirmDeadline(LocalDateTime.now().plusMinutes(deadlineMinutes));
        mission.setUpdatedAt(LocalDateTime.now());
        missionMapper.updateById(mission);

        addLog(missionId, "DISPATCH", user.getUserId(),
                "智能派单完成，派发" + pilotScores.size() + "个飞手+" + orgScores.size() + "个单位(需求:" + reqPilots + "飞手+" + reqOrgs + "单位)", null);

        String urgencyLabel = "RED".equals(mission.getUrgency()) ? "红色" : "YELLOW".equals(mission.getUrgency()) ? "黄色" : "蓝色";
        String typeLabel = "FIRE_FIGHTING".equals(mission.getMissionType()) ? "森林灭火"
                : "FLOOD_RESCUE".equals(mission.getMissionType()) ? "洪涝搜救"
                : "ACCIDENT_SURVEY".equals(mission.getMissionType()) ? "事故勘察" : "其他";
        notificationService.sendMissionNotification(missionId,
            "【" + urgencyLabel + "级】" + typeLabel + "任务",
            mission.getTitle() + " | " + mission.getLocationName() + " | 需" + reqPilots + "飞手+" + reqOrgs + "单位 | 截止" + deadlineMinutes + "分钟内响应");

        return assignments;
    }

    @Transactional
    public MissionAssignment manualDispatch(Long missionId, SecurityUser user, String assigneeType, Long assigneeId) {
        Mission mission = getById(missionId);
        if (mission == null) throw new BusinessException(404, "任务不存在");
        if (!"PUBLISHED".equals(mission.getStatus()) && !"DISPATCHED".equals(mission.getStatus())) {
            throw new BusinessException(400, "当前任务状态不可派单");
        }

        MissionAssignment existing = assignmentMapper.selectOne(
                new LambdaQueryWrapper<MissionAssignment>()
                        .eq(MissionAssignment::getMissionId, missionId)
                        .eq(MissionAssignment::getAssigneeType, assigneeType)
                        .eq(MissionAssignment::getAssigneeId, assigneeId)
                        .ne(MissionAssignment::getStatus, "REJECTED"));
        if (existing != null) throw new BusinessException(400, "该对象已有派单记录");

        String assigneeName;
        if ("PILOT".equals(assigneeType)) {
            Pilot p = pilotMapper.selectById(assigneeId);
            assigneeName = p != null ? p.getFullName() : "飞手#" + assigneeId;
        } else {
            Organization o = orgMapper.selectById(assigneeId);
            assigneeName = o != null ? o.getName() : "单位#" + assigneeId;
        }

        MissionAssignment assignment = new MissionAssignment();
        assignment.setMissionId(missionId);
        assignment.setAssigneeType(assigneeType);
        assignment.setAssigneeId(assigneeId);
        assignment.setMatchScore(0.0);
        assignment.setMatchDetail("{\"manual\":true,\"name\":\"" + assigneeName + "\"}");
        assignment.setStatus("PENDING");
        assignment.setRankOrder(1);
        assignmentMapper.insert(assignment);

        if (!"DISPATCHED".equals(mission.getStatus())) {
            mission.setStatus("DISPATCHED");
            int deadlineMinutes = "RED".equals(mission.getUrgency()) ? 5 : "YELLOW".equals(mission.getUrgency()) ? 15 : 30;
            mission.setConfirmDeadline(LocalDateTime.now().plusMinutes(deadlineMinutes));
        }
        mission.setUpdatedAt(LocalDateTime.now());
        missionMapper.updateById(mission);

        addLog(missionId, "DISPATCH", user.getUserId(), "手动派单给" + assigneeName, null);

        SysUser targetUser = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq("PILOT".equals(assigneeType), SysUser::getPilotId, assigneeId)
                        .eq("ORG".equals(assigneeType), SysUser::getOrgId, assigneeId)
                        .eq(SysUser::getEnabled, 1)
                        .last("LIMIT 1"));
        if (targetUser != null) {
            String urgencyLabel = "RED".equals(mission.getUrgency()) ? "红色" : "YELLOW".equals(mission.getUrgency()) ? "黄色" : "蓝色";
            notificationService.sendToUser(targetUser.getId(), "MISSION", missionId,
                "【手动派单】" + urgencyLabel + "级任务",
                mission.getTitle() + " | " + mission.getLocationName(), "MISSION");
        }

        return assignment;
    }

    @Transactional
    public Mission acceptAssignment(Long missionId, SecurityUser user) {
        Mission mission = getById(missionId);
        if (!"DISPATCHED".equals(mission.getStatus())) {
            throw new BusinessException(400, "当前任务状态不可接单");
        }
        if (mission.getConfirmDeadline() != null && LocalDateTime.now().isAfter(mission.getConfirmDeadline())) {
            throw new BusinessException(400, "接单已超时");
        }

        String assigneeType = "PILOT".equals(user.getRole()) ? "PILOT" : "ORG";
        Long assigneeId = "PILOT".equals(user.getRole()) ? user.getPilotId() : user.getOrgId();

        if (assigneeId == null) {
            throw new BusinessException(403, "账号未关联飞手/单位信息");
        }

        MissionAssignment myAssignment = assignmentMapper.selectOne(
                new LambdaQueryWrapper<MissionAssignment>()
                        .eq(MissionAssignment::getMissionId, missionId)
                        .eq(MissionAssignment::getAssigneeType, assigneeType)
                        .eq(MissionAssignment::getAssigneeId, assigneeId)
                        .in(MissionAssignment::getStatus, "PENDING", "ACCEPTED"));

        if (myAssignment == null) {
            MissionAssignment rejectedCheck = assignmentMapper.selectOne(
                    new LambdaQueryWrapper<MissionAssignment>()
                            .eq(MissionAssignment::getMissionId, missionId)
                            .eq(MissionAssignment::getAssigneeType, assigneeType)
                            .eq(MissionAssignment::getAssigneeId, assigneeId)
                            .eq(MissionAssignment::getStatus, "REJECTED"));
            if (rejectedCheck != null) {
                throw new BusinessException(403, "您已拒绝过该任务，无法再次接单");
            }

            myAssignment = new MissionAssignment();
            myAssignment.setMissionId(missionId);
            myAssignment.setAssigneeType(assigneeType);
            myAssignment.setAssigneeId(assigneeId);
            myAssignment.setMatchScore(0.0);
            myAssignment.setMatchDetail("{\"autoAccept\":true}");
            myAssignment.setStatus("PENDING");
            myAssignment.setRankOrder(99);
            myAssignment.setCreatedAt(LocalDateTime.now());
            assignmentMapper.insert(myAssignment);
        }

        if ("ACCEPTED".equals(myAssignment.getStatus())) {
            throw new BusinessException(400, "您已接取该任务");
        }

        myAssignment.setStatus("ACCEPTED");
        myAssignment.setRespondedAt(LocalDateTime.now());
        assignmentMapper.updateById(myAssignment);

        if ("PILOT".equals(assigneeType)) {
            PilotStatus ps = pilotStatusMapper.selectOne(
                    new LambdaQueryWrapper<PilotStatus>().eq(PilotStatus::getPilotId, assigneeId));
            if (ps != null) {
                ps.setAvailability("ON_MISSION");
                ps.setActiveMissionId(missionId);
                pilotStatusMapper.updateById(ps);
            }
        } else {
            OrgStatus os = orgStatusMapper.selectOne(
                    new LambdaQueryWrapper<OrgStatus>().eq(OrgStatus::getOrgId, assigneeId));
            if (os != null) {
                os.setAvailability("ON_MISSION");
                os.setActiveMissionId(missionId);
                orgStatusMapper.updateById(os);
            }
        }

        long acceptedPilots = assignmentMapper.selectCount(
                new LambdaQueryWrapper<MissionAssignment>()
                        .eq(MissionAssignment::getMissionId, missionId)
                        .eq(MissionAssignment::getAssigneeType, "PILOT")
                        .eq(MissionAssignment::getStatus, "ACCEPTED"));
        long acceptedOrgs = assignmentMapper.selectCount(
                new LambdaQueryWrapper<MissionAssignment>()
                        .eq(MissionAssignment::getMissionId, missionId)
                        .eq(MissionAssignment::getAssigneeType, "ORG")
                        .eq(MissionAssignment::getStatus, "ACCEPTED"));
        int reqPilots = mission.getRequiredPilotCount() != null ? mission.getRequiredPilotCount() : 1;
        int reqOrgs = mission.getRequiredOrgCount() != null ? mission.getRequiredOrgCount() : 1;

        if (acceptedPilots >= reqPilots && acceptedOrgs >= reqOrgs) {
            rejectOtherAssignments(missionId, myAssignment.getId());
        }

        mission.setStatus("ACCEPTED");
        mission.setUpdatedAt(LocalDateTime.now());
        missionMapper.updateById(mission);

        addLog(missionId, "ACCEPT", user.getUserId(), assigneeType + "确认接单", null);

        List<SysUser> admins = userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "ADMIN").eq(SysUser::getEnabled, 1));
        String assigneeName = "PILOT".equals(assigneeType) ? "飞手" : "协作单位";
        for (SysUser admin : admins) {
            notificationService.sendToUser(admin.getId(), "MISSION", missionId,
                "任务已被接取", assigneeName + "已接取任务: " + mission.getTitle(), "MISSION");
        }

        return mission;
    }

    @Transactional
    public Mission rejectAssignment(Long missionId, SecurityUser user) {
        String assigneeType = "PILOT".equals(user.getRole()) ? "PILOT" : "ORG";
        Long assigneeId = "PILOT".equals(user.getRole()) ? user.getPilotId() : user.getOrgId();

        if (assigneeId == null) {
            throw new BusinessException(403, "账号未关联飞手/单位信息");
        }

        MissionAssignment myAssignment = assignmentMapper.selectOne(
                new LambdaQueryWrapper<MissionAssignment>()
                        .eq(MissionAssignment::getMissionId, missionId)
                        .eq(MissionAssignment::getAssigneeType, assigneeType)
                        .eq(MissionAssignment::getAssigneeId, assigneeId)
                        .eq(MissionAssignment::getStatus, "PENDING"));
        if (myAssignment == null) {
            throw new BusinessException(403, "没有待处理的派单");
        }
        myAssignment.setStatus("REJECTED");
        myAssignment.setRespondedAt(LocalDateTime.now());
        assignmentMapper.updateById(myAssignment);

        addLog(missionId, "REJECT", user.getUserId(), assigneeType + "拒绝接单", null);
        return getById(missionId);
    }

    @Transactional
    public void checkTimeout() {
        List<Mission> dispatched = missionMapper.selectList(
                new LambdaQueryWrapper<Mission>()
                        .eq(Mission::getStatus, "DISPATCHED")
                        .lt(Mission::getConfirmDeadline, LocalDateTime.now()));
        for (Mission m : dispatched) {
            List<MissionAssignment> pending = assignmentMapper.selectList(
                    new LambdaQueryWrapper<MissionAssignment>()
                            .eq(MissionAssignment::getMissionId, m.getId())
                            .eq(MissionAssignment::getStatus, "PENDING"));
            for (MissionAssignment ma : pending) {
                ma.setStatus("TIMEOUT");
                ma.setRespondedAt(LocalDateTime.now());
                assignmentMapper.updateById(ma);
            }
            addLog(m.getId(), "TIMEOUT", null, "所有派单超时", null);
            m.setStatus("PUBLISHED");
            m.setUpdatedAt(LocalDateTime.now());
            missionMapper.updateById(m);
        }
    }

    @Transactional
    public Mission updateStatus(Long missionId, String newStatus, SecurityUser user, String comment, String attachmentUrl) {
        Mission mission = getById(missionId);

        if ("COMPLETED".equals(newStatus) && user != null && ("PILOT".equals(user.getRole()) || "ORG".equals(user.getRole()))) {
            String assigneeType = "PILOT".equals(user.getRole()) ? "PILOT" : "ORG";
            Long assigneeId = "PILOT".equals(user.getRole()) ? user.getPilotId() : user.getOrgId();
            if (assigneeId == null) {
                throw new BusinessException(403, "账号未关联飞手/单位信息");
            }

            MissionAssignment myAssignment = assignmentMapper.selectOne(
                    new LambdaQueryWrapper<MissionAssignment>()
                            .eq(MissionAssignment::getMissionId, missionId)
                            .eq(MissionAssignment::getAssigneeType, assigneeType)
                            .eq(MissionAssignment::getAssigneeId, assigneeId)
                            .eq(MissionAssignment::getStatus, "ACCEPTED"));
            if (myAssignment == null) {
                throw new BusinessException(403, "您没有该任务的进行中派单");
            }

            myAssignment.setStatus("COMPLETED");
            myAssignment.setCompletedAt(LocalDateTime.now());
            assignmentMapper.updateById(myAssignment);

            if ("PILOT".equals(assigneeType)) {
                PilotStatus ps = pilotStatusMapper.selectOne(
                        new LambdaQueryWrapper<PilotStatus>().eq(PilotStatus::getPilotId, assigneeId));
                if (ps != null) {
                    ps.setAvailability("IDLE");
                    ps.setMissionCount(ps.getMissionCount() + 1);
                    ps.setActiveMissionId(null);
                    pilotStatusMapper.updateById(ps);
                }
            } else {
                OrgStatus os = orgStatusMapper.selectOne(
                        new LambdaQueryWrapper<OrgStatus>().eq(OrgStatus::getOrgId, assigneeId));
                if (os != null) {
                    os.setAvailability("IDLE");
                    os.setMissionCount(os.getMissionCount() + 1);
                    os.setActiveMissionId(null);
                    orgStatusMapper.updateById(os);
                }
            }

            addLog(missionId, "COMPLETE", user.getUserId(),
                    assigneeType + "完成自身任务" + (comment != null ? ": " + comment : ""), attachmentUrl);

            long acceptedCount = assignmentMapper.selectCount(
                    new LambdaQueryWrapper<MissionAssignment>()
                            .eq(MissionAssignment::getMissionId, missionId)
                            .eq(MissionAssignment::getStatus, "ACCEPTED"));
            long completedCount = assignmentMapper.selectCount(
                    new LambdaQueryWrapper<MissionAssignment>()
                            .eq(MissionAssignment::getMissionId, missionId)
                            .eq(MissionAssignment::getStatus, "COMPLETED"));
            long totalAccepted = acceptedCount + completedCount;

            if (acceptedCount == 0 && totalAccepted > 0) {
                mission.setStatus("COMPLETED");
                mission.setCompletedAt(LocalDateTime.now());
                mission.setUpdatedAt(LocalDateTime.now());
                missionMapper.updateById(mission);

                addLog(missionId, "COMPLETE", null, "所有飞手/单位已完成，任务关闭", null);

                List<SysUser> admins = userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "ADMIN").eq(SysUser::getEnabled, 1));
                for (SysUser admin : admins) {
                    notificationService.sendToUser(admin.getId(), "MISSION", missionId,
                        "任务全部完成", "任务\"" + mission.getTitle() + "\"所有参与者已完成", "MISSION");
                }
            } else {
                mission.setUpdatedAt(LocalDateTime.now());
                missionMapper.updateById(mission);

                List<SysUser> admins = userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "ADMIN").eq(SysUser::getEnabled, 1));
                String assigneeName = "PILOT".equals(assigneeType) ? "飞手" : "协作单位";
                for (SysUser admin : admins) {
                    notificationService.sendToUser(admin.getId(), "MISSION", missionId,
                        assigneeName + "已完成任务",
                        "任务\"" + mission.getTitle() + "\" " + completedCount + "/" + totalAccepted + " 已完成", "MISSION");
                }
            }

            return mission;
        }

        String oldStatus = mission.getStatus();
        mission.setStatus(newStatus);
        mission.setUpdatedAt(LocalDateTime.now());

        if ("COMPLETED".equals(newStatus)) {
            mission.setCompletedAt(LocalDateTime.now());
            releaseAllAssigned(mission);
        }
        if ("CANCELLED".equals(newStatus)) {
            releaseAllAssigned(mission);
        }

        missionMapper.updateById(mission);
        addLog(missionId, "STATUS_CHANGE", user != null ? user.getUserId() : null,
                comment != null ? comment : oldStatus + " -> " + newStatus, attachmentUrl);
        return mission;
    }

    private void releaseAllAssigned(Mission mission) {
        List<MissionAssignment> accepted = assignmentMapper.selectList(
                new LambdaQueryWrapper<MissionAssignment>()
                        .eq(MissionAssignment::getMissionId, mission.getId())
                        .in(MissionAssignment::getStatus, "ACCEPTED", "PENDING"));
        for (MissionAssignment ma : accepted) {
            if ("ACCEPTED".equals(ma.getStatus())) {
                ma.setStatus("COMPLETED");
                ma.setCompletedAt(LocalDateTime.now());
            } else {
                ma.setStatus("REJECTED");
            }
            ma.setRespondedAt(LocalDateTime.now());
            assignmentMapper.updateById(ma);

            if ("PILOT".equals(ma.getAssigneeType())) {
                PilotStatus ps = pilotStatusMapper.selectOne(
                        new LambdaQueryWrapper<PilotStatus>().eq(PilotStatus::getPilotId, ma.getAssigneeId()));
                if (ps != null) {
                    ps.setAvailability("IDLE");
                    ps.setMissionCount(ps.getMissionCount() + 1);
                    ps.setActiveMissionId(null);
                    pilotStatusMapper.updateById(ps);
                }
            } else {
                OrgStatus os = orgStatusMapper.selectOne(
                        new LambdaQueryWrapper<OrgStatus>().eq(OrgStatus::getOrgId, ma.getAssigneeId()));
                if (os != null) {
                    os.setAvailability("IDLE");
                    os.setMissionCount(os.getMissionCount() + 1);
                    os.setActiveMissionId(null);
                    orgStatusMapper.updateById(os);
                }
            }
        }
    }

    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();

        long totalMissions = missionMapper.selectCount(new LambdaQueryWrapper<>());
        long activeMissions = missionMapper.selectCount(
                new LambdaQueryWrapper<Mission>().in(Mission::getStatus, List.of("PUBLISHED", "DISPATCHED", "ACCEPTED", "IN_PROGRESS")));
        long completedMissions = missionMapper.selectCount(
                new LambdaQueryWrapper<Mission>().eq(Mission::getStatus, "COMPLETED"));
        long idlePilots = pilotStatusMapper.selectCount(
                new LambdaQueryWrapper<PilotStatus>().eq(PilotStatus::getAvailability, "IDLE"));
        long onMissionPilots = pilotStatusMapper.selectCount(
                new LambdaQueryWrapper<PilotStatus>().eq(PilotStatus::getAvailability, "ON_MISSION"));
        long idleOrgs = orgStatusMapper.selectCount(
                new LambdaQueryWrapper<OrgStatus>().eq(OrgStatus::getAvailability, "IDLE"));
        long onMissionOrgs = orgStatusMapper.selectCount(
                new LambdaQueryWrapper<OrgStatus>().eq(OrgStatus::getAvailability, "ON_MISSION"));

        data.put("totalMissions", totalMissions);
        data.put("activeMissions", activeMissions);
        data.put("completedMissions", completedMissions);
        data.put("idlePilots", idlePilots);
        data.put("onMissionPilots", onMissionPilots);
        data.put("idleOrgs", idleOrgs);
        data.put("onMissionOrgs", onMissionOrgs);

        List<Mission> recentMissions = missionMapper.selectList(
                new LambdaQueryWrapper<Mission>().orderByDesc(Mission::getCreatedAt).last("LIMIT 10"));
        data.put("recentMissions", recentMissions);

        List<PilotStatus> pilotStatuses = pilotStatusMapper.selectList(null);
        List<Map<String, Object>> enrichedPilotStatuses = new ArrayList<>();
        for (PilotStatus ps : pilotStatuses) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", ps.getId());
            item.put("pilotId", ps.getPilotId());
            item.put("availability", ps.getAvailability());
            item.put("currentLatitude", ps.getCurrentLatitude());
            item.put("currentLongitude", ps.getCurrentLongitude());
            item.put("droneType", ps.getDroneType());
            item.put("missionCount", ps.getMissionCount());
            item.put("activeMissionId", ps.getActiveMissionId());
            Pilot pilot = pilotMapper.selectById(ps.getPilotId());
            item.put("pilotName", pilot != null ? pilot.getFullName() : "飞手#" + ps.getPilotId());
            enrichedPilotStatuses.add(item);
        }
        data.put("pilotStatuses", enrichedPilotStatuses);

        List<OrgStatus> orgStatuses = orgStatusMapper.selectList(null);
        List<Map<String, Object>> enrichedOrgStatuses = new ArrayList<>();
        for (OrgStatus os : orgStatuses) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", os.getId());
            item.put("orgId", os.getOrgId());
            item.put("availability", os.getAvailability());
            item.put("latitude", os.getLatitude());
            item.put("longitude", os.getLongitude());
            item.put("missionCount", os.getMissionCount());
            item.put("activeMissionId", os.getActiveMissionId());
            Organization org = orgMapper.selectById(os.getOrgId());
            item.put("orgName", org != null ? org.getName() : "单位#" + os.getOrgId());
            enrichedOrgStatuses.add(item);
        }
        data.put("orgStatuses", enrichedOrgStatuses);

        return data;
    }

    public PilotStatus getPilotStatus(Long pilotId) {
        return pilotStatusMapper.selectOne(
                new LambdaQueryWrapper<PilotStatus>().eq(PilotStatus::getPilotId, pilotId));
    }

    public OrgStatus getOrgStatus(Long orgId) {
        return orgStatusMapper.selectOne(
                new LambdaQueryWrapper<OrgStatus>().eq(OrgStatus::getOrgId, orgId));
    }

    @Transactional
    public PilotStatus updatePilotStatus(Long pilotId, String availability, Double lat, Double lng, String droneType) {
        PilotStatus ps = pilotStatusMapper.selectOne(
                new LambdaQueryWrapper<PilotStatus>().eq(PilotStatus::getPilotId, pilotId));
        if (ps == null) {
            ps = new PilotStatus();
            ps.setPilotId(pilotId);
            ps.setMissionCount(0);
            ps.setCreatedAt(LocalDateTime.now());
        }
        if (availability != null) ps.setAvailability(availability);
        if (lat != null) ps.setCurrentLatitude(lat);
        if (lng != null) ps.setCurrentLongitude(lng);
        if (droneType != null) ps.setDroneType(droneType);
        ps.setUpdatedAt(LocalDateTime.now());
        if (ps.getId() == null) {
            pilotStatusMapper.insert(ps);
        } else {
            pilotStatusMapper.updateById(ps);
        }
        return ps;
    }

    @Transactional
    public OrgStatus updateOrgStatus(Long orgId, String availability, Double lat, Double lng) {
        OrgStatus os = orgStatusMapper.selectOne(
                new LambdaQueryWrapper<OrgStatus>().eq(OrgStatus::getOrgId, orgId));
        if (os == null) {
            os = new OrgStatus();
            os.setOrgId(orgId);
            os.setMissionCount(0);
            os.setCreatedAt(LocalDateTime.now());
        }
        if (availability != null) os.setAvailability(availability);
        if (lat != null) os.setLatitude(lat);
        if (lng != null) os.setLongitude(lng);
        os.setUpdatedAt(LocalDateTime.now());
        if (os.getId() == null) {
            orgStatusMapper.insert(os);
        } else {
            orgStatusMapper.updateById(os);
        }
        return os;
    }

    private List<Map<String, Object>> scorePilots(Mission mission) {
        List<Pilot> approvedPilots = pilotMapper.selectList(
                new LambdaQueryWrapper<Pilot>().eq(Pilot::getStatus, "APPROVED"));

        List<Map<String, Object>> results = new ArrayList<>();
        for (Pilot pilot : approvedPilots) {
            Map<String, Object> scoreMap = new HashMap<>();
            scoreMap.put("pilotId", pilot.getId());
            scoreMap.put("pilotName", pilot.getFullName());

            PilotStatus ps = pilotStatusMapper.selectOne(
                    new LambdaQueryWrapper<PilotStatus>().eq(PilotStatus::getPilotId, pilot.getId()));

            double distanceScore = 0;
            double statusScore = 0;
            double certScore = 0;
            double loadScore = 0;

            if (ps != null) {
                if ("IDLE".equals(ps.getAvailability())) {
                    statusScore = 30;
                } else if ("ON_MISSION".equals(ps.getAvailability())) {
                    statusScore = 0;
                } else {
                    statusScore = 5;
                }

                if (ps.getCurrentLatitude() != null && ps.getCurrentLongitude() != null
                        && mission.getLatitude() != null && mission.getLongitude() != null) {
                    double dist = haversine(ps.getCurrentLatitude(), ps.getCurrentLongitude(),
                            mission.getLatitude(), mission.getLongitude());
                    scoreMap.put("distanceKm", Math.round(dist * 10.0) / 10.0);
                    distanceScore = Math.max(0, 40 - dist * 0.5);
                }

                if (mission.getRequiredDroneType() != null && ps.getDroneType() != null) {
                    if (ps.getDroneType().contains(mission.getRequiredDroneType())) {
                        scoreMap.put("droneMatch", true);
                    }
                }

                loadScore = Math.max(0, 15 - ps.getMissionCount() * 2);
            } else {
                statusScore = 10;
            }

            if (mission.getRequiredCertTypes() != null) {
                List<PilotCertificate> certs = certMapper.selectList(
                        new LambdaQueryWrapper<PilotCertificate>().eq(PilotCertificate::getPilotId, pilot.getId()));
                Set<String> heldCerts = certs.stream().map(PilotCertificate::getCertType).collect(Collectors.toSet());
                String[] required = mission.getRequiredCertTypes().split(",");
                int matched = 0;
                for (String req : required) {
                    if (heldCerts.contains(req.trim())) matched++;
                }
                certScore = required.length > 0 ? 15.0 * matched / required.length : 0;
                scoreMap.put("certMatched", matched);
                scoreMap.put("certRequired", required.length);
            }

            double total = distanceScore + statusScore + certScore + loadScore;
            scoreMap.put("distanceScore", Math.round(distanceScore * 100.0) / 100.0);
            scoreMap.put("statusScore", statusScore);
            scoreMap.put("certScore", Math.round(certScore * 100.0) / 100.0);
            scoreMap.put("loadScore", loadScore);
            scoreMap.put("totalScore", Math.round(total * 100.0) / 100.0);

            if (ps == null || "IDLE".equals(ps.getAvailability())) {
                results.add(scoreMap);
            }
        }

        results.sort((a, b) -> Double.compare(
                ((Number) b.get("totalScore")).doubleValue(),
                ((Number) a.get("totalScore")).doubleValue()));

        return results.stream().limit(5).collect(Collectors.toList());
    }

    private List<Map<String, Object>> scoreOrgs(Mission mission) {
        List<Organization> approvedOrgs = orgMapper.selectList(
                new LambdaQueryWrapper<Organization>().eq(Organization::getStatus, "APPROVED"));

        List<Map<String, Object>> results = new ArrayList<>();
        for (Organization org : approvedOrgs) {
            Map<String, Object> scoreMap = new HashMap<>();
            scoreMap.put("orgId", org.getId());
            scoreMap.put("orgName", org.getName());

            OrgStatus os = orgStatusMapper.selectOne(
                    new LambdaQueryWrapper<OrgStatus>().eq(OrgStatus::getOrgId, org.getId()));

            double distanceScore = 0;
            double statusScore = 0;
            double loadScore = 0;

            if (os != null) {
                if ("IDLE".equals(os.getAvailability())) {
                    statusScore = 30;
                } else if ("ON_MISSION".equals(os.getAvailability())) {
                    statusScore = 0;
                } else {
                    statusScore = 5;
                }

                if (os.getLatitude() != null && os.getLongitude() != null
                        && mission.getLatitude() != null && mission.getLongitude() != null) {
                    double dist = haversine(os.getLatitude(), os.getLongitude(),
                            mission.getLatitude(), mission.getLongitude());
                    scoreMap.put("distanceKm", Math.round(dist * 10.0) / 10.0);
                    distanceScore = Math.max(0, 40 - dist * 0.5);
                }

                loadScore = Math.max(0, 30 - os.getMissionCount() * 3);
            } else {
                statusScore = 10;
            }

            double total = distanceScore + statusScore + loadScore;
            scoreMap.put("distanceScore", Math.round(distanceScore * 100.0) / 100.0);
            scoreMap.put("statusScore", statusScore);
            scoreMap.put("loadScore", loadScore);
            scoreMap.put("totalScore", Math.round(total * 100.0) / 100.0);

            if (os == null || "IDLE".equals(os.getAvailability())) {
                results.add(scoreMap);
            }
        }

        results.sort((a, b) -> Double.compare(
                ((Number) b.get("totalScore")).doubleValue(),
                ((Number) a.get("totalScore")).doubleValue()));

        return results.stream().limit(5).collect(Collectors.toList());
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private void rejectOtherAssignments(Long missionId, Long acceptedId) {
        List<MissionAssignment> others = assignmentMapper.selectList(
                new LambdaQueryWrapper<MissionAssignment>()
                        .eq(MissionAssignment::getMissionId, missionId)
                        .ne(MissionAssignment::getId, acceptedId)
                        .eq(MissionAssignment::getStatus, "PENDING"));
        for (MissionAssignment ma : others) {
            ma.setStatus("REJECTED");
            ma.setRespondedAt(LocalDateTime.now());
            assignmentMapper.updateById(ma);
        }
    }

    private void addLog(Long missionId, String action, Long operatorId, String comment, String attachmentUrl) {
        MissionLog log = new MissionLog();
        log.setMissionId(missionId);
        log.setAction(action);
        log.setOperatorId(operatorId);
        log.setCommentText(comment);
        log.setAttachmentUrl(attachmentUrl);
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
    }

    @Transactional
    public MissionAssignment orgDispatchPilot(Long missionId, Long orgId, Long pilotId, SecurityUser user) {
        Mission mission = getById(missionId);

        MissionAssignment orgAssignment = assignmentMapper.selectOne(
                new LambdaQueryWrapper<MissionAssignment>()
                        .eq(MissionAssignment::getMissionId, missionId)
                        .eq(MissionAssignment::getAssigneeType, "ORG")
                        .eq(MissionAssignment::getAssigneeId, orgId)
                        .eq(MissionAssignment::getStatus, "ACCEPTED"));
        if (orgAssignment == null) {
            throw new BusinessException(403, "该单位未被派单到该任务或尚未接单");
        }

        PilotOrg pilotOrg = pilotOrgMapper.selectOne(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getPilotId, pilotId)
                        .eq(PilotOrg::getOrgId, orgId)
                        .eq(PilotOrg::getStatus, "ACTIVE"));
        if (pilotOrg == null) {
            throw new BusinessException(403, "该飞手不属于本单位");
        }

        PilotStatus pilotStatus = pilotStatusMapper.selectOne(
                new LambdaQueryWrapper<PilotStatus>().eq(PilotStatus::getPilotId, pilotId));
        if (pilotStatus == null || !"IDLE".equals(pilotStatus.getAvailability())) {
            throw new BusinessException(400, "该飞手当前不可用");
        }

        MissionAssignment existingPilotAssign = assignmentMapper.selectOne(
                new LambdaQueryWrapper<MissionAssignment>()
                        .eq(MissionAssignment::getMissionId, missionId)
                        .eq(MissionAssignment::getAssigneeType, "PILOT")
                        .eq(MissionAssignment::getAssigneeId, pilotId)
                        .ne(MissionAssignment::getStatus, "REJECTED"));
        if (existingPilotAssign != null) {
            throw new BusinessException(400, "该飞手已有该任务的派单记录");
        }

        Pilot pilot = pilotMapper.selectById(pilotId);
        String pilotName = pilot != null ? pilot.getFullName() : "飞手#" + pilotId;

        MissionAssignment assignment = new MissionAssignment();
        assignment.setMissionId(missionId);
        assignment.setAssigneeType("PILOT");
        assignment.setAssigneeId(pilotId);
        assignment.setStatus("ACCEPTED");
        assignment.setMatchScore(0.0);
        assignment.setMatchDetail("{\"orgDispatch\":true,\"orgId\":" + orgId + ",\"pilotName\":\"" + pilotName + "\"}");
        assignment.setRankOrder(1);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setRespondedAt(LocalDateTime.now());
        assignmentMapper.insert(assignment);

        pilotStatus.setAvailability("ON_MISSION");
        pilotStatus.setActiveMissionId(missionId);
        pilotStatus.setUpdatedAt(LocalDateTime.now());
        pilotStatusMapper.updateById(pilotStatus);

        SysUser targetUser = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getPilotId, pilotId)
                        .eq(SysUser::getEnabled, 1)
                        .last("LIMIT 1"));
        if (targetUser != null) {
            notificationService.sendToUser(targetUser.getId(), "MISSION", missionId,
                "单位指派任务", "您已被单位指派执行任务: " + mission.getTitle(), "MISSION");
        }

        addLog(missionId, "ORG_DISPATCH", user.getUserId(),
                "单位指派飞手" + pilotName + "执行任务", null);

        return assignment;
    }

    @Transactional
    public MissionAssignment orgAutoDispatchPilot(Long missionId, Long orgId, SecurityUser user) {
        Mission mission = getById(missionId);

        MissionAssignment orgAssignment = assignmentMapper.selectOne(
                new LambdaQueryWrapper<MissionAssignment>()
                        .eq(MissionAssignment::getMissionId, missionId)
                        .eq(MissionAssignment::getAssigneeType, "ORG")
                        .eq(MissionAssignment::getAssigneeId, orgId)
                        .eq(MissionAssignment::getStatus, "ACCEPTED"));
        if (orgAssignment == null) {
            throw new BusinessException(403, "该单位未被派单到该任务或尚未接单");
        }

        List<PilotOrg> activePilotOrgs = pilotOrgMapper.selectList(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getOrgId, orgId)
                        .eq(PilotOrg::getStatus, "ACTIVE"));

        List<Map<String, Object>> scoredPilots = new ArrayList<>();
        for (PilotOrg po : activePilotOrgs) {
            PilotStatus ps = pilotStatusMapper.selectOne(
                    new LambdaQueryWrapper<PilotStatus>().eq(PilotStatus::getPilotId, po.getPilotId()));
            if (ps == null || !"IDLE".equals(ps.getAvailability())) {
                continue;
            }

            MissionAssignment existingAssign = assignmentMapper.selectOne(
                    new LambdaQueryWrapper<MissionAssignment>()
                            .eq(MissionAssignment::getMissionId, missionId)
                            .eq(MissionAssignment::getAssigneeType, "PILOT")
                            .eq(MissionAssignment::getAssigneeId, po.getPilotId())
                            .ne(MissionAssignment::getStatus, "REJECTED"));
            if (existingAssign != null) {
                continue;
            }

            double distanceScore = 0;
            double statusScore = 30;
            double loadScore = 0;

            if (ps.getCurrentLatitude() != null && ps.getCurrentLongitude() != null
                    && mission.getLatitude() != null && mission.getLongitude() != null) {
                double dist = haversine(ps.getCurrentLatitude(), ps.getCurrentLongitude(),
                        mission.getLatitude(), mission.getLongitude());
                distanceScore = Math.max(0, 40 - dist * 0.5);
            }

            loadScore = Math.max(0, 15 - (ps.getMissionCount() != null ? ps.getMissionCount() : 0) * 2);

            double totalScore = distanceScore + statusScore + loadScore;

            Map<String, Object> scoreMap = new HashMap<>();
            scoreMap.put("pilotId", po.getPilotId());
            scoreMap.put("distanceScore", Math.round(distanceScore * 100.0) / 100.0);
            scoreMap.put("statusScore", statusScore);
            scoreMap.put("loadScore", loadScore);
            scoreMap.put("totalScore", Math.round(totalScore * 100.0) / 100.0);
            scoredPilots.add(scoreMap);
        }

        if (scoredPilots.isEmpty()) {
            throw new BusinessException(400, "该单位当前没有可用飞手");
        }

        scoredPilots.sort((a, b) -> Double.compare(
                ((Number) b.get("totalScore")).doubleValue(),
                ((Number) a.get("totalScore")).doubleValue()));

        Long bestPilotId = ((Number) scoredPilots.get(0).get("pilotId")).longValue();

        return orgDispatchPilot(missionId, orgId, bestPilotId, user);
    }

    public List<Map<String, Object>> getOrgAvailablePilots(Long orgId) {
        List<PilotOrg> activePilotOrgs = pilotOrgMapper.selectList(
                new LambdaQueryWrapper<PilotOrg>()
                        .eq(PilotOrg::getOrgId, orgId)
                        .eq(PilotOrg::getStatus, "ACTIVE"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (PilotOrg po : activePilotOrgs) {
            PilotStatus ps = pilotStatusMapper.selectOne(
                    new LambdaQueryWrapper<PilotStatus>().eq(PilotStatus::getPilotId, po.getPilotId()));
            if (ps == null || !"IDLE".equals(ps.getAvailability())) {
                continue;
            }

            Pilot pilot = pilotMapper.selectById(po.getPilotId());
            Map<String, Object> item = new HashMap<>();
            item.put("pilotId", po.getPilotId());
            item.put("pilotName", pilot != null ? pilot.getFullName() : "飞手#" + po.getPilotId());
            item.put("phone", pilot != null ? pilot.getPhone() : null);
            item.put("availability", ps.getAvailability());
            item.put("currentLatitude", ps.getCurrentLatitude());
            item.put("currentLongitude", ps.getCurrentLongitude());
            item.put("droneType", ps.getDroneType());
            item.put("missionCount", ps.getMissionCount());
            result.add(item);
        }
        return result;
    }
}
