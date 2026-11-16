package com.drone.collab.service;

import com.drone.collab.domain.Statuses;
import com.drone.collab.entity.Organization;
import com.drone.collab.entity.Pilot;
import com.drone.collab.entity.PilotCertificate;
import com.drone.collab.mapper.OrganizationMapper;
import com.drone.collab.mapper.PilotCertificateMapper;
import com.drone.collab.mapper.PilotMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报名自动预审：基于可配置权重与多维度分项得分，对飞手/单位资料做加权综合评分，并结合阈值划分
 * 「通过 / 待人工复核 / 驳回」区间。该思路对应多目标决策中的加权标量化与约束处理，便于在管理后台
 * 调整权重与阈值以优化筛选策略（与人工智能优化中的目标加权、可行域裁剪思想一致）。
 */
@Service
public class AutoReviewService {

    private final PilotMapper pilotMapper;
    private final PilotCertificateMapper pilotCertificateMapper;
    private final OrganizationMapper organizationMapper;
    private final ReviewConfigService reviewConfigService;

    public AutoReviewService(
            PilotMapper pilotMapper,
            PilotCertificateMapper pilotCertificateMapper,
            OrganizationMapper organizationMapper,
            ReviewConfigService reviewConfigService) {
        this.pilotMapper = pilotMapper;
        this.pilotCertificateMapper = pilotCertificateMapper;
        this.organizationMapper = organizationMapper;
        this.reviewConfigService = reviewConfigService;
    }

    public AutoOutcome reviewPilot(Long pilotId) {
        Pilot p = pilotMapper.selectById(pilotId);
        if (p == null) {
            return AutoOutcome.reject("飞手不存在");
        }
        Map<String, Object> cfg = reviewConfigService.getDefaultConfigMap();
        List<String> blacklist = asStringList(cfg.get("blacklistKeywords"));
        int passTh = intVal(cfg.get("passThreshold"), 70);
        int manualTh = intVal(cfg.get("manualThreshold"), 45);
        int softReject = intVal(cfg.get("softRejectBelow"), 30);
        @SuppressWarnings("unchecked")
        Map<String, Object> weights = (Map<String, Object>) cfg.getOrDefault("weights", Map.of());

        List<String> reasons = new ArrayList<>();

        for (String kw : blacklist) {
            if (kw != null && !kw.isEmpty()) {
                if (contains(p.getFullName(), kw) || contains(p.getRegion(), kw)) {
                    reasons.add("命中黑名单关键词: " + kw);
                    return new AutoOutcome(Statuses.AUTO_REJECT, 0, reasons);
                }
            }
        }

        if (isBlank(p.getFullName()) || isBlank(p.getPhone()) || isBlank(p.getRecordNo())) {
            reasons.add("基础必填项缺失(姓名/手机/备案号)");
            return new AutoOutcome(Statuses.AUTO_REJECT, 0, reasons);
        }

        List<PilotCertificate> certs = pilotCertificateMapper.selectList(
                new LambdaQueryWrapper<PilotCertificate>().eq(PilotCertificate::getPilotId, pilotId));
        LocalDate today = LocalDate.now();
        for (PilotCertificate c : certs) {
            if (c.getValidUntil() != null && c.getValidUntil().isBefore(today)) {
                reasons.add("存在过期证书: " + c.getCertType());
                return new AutoOutcome(Statuses.AUTO_REJECT, 0, reasons);
            }
        }

        double wBasic = doubleVal(weights.get("basicFields"), 0.25);
        double wCert = doubleVal(weights.get("certificates"), 0.35);
        double wRecord = doubleVal(weights.get("recordNo"), 0.2);
        double wRegion = doubleVal(weights.get("region"), 0.1);

        int basicScore = scoreBasic(p);
        int certScore = Math.min(100, certs.size() * 34);
        int recordScore = isBlank(p.getRecordNo()) ? 0 : 100;
        int regionScore = isBlank(p.getRegion()) ? 40 : 90;

        double raw = wBasic * basicScore + wCert * certScore + wRecord * recordScore + wRegion * regionScore;
        int score = (int) Math.round(Math.min(100, raw));

        String result;
        if (score >= passTh) {
            result = Statuses.AUTO_PASS;
            reasons.add("加权分通过阈值");
        } else if (score >= manualTh) {
            result = Statuses.AUTO_NEED_MANUAL;
            reasons.add("分数处于人工复核区间");
        } else if (score < softReject) {
            result = Statuses.AUTO_REJECT;
            reasons.add("综合分过低(软规则)");
        } else {
            result = Statuses.AUTO_NEED_MANUAL;
            reasons.add("需人工复核");
        }

        reasons.add("certCount=" + certs.size() + ", score=" + score);
        return new AutoOutcome(result, score, reasons);
    }

