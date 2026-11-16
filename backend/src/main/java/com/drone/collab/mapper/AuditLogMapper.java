package com.drone.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.collab.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
