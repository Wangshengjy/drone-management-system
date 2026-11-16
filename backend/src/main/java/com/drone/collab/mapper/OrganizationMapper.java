package com.drone.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drone.collab.entity.Organization;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {
}
