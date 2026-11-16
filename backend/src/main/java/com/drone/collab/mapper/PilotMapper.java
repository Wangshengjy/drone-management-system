package com.drone.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.collab.entity.Pilot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PilotMapper extends BaseMapper<Pilot> {
}
