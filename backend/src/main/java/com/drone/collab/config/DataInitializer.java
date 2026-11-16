package com.drone.collab.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.collab.domain.Statuses;
import com.drone.collab.entity.AuditLog;
import com.drone.collab.entity.Mission;
import com.drone.collab.entity.OrgApplication;
import com.drone.collab.entity.OrgStatus;
import com.drone.collab.entity.Organization;
import com.drone.collab.entity.Pilot;
import com.drone.collab.entity.PilotApplication;
import com.drone.collab.entity.PilotCertificate;
import com.drone.collab.entity.PilotStatus;
import com.drone.collab.entity.ReviewRuleConfig;
import com.drone.collab.entity.SysUser;
import com.drone.collab.mapper.AuditLogMapper;
import com.drone.collab.mapper.MissionAssignmentMapper;
import com.drone.collab.mapper.MissionLogMapper;
import com.drone.collab.mapper.MissionMapper;
import com.drone.collab.mapper.OrgApplicationMapper;
import com.drone.collab.mapper.OrgStatusMapper;
import com.drone.collab.mapper.OrganizationMapper;
import com.drone.collab.mapper.PilotApplicationMapper;
import com.drone.collab.mapper.PilotCertificateMapper;
import com.drone.collab.mapper.PilotMapper;
import com.drone.collab.mapper.PilotStatusMapper;
import com.drone.collab.mapper.ReviewRuleConfigMapper;
import com.drone.collab.mapper.SysUserMapper;
import com.drone.collab.service.ReviewConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final String DEFAULT_REVIEW_JSON =
            "{\"passThreshold\":70,\"manualThreshold\":45,\"softRejectBelow\":30,"
                    + "\"blacklistKeywords\":[\"blacklist\",\"illegal\"],"
                    + "\"weights\":{\"basicFields\":0.25,\"certificates\":0.35,\"recordNo\":0.2,\"region\":0.1,\"creditCode\":0.1}}";

    private static final String DEMO_PASSWORD = "123456";

    private final SysUserMapper sysUserMapper;
    private final ReviewRuleConfigMapper reviewRuleConfigMapper;
    private final PilotMapper pilotMapper;
    private final OrganizationMapper organizationMapper;
    private final PilotCertificateMapper pilotCertificateMapper;
    private final PilotApplicationMapper pilotApplicationMapper;
    private final OrgApplicationMapper orgApplicationMapper;
    private final AuditLogMapper auditLogMapper;
    private final PilotStatusMapper pilotStatusMapper;
    private final OrgStatusMapper orgStatusMapper;
    private final MissionMapper missionMapper;
    private final MissionAssignmentMapper missionAssignmentMapper;
    private final MissionLogMapper missionLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Value("${app.bootstrap.admin-password:123456}")
    private String adminPassword;

    public DataInitializer(
            SysUserMapper sysUserMapper,
            ReviewRuleConfigMapper reviewRuleConfigMapper,
            PilotMapper pilotMapper,
            OrganizationMapper organizationMapper,
            PilotCertificateMapper pilotCertificateMapper,
            PilotApplicationMapper pilotApplicationMapper,
            OrgApplicationMapper orgApplicationMapper,
            AuditLogMapper auditLogMapper,
            PilotStatusMapper pilotStatusMapper,
            OrgStatusMapper orgStatusMapper,
            MissionMapper missionMapper,
            MissionAssignmentMapper missionAssignmentMapper,
            MissionLogMapper missionLogMapper,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            DataSource dataSource) {
        this.sysUserMapper = sysUserMapper;
        this.reviewRuleConfigMapper = reviewRuleConfigMapper;
        this.pilotMapper = pilotMapper;
        this.organizationMapper = organizationMapper;
        this.pilotCertificateMapper = pilotCertificateMapper;
        this.pilotApplicationMapper = pilotApplicationMapper;
        this.orgApplicationMapper = orgApplicationMapper;
        this.auditLogMapper = auditLogMapper;
        this.pilotStatusMapper = pilotStatusMapper;
        this.orgStatusMapper = orgStatusMapper;
        this.missionMapper = missionMapper;
        this.missionAssignmentMapper = missionAssignmentMapper;
        this.missionLogMapper = missionLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureSchemaReady();
        seedReviewConfig();
        SysUser admin = seedAdmin();
        seedDemoData(admin);
    }

    private void ensureSchemaReady() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()",
                Integer.class);
        if (tableCount == null || tableCount == 0) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("db/schema.sql"));
            populator.setSqlScriptEncoding("UTF-8");
            DatabasePopulatorUtils.execute(populator, dataSource);
            return;
        }
        if (tableExists("users") && tableExists("review_rule_config")) {
            ensureNewTables();
            return;
        }
        throw new IllegalStateException("Database schema is incomplete. Please initialize required tables before startup.");
    }

    private void ensureNewTables() {
        if (!tableExists("pilot_org")) {
            jdbcTemplate.execute(
                "CREATE TABLE `pilot_org` (" +
                "`id` BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "`pilot_id` BIGINT NOT NULL, " +
                "`org_id` BIGINT NOT NULL, " +
                "`status` VARCHAR(32) NOT NULL DEFAULT 'PENDING', " +
                "`initiator` VARCHAR(16) NOT NULL DEFAULT 'ORG', " +
                "`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "`updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "UNIQUE KEY `uk_pilot_org` (`pilot_id`, `org_id`), " +
                "KEY `idx_pilot_org_pilot` (`pilot_id`), " +
                "KEY `idx_pilot_org_org` (`org_id`), " +
                "KEY `idx_pilot_org_status` (`status`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private void seedReviewConfig() {
        long count = reviewRuleConfigMapper.selectCount(
                new LambdaQueryWrapper<ReviewRuleConfig>()
                        .eq(ReviewRuleConfig::getConfigKey, ReviewConfigService.KEY_DEFAULT));
        if (count > 0) {
            return;
        }
        ReviewRuleConfig row = new ReviewRuleConfig();
        row.setConfigKey(ReviewConfigService.KEY_DEFAULT);
        row.setConfigValue(DEFAULT_REVIEW_JSON);
        row.setUpdatedAt(LocalDateTime.now());
        reviewRuleConfigMapper.insert(row);
    }

    private SysUser seedAdmin() {
        SysUser admin = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin").last("limit 1"));
        if (admin != null) {
            boolean changed = false;
            if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(adminPassword));
                changed = true;
            }
            if (!"ADMIN".equals(admin.getRole())) {
                admin.setRole("ADMIN");
                changed = true;
            }
            if (admin.getEnabled() == null || admin.getEnabled() == 0) {
                admin.setEnabled(1);
                changed = true;
            }
            if (changed) {
                sysUserMapper.updateById(admin);
            }
            return admin;
        }

        SysUser user = new SysUser();
        user.setUsername("admin");
        user.setPassword(passwordEncoder.encode(adminPassword));
        user.setRole("ADMIN");
        user.setEnabled(1);
        user.setCreatedAt(LocalDateTime.now());
        sysUserMapper.insert(user);
        return user;
    }

    private void seedDemoData(SysUser admin) {
        ensurePilotDemo(
                admin,
                new DemoPilotSpec(
                        "pilot_sample_alpha",
                        "Pilot Alpha",
                        "13800010001",
                        "alpha@example.com",
                        "110101199001010011",
                        "UAS-SAMPLE-ALPHA",
                        "Shanghai",
                        Statuses.APPROVED,
                        Statuses.AUTO_PASS,
                        91,
                        LocalDateTime.now().minusMonths(5),
                        List.of(
                                new DemoCertSpec("AOPA", "AOPA-ALPHA-01", LocalDate.now().plusYears(2)),
                                new DemoCertSpec("UTC-SURVEY", "UTC-ALPHA-02", LocalDate.now().plusYears(1)))),
                "APPROVE");
        ensurePilotDemo(
                admin,
                new DemoPilotSpec(
                        "pilot_sample_bravo",
                        "Pilot Bravo",
                        "13800010002",
                        "bravo@example.com",
                        "110101199001010022",
                        "UAS-SAMPLE-BRAVO",
                        "Zhejiang",
                        Statuses.APPROVED,
                        Statuses.AUTO_NEED_MANUAL,
                        66,
                        LocalDateTime.now().minusMonths(3),
                        List.of(new DemoCertSpec("CAAC", "CAAC-BRAVO-01", LocalDate.now().plusYears(3)))),
                "APPROVE");
        ensurePilotDemo(
                admin,
                new DemoPilotSpec(
                        "pilot_sample_charlie",
                        "Pilot Charlie",
                        "13800010003",
                        "charlie@example.com",
                        "110101199001010033",
                        "UAS-SAMPLE-CHARLIE",
                        "Beijing",
                        Statuses.PENDING_REVIEW,
                        Statuses.AUTO_NEED_MANUAL,
                        58,
                        LocalDateTime.now().minusDays(10),
                        List.of(new DemoCertSpec("AOPA", "AOPA-CHARLIE-01", LocalDate.now().plusYears(2)))),
                null);
        ensurePilotDemo(
                admin,
                new DemoPilotSpec(
                        "pilot_sample_delta",
                        "Pilot Delta",
                        "13800010004",
                        "delta@example.com",
                        "110101199001010044",
                        "UAS-SAMPLE-DELTA",
                        "Guangdong",
                        Statuses.REJECTED,
                        Statuses.AUTO_REJECT,
                        24,
                        LocalDateTime.now().minusMonths(1),
                        List.of()),
                "REJECT");
        ensurePilotDemo(
                admin,
                new DemoPilotSpec(
                        "pilot_sample_echo",
                        "Pilot Echo",
                        "13800010005",
                        "echo@example.com",
                        "310101199203020055",
                        "UAS-SAMPLE-ECHO",
                        "Jiangsu",
                        Statuses.APPROVED,
                        Statuses.AUTO_PASS,
                        85,
                        LocalDateTime.now().minusMonths(6),
                        List.of(new DemoCertSpec("AOPA", "AOPA-ECHO-01", LocalDate.now().plusYears(1)))),
                "APPROVE");
        ensurePilotDemo(
                admin,
                new DemoPilotSpec(
                        "pilot_sample_foxtrot",
                        "Pilot Foxtrot",
                        "13800010006",
                        "foxtrot@example.com",
                        "440101199304030066",
                        "UAS-SAMPLE-FOXTROT",
                        "Sichuan",
                        Statuses.DRAFT,
                        null,
                        null,
                        LocalDateTime.now().minusDays(2),
                        List.of()),
                null);
        ensurePilotDemo(
                admin,
                new DemoPilotSpec(
                        "pilot_sample_golf",
                        "Pilot Golf",
                        "13800010007",
                        "golf@example.com",
                        "510101199405040077",
                        "UAS-SAMPLE-GOLF",
                        "Hubei",
                        Statuses.PENDING_REVIEW,
                        Statuses.AUTO_NEED_MANUAL,
                        52,
                        LocalDateTime.now().minusDays(5),
                        List.of(new DemoCertSpec("CAAC", "CAAC-GOLF-01", LocalDate.now().plusYears(2)))),
                null);
        ensurePilotDemo(
                admin,
                new DemoPilotSpec(
                        "pilot_sample_hotel",
                        "Pilot Hotel",
                        "13800010008",
                        "hotel@example.com",
                        "330101199506050088",
                        "UAS-SAMPLE-HOTEL",
                        "Fujian",
                        Statuses.FROZEN,
                        Statuses.AUTO_PASS,
                        80,
                        LocalDateTime.now().minusMonths(8),
                        List.of(
                                new DemoCertSpec("AOPA", "AOPA-HOTEL-01", LocalDate.now().plusYears(1)),
                                new DemoCertSpec("UTC-SURVEY", "UTC-HOTEL-02", LocalDate.now().plusMonths(6)))),
                "APPROVE");
        ensurePilotDemo(
                admin,
                new DemoPilotSpec(
                        "pilot_sample_india",
                        "Pilot India",
                        "13800010009",
                        "india@example.com",
                        "320101199607060099",
                        "UAS-SAMPLE-INDIA",
                        "Shandong",
                        Statuses.REJECTED,
                        Statuses.AUTO_NEED_MANUAL,
                        48,
                        LocalDateTime.now().minusWeeks(3),
                        List.of(new DemoCertSpec("AOPA", "AOPA-INDIA-01", LocalDate.now().plusYears(1)))),
                "REJECT");
        ensurePilotDemo(
                admin,
                new DemoPilotSpec(
                        "pilot_sample_juliet",
                        "Pilot Juliet",
                        "13800010010",
                        "juliet@example.com",
                        "210101199708070100",
                        "UAS-SAMPLE-JULIET",
                        "Liaoning",
                        Statuses.APPROVED,
                        Statuses.AUTO_PASS,
                        93,
                        LocalDateTime.now().minusMonths(7),
                        List.of(
                                new DemoCertSpec("CAAC", "CAAC-JULIET-01", LocalDate.now().plusYears(3)),
                                new DemoCertSpec("AOPA", "AOPA-JULIET-02", LocalDate.now().plusYears(2)),
                                new DemoCertSpec("UTC-SURVEY", "UTC-JULIET-03", LocalDate.now().plusYears(1)))),
                "APPROVE");

        ensureOrgDemo(
                admin,
                new DemoOrgSpec(
                        "org_sample_horizon",
                        "Horizon Rescue",
                        "ENTERPRISE",
                        "91310000SAMPLE0001",
                        "Alex Chen",
                        "02100010001",
                        "Shanghai Pudong 100",
                        Statuses.APPROVED,
                        Statuses.AUTO_PASS,
                        88,
                        LocalDateTime.now().minusMonths(4)),
                "APPROVE");
        ensureOrgDemo(
                admin,
                new DemoOrgSpec(
                        "org_sample_sky",
                        "Sky Volunteers",
                        "VOLUNTEER",
                        "91310000SAMPLE0002",
                        "Mia Lin",
                        "02100010002",
                        "Suzhou Industrial Park 28",
                        Statuses.PENDING_REVIEW,
                        Statuses.AUTO_NEED_MANUAL,
                        61,
                        LocalDateTime.now().minusDays(7)),
                null);
        ensureOrgDemo(
                admin,
                new DemoOrgSpec(
                        "org_sample_field",
                        "Field Survey Lab",
                        "OTHER",
                        "91310000SAMPLE0003",
                        "Noah Wu",
                        "02100010003",
                        "Hangzhou Future City 9",
                        Statuses.REJECTED,
                        Statuses.AUTO_REJECT,
                        29,
                        LocalDateTime.now().minusMonths(2)),
                "REJECT");
        ensureOrgDemo(
                admin,
                new DemoOrgSpec(
                        "org_sample_eagle",
                        "Eagle Aviation Tech",
                        "ENTERPRISE",
                        "91440100SAMPLE0004",
                        "Leo Zhang",
                        "02000010004",
                        "Guangzhou Tianhe 55",
                        Statuses.APPROVED,
                        Statuses.AUTO_PASS,
                        90,
                        LocalDateTime.now().minusMonths(9)),
                "APPROVE");
        ensureOrgDemo(
                admin,
                new DemoOrgSpec(
                        "org_sample_river",
                        "River Emergency Team",
                        "VOLUNTEER",
                        "91510100SAMPLE0005",
                        "Sara Li",
                        "02800010005",
                        "Chengdu High-Tech 12",
                        Statuses.DRAFT,
                        null,
                        null,
                        LocalDateTime.now().minusDays(3)),
                null);
        ensureOrgDemo(
                admin,
                new DemoOrgSpec(
                        "org_sample_mountain",
                        "Mountain Relief Org",
                        "VOLUNTEER",
                        "91510700SAMPLE0006",
                        "Jay Wang",
                        "08160001006",
                        "Mianyang Science City 3",
                        Statuses.PENDING_REVIEW,
                        Statuses.AUTO_NEED_MANUAL,
                        55,
                        LocalDateTime.now().minusDays(4)),
                null);
        ensureOrgDemo(
                admin,
                new DemoOrgSpec(
                        "org_sample_ocean",
                        "Ocean Drone Services",
                        "ENTERPRISE",
                        "91350100SAMPLE0007",
                        "Amy Huang",
                        "05910001007",
                        "Fuzhou Gulou 88",
                        Statuses.APPROVED,
                        Statuses.AUTO_NEED_MANUAL,
                        68,
                        LocalDateTime.now().minusMonths(5)),
                "APPROVE");
        ensureOrgDemo(
                admin,
                new DemoOrgSpec(
                        "org_sample_forest",
                        "Forest Patrol Inc",
                        "ENTERPRISE",
                        "91533000SAMPLE0008",
                        "Tom Xu",
                        "08710001008",
                        "Kunming Panlong 22",
                        Statuses.FROZEN,
                        Statuses.AUTO_PASS,
                        82,
                        LocalDateTime.now().minusMonths(10)),
                "APPROVE");
        ensureOrgDemo(
                admin,
                new DemoOrgSpec(
                        "org_sample_cloud",
                        "Cloud Mapping Co",
                        "ENTERPRISE",
                        "91320100SAMPLE0009",
                        "Ivy Zhou",
                        "02500001009",
                        "Nanjing Xuanwu 16",
                        Statuses.REJECTED,
                        Statuses.AUTO_NEED_MANUAL,
                        42,
                        LocalDateTime.now().minusWeeks(2)),
                "REJECT");
        ensureOrgDemo(
                admin,
                new DemoOrgSpec(
                        "org_sample_star",
                        "Star Volunteer Hub",
                        "VOLUNTEER",
                        "91210100SAMPLE0010",
                        "Ben Liu",
                        "02400001010",
                        "Shenyang Heping 7",
                        Statuses.APPROVED,
                        Statuses.AUTO_PASS,
                        95,
                        LocalDateTime.now().minusMonths(11)),
                "APPROVE");

        seedMissionDemoData(admin);
    }

    private void seedMissionDemoData(SysUser admin) {
        if (missionMapper.selectCount(new LambdaQueryWrapper<>()) > 0) {
            return;
        }

        List<Pilot> approvedPilots = pilotMapper.selectList(
                new LambdaQueryWrapper<Pilot>().eq(Pilot::getStatus, Statuses.APPROVED));
        List<Organization> approvedOrgs = organizationMapper.selectList(
                new LambdaQueryWrapper<Organization>().eq(Organization::getStatus, Statuses.APPROVED));

        double[][] pilotCoords = {
                {31.2304, 121.4737}, {30.2741, 120.1551}, {39.9042, 116.4074},
                {32.0603, 118.7969}, {41.8057, 123.4315}
        };
        String[] droneTypes = {"多旋翼-载重", "多旋翼-轻型", "固定翼-测绘", "多旋翼-载重", "固定翼-侦察"};

        for (int i = 0; i < approvedPilots.size(); i++) {
            Pilot p = approvedPilots.get(i);
            PilotStatus ps = new PilotStatus();
            ps.setPilotId(p.getId());
            ps.setAvailability(i < 3 ? "IDLE" : (i < 4 ? "ON_MISSION" : "LEAVE"));
            double[] coord = pilotCoords[i % pilotCoords.length];
            ps.setCurrentLatitude(coord[0]);
            ps.setCurrentLongitude(coord[1]);
            ps.setDroneType(droneTypes[i % droneTypes.length]);
            ps.setMissionCount(i * 2);
            ps.setActiveMissionId(i == 3 ? -1L : null);
            ps.setUpdatedAt(LocalDateTime.now());
            pilotStatusMapper.insert(ps);
        }

        double[][] orgCoords = {
                {31.2304, 121.4737}, {23.1291, 113.2644}, {30.5728, 104.0668},
                {26.0745, 119.2965}, {41.8057, 123.4315}
        };

        for (int i = 0; i < approvedOrgs.size(); i++) {
            Organization o = approvedOrgs.get(i);
            OrgStatus os = new OrgStatus();
            os.setOrgId(o.getId());
            os.setAvailability(i < 3 ? "IDLE" : (i < 4 ? "ON_MISSION" : "OFFLINE"));
            double[] coord = orgCoords[i % orgCoords.length];
            os.setLatitude(coord[0]);
            os.setLongitude(coord[1]);
            os.setMissionCount(i + 1);
            os.setActiveMissionId(i == 3 ? -1L : null);
            os.setUpdatedAt(LocalDateTime.now());
            orgStatusMapper.insert(os);
        }

        String[][] missionData = {
                {"森林火灾紧急救援", "浙江山区突发森林火灾，需无人机空中监测火势蔓延方向", "FIRE_FIGHTING", "RED", "30.2741", "120.1551", "浙江杭州西湖山区", "AOPA,UTC-SURVEY", "多旋翼-载重"},
                {"洪涝灾害搜救", "持续暴雨导致洪涝，需无人机搜救被困群众", "FLOOD_RESCUE", "RED", "30.5728", "104.0668", "四川成都金堂县", "AOPA,CAAC", "多旋翼-载重"},
                {"交通事故现场勘察", "高速公路发生重大交通事故，需无人机航拍现场", "ACCIDENT_SURVEY", "YELLOW", "31.2304", "121.4737", "上海浦东新区", "UTC-SURVEY", "多旋翼-轻型"},
                {"化工厂泄漏监测", "化工厂发生有害气体泄漏，需无人机空中采样监测", "OTHER", "RED", "23.1291", "113.2644", "广东广州黄埔区", "AOPA", "固定翼-测绘"},
                {"地震灾后评估", "地震后需对建筑损伤进行航拍评估", "OTHER", "YELLOW", "26.0745", "119.2965", "福建福州闽侯县", "UTC-SURVEY,CAAC", "固定翼-侦察"},
                {"山体滑坡监测", "连续降雨导致山体滑坡风险，需持续监测", "OTHER", "BLUE", "30.2741", "120.1551", "浙江杭州临安区", "UTC-SURVEY", "多旋翼-轻型"},
                {"大型活动安保巡逻", "大型户外活动需无人机安保巡逻", "OTHER", "BLUE", "39.9042", "116.4074", "北京朝阳区", "AOPA", "多旋翼-轻型"},
                {"海上搜救支援", "渔船遇险需无人机协助海上搜救", "FLOOD_RESCUE", "RED", "24.4798", "118.0894", "福建厦门海域", "AOPA,CAAC", "固定翼-侦察"},
                {"电力线路巡检", "暴风后需对高压线路进行无人机巡检", "ACCIDENT_SURVEY", "YELLOW", "32.0603", "118.7969", "江苏南京江宁区", "UTC-SURVEY", "多旋翼-轻型"},
                {"森林防火巡查", "秋季森林防火期例行巡查", "FIRE_FIGHTING", "BLUE", "41.8057", "123.4315", "辽宁沈阳棋盘山", "AOPA", "固定翼-测绘"},
        };

        String[] weatherData = {
                "风速12m/s 西北风, 能见度8km, 温度28°C",
                "风速5m/s 东南风, 能见度3km(雨), 温度22°C",
                "风速3m/s 北风, 能见度15km, 温度20°C",
                "风速8m/s 东风, 能见度5km(雾), 温度25°C",
                "风速6m/s 西风, 能见度10km, 温度18°C",
                "风速4m/s 南风, 能见度6km(阴), 温度16°C",
                "风速2m/s 东风, 能见度20km, 温度26°C",
                "风速15m/s 东北风, 能见度4km(浪), 温度19°C",
                "风速7m/s 西北风, 能见度12km, 温度15°C",
                "风速3m/s 北风, 能见度18km, 温度12°C"
        };

        String[] statuses = {"COMPLETED", "IN_PROGRESS", "DISPATCHED", "PUBLISHED", "ACCEPTED", "PUBLISHED", "COMPLETED", "DISPATCHED", "PUBLISHED", "PUBLISHED"};

        for (int i = 0; i < missionData.length; i++) {
            Mission m = new Mission();
            m.setTitle(missionData[i][0]);
            m.setDescription(missionData[i][1]);
            m.setMissionType(missionData[i][2]);
            m.setUrgency(missionData[i][3]);
            m.setLatitude(Double.parseDouble(missionData[i][4]));
            m.setLongitude(Double.parseDouble(missionData[i][5]));
            m.setLocationName(missionData[i][6]);
            m.setWeatherInfo(weatherData[i]);
            m.setRequiredCertTypes(missionData[i][7]);
            m.setRequiredDroneType(missionData[i][8]);
            m.setCreatorId(admin.getId());
            m.setStatus(statuses[i]);
            m.setCreatedAt(LocalDateTime.now().minusDays(missionData.length - i));
            m.setUpdatedAt(LocalDateTime.now().minusDays(missionData.length - i));

            if ("COMPLETED".equals(statuses[i])) {
                m.setCompletedAt(LocalDateTime.now().minusDays(missionData.length - i - 1));
                if (!approvedPilots.isEmpty()) m.setAssignedPilotId(approvedPilots.get(i % approvedPilots.size()).getId());
                if (!approvedOrgs.isEmpty()) m.setAssignedOrgId(approvedOrgs.get(i % approvedOrgs.size()).getId());
            } else if ("IN_PROGRESS".equals(statuses[i]) || "ACCEPTED".equals(statuses[i])) {
                if (!approvedPilots.isEmpty()) m.setAssignedPilotId(approvedPilots.get(i % approvedPilots.size()).getId());
                if (!approvedOrgs.isEmpty()) m.setAssignedOrgId(approvedOrgs.get(i % approvedOrgs.size()).getId());
            } else if ("DISPATCHED".equals(statuses[i])) {
                int deadlineMin = "RED".equals(m.getUrgency()) ? 5 : "YELLOW".equals(m.getUrgency()) ? 15 : 30;
                m.setConfirmDeadline(LocalDateTime.now().plusMinutes(deadlineMin));
            }

            missionMapper.insert(m);
        }
    }

    private void ensurePilotDemo(SysUser admin, DemoPilotSpec spec, String reviewDecision) {
        if (hasUser(spec.username())) {
            return;
        }

        Pilot pilot = new Pilot();
        pilot.setFullName(spec.fullName());
        pilot.setPhone(spec.phone());
        pilot.setEmail(spec.email());
        pilot.setIdCard(spec.idCard());
        pilot.setRecordNo(spec.recordNo());
        pilot.setRegion(spec.region());
        pilot.setStatus(spec.status());
        pilot.setAutoResult(spec.autoResult());
        pilot.setAutoScore(spec.autoScore());
        pilot.setAutoReason("[\"seeded demo data\"]");
        pilot.setCreatedAt(spec.createdAt());
        pilot.setUpdatedAt(spec.createdAt());
        pilotMapper.insert(pilot);

        for (DemoCertSpec certSpec : spec.certificates()) {
            PilotCertificate cert = new PilotCertificate();
            cert.setPilotId(pilot.getId());
            cert.setCertType(certSpec.certType());
            cert.setCertNo(certSpec.certNo());
            cert.setValidUntil(certSpec.validUntil());
            cert.setCreatedAt(spec.createdAt());
            pilotCertificateMapper.insert(cert);
        }

        SysUser user = new SysUser();
        user.setUsername(spec.username());
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole("PILOT");
        user.setPilotId(pilot.getId());
        user.setEnabled(1);
        user.setCreatedAt(spec.createdAt());
        sysUserMapper.insert(user);

        pilot.setUserId(user.getId());
        pilotMapper.updateById(pilot);

        if (Statuses.APPROVED.equals(spec.status())) {
            ensurePilotStatusRecord(pilot.getId(), spec.createdAt());
        }

        PilotApplication application = new PilotApplication();
        application.setPilotId(pilot.getId());
        application.setApplyType("REGISTER");
        application.setStatus(spec.status());
        application.setAutoResult(spec.autoResult());
        application.setAutoScore(spec.autoScore());
        application.setAutoReason("[\"seeded demo data\"]");
        application.setCreatedAt(spec.createdAt());
        pilotApplicationMapper.insert(application);

        createAudit(
                "PILOT",
                pilot.getId(),
                "SUBMIT_AUTO",
                user.getId(),
                "seeded demo submit",
                "{\"autoResult\":\"" + spec.autoResult() + "\"}",
                spec.createdAt());

        if (reviewDecision != null) {
            createAudit(
                    "PILOT",
                    pilot.getId(),
                    "MANUAL_REVIEW",
                    admin.getId(),
                    "seeded demo review",
                    "{\"decision\":\"" + reviewDecision + "\"}",
                    spec.createdAt().plusDays(1));
        }
    }

    private void ensureOrgDemo(SysUser admin, DemoOrgSpec spec, String reviewDecision) {
        if (hasUser(spec.username())) {
            return;
        }

        Organization organization = new Organization();
        organization.setName(spec.name());
        organization.setOrgType(spec.orgType());
        organization.setCreditCode(spec.creditCode());
        organization.setContactName(spec.contactName());
        organization.setPhone(spec.phone());
        organization.setAddress(spec.address());
        organization.setStatus(spec.status());
        organization.setAutoResult(spec.autoResult());
        organization.setAutoScore(spec.autoScore());
        organization.setAutoReason("[\"seeded demo data\"]");
        organization.setCreatedAt(spec.createdAt());
        organization.setUpdatedAt(spec.createdAt());
        organizationMapper.insert(organization);

        SysUser user = new SysUser();
        user.setUsername(spec.username());
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole("ORG");
        user.setOrgId(organization.getId());
        user.setEnabled(1);
        user.setCreatedAt(spec.createdAt());
        sysUserMapper.insert(user);

        organization.setUserId(user.getId());
        organizationMapper.updateById(organization);

        if (Statuses.APPROVED.equals(spec.status())) {
            ensureOrgStatusRecord(organization.getId(), spec.createdAt());
        }

        OrgApplication application = new OrgApplication();
        application.setOrgId(organization.getId());
        application.setApplyType("REGISTER");
        application.setStatus(spec.status());
        application.setAutoResult(spec.autoResult());
        application.setAutoScore(spec.autoScore());
        application.setAutoReason("[\"seeded demo data\"]");
        application.setCreatedAt(spec.createdAt());
        orgApplicationMapper.insert(application);

        createAudit(
                "ORG",
                organization.getId(),
                "SUBMIT_AUTO",
                user.getId(),
                "seeded demo submit",
                "{\"autoResult\":\"" + spec.autoResult() + "\"}",
                spec.createdAt());

        if (reviewDecision != null) {
            createAudit(
                    "ORG",
                    organization.getId(),
                    "MANUAL_REVIEW",
                    admin.getId(),
                    "seeded demo review",
                    "{\"decision\":\"" + reviewDecision + "\"}",
                    spec.createdAt().plusDays(1));
        }
    }

    private boolean hasUser(String username) {
        return sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0;
    }

    private void createAudit(
            String entityType,
            Long entityId,
            String action,
            Long operatorUserId,
            String comment,
            String detailJson,
            LocalDateTime createdAt) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setOperatorUserId(operatorUserId);
        log.setCommentText(comment);
        log.setDetailJson(detailJson);
        log.setCreatedAt(createdAt);
        auditLogMapper.insert(log);
    }

    private record DemoCertSpec(String certType, String certNo, LocalDate validUntil) {
    }

    private record DemoPilotSpec(
            String username,
            String fullName,
            String phone,
            String email,
            String idCard,
            String recordNo,
            String region,
            String status,
            String autoResult,
            Integer autoScore,
            LocalDateTime createdAt,
            List<DemoCertSpec> certificates) {
    }

    private record DemoOrgSpec(
            String username,
            String name,
            String orgType,
            String creditCode,
            String contactName,
            String phone,
            String address,
            String status,
            String autoResult,
            Integer autoScore,
            LocalDateTime createdAt) {
    }

    private void ensurePilotStatusRecord(Long pilotId, LocalDateTime createdAt) {
        Long count = pilotStatusMapper.selectCount(
                new LambdaQueryWrapper<PilotStatus>().eq(PilotStatus::getPilotId, pilotId));
        if (count == null || count == 0) {
            PilotStatus ps = new PilotStatus();
            ps.setPilotId(pilotId);
            ps.setAvailability("IDLE");
            ps.setMissionCount(0);
            ps.setCreatedAt(createdAt);
            ps.setUpdatedAt(createdAt);
            pilotStatusMapper.insert(ps);
        }
    }

    private void ensureOrgStatusRecord(Long orgId, LocalDateTime createdAt) {
        Long count = orgStatusMapper.selectCount(
                new LambdaQueryWrapper<OrgStatus>().eq(OrgStatus::getOrgId, orgId));
        if (count == null || count == 0) {
            OrgStatus os = new OrgStatus();
            os.setOrgId(orgId);
            os.setAvailability("IDLE");
            os.setMissionCount(0);
            os.setCreatedAt(createdAt);
            os.setUpdatedAt(createdAt);
            orgStatusMapper.insert(os);
        }
    }
}