    public AutoOutcome reviewOrganization(Long orgId) {
        Organization o = organizationMapper.selectById(orgId);
        if (o == null) {
            return AutoOutcome.reject("单位不存在");
        }
        Map<String, Object> cfg = reviewConfigService.getDefaultConfigMap();
        List<String> blacklist = asStringList(cfg.get("blacklistKeywords"));
        int passTh = intVal(cfg.get("passThreshold"), 70);
        int manualTh = intVal(cfg.get("manualThreshold"), 45);
        int softReject = intVal(cfg.get("softRejectBelow"), 30);
        @SuppressWarnings("unchecked")
        Map<String, Object> weights = (Map<String, Object>) cfg.getOrDefault("weights", Map.of());

        List<String> reasons = new ArrayList<>();
        for (String kw : blacklist) {
            if (kw != null && !kw.isEmpty() && (contains(o.getName(), kw) || contains(o.getAddress(), kw))) {
                reasons.add("命中黑名单关键词: " + kw);
                return new AutoOutcome(Statuses.AUTO_REJECT, 0, reasons);
            }
        }

        if (isBlank(o.getName()) || isBlank(o.getPhone()) || isBlank(o.getCreditCode())) {
            reasons.add("基础必填项缺失(名称/手机/统一社会信用代码)");
            return new AutoOutcome(Statuses.AUTO_REJECT, 0, reasons);
        }

        double wCredit = doubleVal(weights.get("creditCode"), 0.35);
        double wBasic = doubleVal(weights.get("basicFields"), 0.35);
        double wRegion = doubleVal(weights.get("region"), 0.15);

        int creditSc = isBlank(o.getCreditCode()) ? 0 : 100;
        int basicSc = (isBlank(o.getContactName()) ? 60 : 90);
        int addrSc = isBlank(o.getAddress()) ? 50 : 85;

        double raw = wCredit * creditSc + wBasic * basicSc + wRegion * addrSc;
        int score = (int) Math.round(Math.min(100, raw));

        String result;
        if (score >= passTh) {
            result = Statuses.AUTO_PASS;
        } else if (score >= manualTh) {
            result = Statuses.AUTO_NEED_MANUAL;
        } else if (score < softReject) {
            result = Statuses.AUTO_REJECT;
            reasons.add("综合分过低");
        } else {
            result = Statuses.AUTO_NEED_MANUAL;
        }
        reasons.add("score=" + score);
        return new AutoOutcome(result, score, reasons);
    }

    private static int scoreBasic(Pilot p) {
        int n = 0;
        if (!isBlank(p.getFullName())) {
            n += 34;
        }
        if (!isBlank(p.getPhone())) {
            n += 33;
        }
        if (!isBlank(p.getEmail())) {
            n += 33;
        }
        return Math.min(100, n);
    }

    private static boolean contains(String s, String kw) {
        return s != null && kw != null && s.contains(kw);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static int intVal(Object o, int def) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return def;
    }

    private static double doubleVal(Object o, double def) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object o) {
        if (o instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toList());
        }
        return List.of();
    }

    public record AutoOutcome(String autoResult, int score, List<String> reasons) {
        public static AutoOutcome reject(String msg) {
            List<String> r = new ArrayList<>();
            r.add(msg);
            return new AutoOutcome(Statuses.AUTO_REJECT, 0, r);
        }

        public String reasonJson() {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(reasons);
            } catch (Exception e) {
                return reasons.toString();
            }
        }
    }
}
