package com.zifang.ctc.core.domain.service.impl;

import com.zifang.ctc.core.domain.entity.DictCategoryDO;
import com.zifang.ctc.core.domain.mapper.DictCategoryMapper;
import com.zifang.ctc.core.domain.service.DictCategoryDbService;
import com.zifang.z.meta.api.annotation.BizMethod;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典分类数据库服务实现。
 *
 * @author zifang
 * @since 1.0.8
 */
@Service
public class DictCategoryDbServiceImpl implements DictCategoryDbService {

    private final DictCategoryMapper dictCategoryMapper;

    public DictCategoryDbServiceImpl(DictCategoryMapper dictCategoryMapper) {
        this.dictCategoryMapper = dictCategoryMapper;
    }

    @Override
    @BizMethod(name = "selectGlobal", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public List<DictCategoryDO> selectGlobal() {
        return dictCategoryMapper.selectGlobal();
    }

    @Override
    @BizMethod(name = "selectByTenant", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public List<DictCategoryDO> selectByTenant(String tenantCode) {
        return dictCategoryMapper.selectByTenant(tenantCode);
    }

    @Override
    @BizMethod(name = "selectAll", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public List<DictCategoryDO> selectAll() {
        return dictCategoryMapper.selectAll();
    }

    @Override
    @BizMethod(name = "selectById", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public DictCategoryDO selectById(Long id) {
        return dictCategoryMapper.selectById(id);
    }

    @Override
    @BizMethod(name = "selectByScope", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public DictCategoryDO selectByScope(String tenantCode, String domainCode, String catCode) {
        return dictCategoryMapper.selectByScope(tenantCode, domainCode, catCode);
    }

    @Override
    @BizMethod(name = "countChildren", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public long countChildren(String catCode) {
        return dictCategoryMapper.countChildren(catCode);
    }

    @Override
    @BizMethod(name = "insert", module = "z-ctc", description = "", operation = BizMethod.Operation.CREATE)
    public int insert(DictCategoryDO entity) {
        return dictCategoryMapper.insert(entity);
    }

    @Override
    @BizMethod(name = "updateById", module = "z-ctc", description = "", operation = BizMethod.Operation.UPDATE)
    public int updateById(DictCategoryDO entity) {
        return dictCategoryMapper.updateById(entity);
    }

    @Override
    @BizMethod(name = "deleteById", module = "z-ctc", description = "", operation = BizMethod.Operation.DELETE)
    public int deleteById(Long id) {
        return dictCategoryMapper.deleteById(id);
    }
}



