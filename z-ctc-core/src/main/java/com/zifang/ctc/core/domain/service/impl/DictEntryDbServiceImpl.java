package com.zifang.ctc.core.domain.service.impl;

import com.zifang.ctc.core.domain.entity.DictEntryDO;
import com.zifang.ctc.core.domain.mapper.DictEntryMapper;
import com.zifang.ctc.core.domain.service.DictEntryDbService;
import com.zifang.z.meta.api.annotation.BizMethod;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典条目数据库服务实现。
 *
 * @author zifang
 * @since 1.0.8
 */
@Service
public class DictEntryDbServiceImpl implements DictEntryDbService {

    private final DictEntryMapper dictEntryMapper;

    public DictEntryDbServiceImpl(DictEntryMapper dictEntryMapper) {
        this.dictEntryMapper = dictEntryMapper;
    }

    @Override
    @BizMethod(name = "selectByCategory", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public List<DictEntryDO> selectByCategory(String category) {
        return dictEntryMapper.selectByCategory(category);
    }

    @Override
    @BizMethod(name = "selectVisible", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public List<DictEntryDO> selectVisible(String tenantCode) {
        return dictEntryMapper.selectVisible(tenantCode);
    }

    @Override
    @BizMethod(name = "selectById", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public DictEntryDO selectById(Long id) {
        return dictEntryMapper.selectById(id);
    }

    @Override
    @BizMethod(name = "selectByScope", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public DictEntryDO selectByScope(String tenantCode, String domainCode, String category, String dictKey) {
        return dictEntryMapper.selectByScope(tenantCode, domainCode, category, dictKey);
    }

    @Override
    @BizMethod(name = "countByCategory", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public long countByCategory(String category) {
        return dictEntryMapper.countByCategory(category);
    }

    @Override
    @BizMethod(name = "insert", module = "z-ctc", description = "", operation = BizMethod.Operation.CREATE)
    public int insert(DictEntryDO entity) {
        return dictEntryMapper.insert(entity);
    }

    @Override
    @BizMethod(name = "updateById", module = "z-ctc", description = "", operation = BizMethod.Operation.UPDATE)
    public int updateById(DictEntryDO entity) {
        return dictEntryMapper.updateById(entity);
    }

    @Override
    @BizMethod(name = "deleteById", module = "z-ctc", description = "", operation = BizMethod.Operation.DELETE)
    public int deleteById(Long id) {
        return dictEntryMapper.deleteById(id);
    }
}



