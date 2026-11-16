package com.drone.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.collab.entity.Mission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MissionMapper extends BaseMapper<Mission> {
}
