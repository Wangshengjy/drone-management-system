package com.drone.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.collab.entity.ReviewRuleConfig;
import com.drone.collab.mapper.ReviewRuleConfigMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewConfigService {

    public static final String KEY_DEFAULT = "default";

    private final ReviewRuleConfigMapper reviewRuleConfigMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReviewConfigService(ReviewRuleConfigMapper reviewRuleConfigMapper) {
        this.reviewRuleConfigMapper = reviewRuleConfigMapper;
    }

    public Map<String, Object> getDefaultConfigMap() {
        ReviewRuleConfig row = reviewRuleConfigMapper.selectOne(
                new LambdaQueryWrapper<ReviewRuleConfig>().eq(ReviewRuleConfig::getConfigKey, KEY_DEFAULT));
        if (row == null) {
            return defaultJsonMap();
        }
        try {
            return objectMapper.readValue(row.getConfigValue(), new TypeReference<>() {
            });
        } catch (Exception e) {
            return defaultJsonMap();
        }
    }

    public void saveDefault(String json) {
        ReviewRuleConfig row = reviewRuleConfigMapper.selectOne(
                new LambdaQueryWrapper<ReviewRuleConfig>().eq(ReviewRuleConfig::getConfigKey, KEY_DEFAULT));
        LocalDateTime now = LocalDateTime.now();
        if (row == null) {
            row = new ReviewRuleConfig();
            row.setConfigKey(KEY_DEFAULT);
            row.setConfigValue(json);
            row.setUpdatedAt(now);
            reviewRuleConfigMapper.insert(row);
        } else {
            row.setConfigValue(json);
            row.setUpdatedAt(now);
            reviewRuleConfigMapper.updateById(row);
        }
    }

    private Map<String, Object> defaultJsonMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("passThreshold", 70);
        m.put("manualThreshold", 45);
        m.put("softRejectBelow", 30);
        m.put("blacklistKeywords", List.of("黑名单", "非法"));
        m.put("weights", Map.of(
                "basicFields", 0.25,
                "certificates", 0.35,
                "recordNo", 0.2,
                "region", 0.1,
                "creditCode", 0.1));
        return m;
    }
}
