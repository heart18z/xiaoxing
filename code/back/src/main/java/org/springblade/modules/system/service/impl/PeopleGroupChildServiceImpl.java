package org.springblade.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.modules.system.entity.PeopleGroupChild;
import org.springblade.modules.system.mapper.PeopleGroupChildMapper;
import org.springblade.modules.system.service.IPeopleGroupChildService;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("all")
@Slf4j
@AllArgsConstructor
public class PeopleGroupChildServiceImpl extends ServiceImpl<PeopleGroupChildMapper, PeopleGroupChild> implements IPeopleGroupChildService {
}
