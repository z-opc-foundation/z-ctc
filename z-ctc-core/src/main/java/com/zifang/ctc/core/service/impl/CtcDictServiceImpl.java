package com.zifang.ctc.core.service.impl;

import com.zifang.ctc.core.domain.entity.DictCategoryDO;
import com.zifang.ctc.core.domain.entity.DictEntryDO;
import com.zifang.ctc.core.domain.service.DictCategoryDbService;
import com.zifang.ctc.core.domain.service.DictEntryDbService;
import com.zifang.ctc.core.service.DictService;
import com.zifang.z.meta.api.annotation.BizMethod;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 灵活字典服务实现。
 *
 * <p>三段作用域：分类的 scope 决定 tenantCode / domainCode 怎么填；
 * 条目的作用域继承分类（除非显式指定）；条目是否生效看 status 与有效期窗口。</p>
 *
 * @author zifang
 * @since 1.0.8
 */
@Service
public class CtcDictServiceImpl implements DictService {

    /** 条目值类型白名单。 */
    private static final Set<String> VALUE_TYPES = Set.of("string", "number", "boolean", "json");

    /** 作用域白名单。 */
    private static final Set<String> SCOPES = Set.of("GLOBAL", "TENANT", "DOMAIN");

    /** 条目形状白名单。 */
    private static final Set<String> ENTRY_SHAPES = Set.of("flat", "tree");

    private final DictCategoryDbService categoryDbService;
    private final DictEntryDbService entryDbService;

    public CtcDictServiceImpl(DictCategoryDbService categoryDbService, DictEntryDbService entryDbService) {
        this.categoryDbService = categoryDbService;
        this.entryDbService = entryDbService;
    }

    @Override
    @BizMethod(name = "listVisibleCategories", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public List<DictCategoryDO> listVisibleCategories(String tenantCode, String domainCode) {
        List<DictCategoryDO> result = new ArrayList<>(categoryDbService.selectGlobal());
        if (tenantCode != null && !tenantCode.isEmpty()) {
            result.addAll(categoryDbService.selectByTenant(tenantCode));
        }
        return result;
    }

    @Override
    @BizMethod(name = "findCategoryById", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public DictCategoryDO findCategoryById(Long id) {
        return id == null ? null : categoryDbService.selectById(id);
    }

    @Override
    @BizMethod(name = "createCategory", module = "z-ctc", description = "", operation = BizMethod.Operation.CREATE)
    public Long createCategory(DictCategoryDO category, String operator) {
        normalizeCategoryScope(category);
        validateCategory(category);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        category.setCreatedBy(operator);
        category.setUpdatedBy(operator);
        if (category.getStatus() == null) category.setStatus(1);
        if (category.getSortOrder() == null) category.setSortOrder(0);
        if (category.getBuiltin() == null) category.setBuiltin(0);
        if (category.getValueType() == null) category.setValueType("string");
        if (category.getEntryShape() == null) category.setEntryShape("flat");
        categoryDbService.insert(category);
        return category.getId();
    }

    @Override
    @BizMethod(name = "updateCategory", module = "z-ctc", description = "", operation = BizMethod.Operation.UPDATE)
    public boolean updateCategory(Long id, DictCategoryDO patch, String operator) {
        DictCategoryDO existing = categoryDbService.selectById(id);
        if (existing == null) return false;
        if (isBuiltin(existing) && patch.getCatCode() != null && changed(existing.getCatCode(), patch.getCatCode())) {
            throw new IllegalStateException("系统内置分类不允许改编码");
        }
        applyPatch(existing, patch);
        normalizeCategoryScope(existing);
        validateCategory(existing);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(operator);
        categoryDbService.updateById(existing);
        return true;
    }

    @Override
    @BizMethod(name = "deleteCategory", module = "z-ctc", description = "", operation = BizMethod.Operation.DELETE)
    public boolean deleteCategory(Long id) {
        DictCategoryDO existing = categoryDbService.selectById(id);
        if (existing == null) return false;
        if (isBuiltin(existing)) {
            throw new IllegalStateException("系统内置分类不允许删除");
        }
        if (categoryDbService.countChildren(existing.getCatCode()) > 0) {
            throw new IllegalStateException("分类下还有子分类，不能删");
        }
        if (entryDbService.countByCategory(existing.getCatCode()) > 0) {
            throw new IllegalStateException("分类下还有条目，不能删");
        }
        categoryDbService.deleteById(id);
        return true;
    }

    @Override
    @BizMethod(name = "listEntries", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public List<DictEntryDO> listEntries(String category, String tenantCode, String domainCode) {
        DictCategoryDO definition = resolveCategoryDefinition(category, tenantCode);
        List<DictEntryDO> entries = entryDbService.selectByCategory(category);
        List<DictEntryDO> visible = new ArrayList<>();
        for (DictEntryDO entry : entries) {
            if (effective(entry) != null) visible.add(entry);
        }
        if (definition != null && "tree".equals(definition.getEntryShape())) {
            return visible;
        }
        return visible;
    }

    @Override
    @BizMethod(name = "findEntryById", module = "z-ctc", description = "", operation = BizMethod.Operation.QUERY)
    public DictEntryDO findEntryById(Long id) {
        return id == null ? null : entryDbService.selectById(id);
    }

    @Override
    @BizMethod(name = "createEntry", module = "z-ctc", description = "", operation = BizMethod.Operation.CREATE)
    public Long createEntry(DictEntryDO entry, String operator) {
        DictCategoryDO category = resolveCategoryDefinition(entry.getCategory(), entry.getTenantCode());
        normalizeEntryScope(entry, category);
        validateParentKey(entry, category);
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        entry.setCreatedBy(operator);
        entry.setUpdatedBy(operator);
        if (entry.getStatus() == null) entry.setStatus(1);
        if (entry.getSortOrder() == null) entry.setSortOrder(0);
        entryDbService.insert(entry);
        return entry.getId();
    }

    @Override
    @BizMethod(name = "updateEntry", module = "z-ctc", description = "", operation = BizMethod.Operation.UPDATE)
    public boolean updateEntry(Long id, DictEntryDO patch, String operator) {
        DictEntryDO existing = entryDbService.selectById(id);
        if (existing == null) return false;
        applyEntryPatch(existing, patch);
        DictCategoryDO category = resolveCategoryDefinition(existing.getCategory(), existing.getTenantCode());
        validateParentKey(existing, category);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(operator);
        entryDbService.updateById(existing);
        return true;
    }

    @Override
    @BizMethod(name = "deleteEntry", module = "z-ctc", description = "", operation = BizMethod.Operation.DELETE)
    public boolean deleteEntry(Long id) {
        DictEntryDO existing = entryDbService.selectById(id);
        if (existing == null) return false;
        DictCategoryDO category = resolveCategoryDefinition(existing.getCategory(), existing.getTenantCode());
        if (category != null && "tree".equals(category.getEntryShape())
                && hasTreeChildren(existing.getCategory(), existing.getDictKey())) {
            throw new IllegalStateException("条目下还有子条目，不能删");
        }
        entryDbService.deleteById(id);
        return true;
    }

    @Override
    @BizMethod(name = "lookup", module = "z-ctc", description = "", operation = BizMethod.Operation.ACTION)
    public DictEntryDO lookup(String category, String key, String tenantCode, String domainCode) {
        DictEntryDO entry = entryDbService.selectByScope(tenantCode, domainCode, category, key);
        return effective(entry);
    }

    @Override
    @BizMethod(name = "resolve", module = "z-ctc", description = "", operation = BizMethod.Operation.ACTION)
    public Map<String, List<DictEntryDO>> resolve(List<String> categories, String tenantCode, String domainCode) {
        Map<String, List<DictEntryDO>> result = new LinkedHashMap<>();
        if (categories == null) return result;
        for (String category : categories) {
            result.computeIfAbsent(category, k -> new ArrayList<>());
            for (DictEntryDO entry : listEntries(category, tenantCode, domainCode)) {
                result.get(category).add(entry);
            }
        }
        return result;
    }

    /** 分类作用域归一：GLOBAL 租户域都空；TENANT 只有租户；DOMAIN 租户 + 域。 */
    private void normalizeCategoryScope(DictCategoryDO category) {
        String scope = category.getScope() == null ? "TENANT" : category.getScope().toUpperCase();
        category.setScope(scope);
        if ("GLOBAL".equals(scope)) {
            category.setTenantCode("");
            category.setDomainCode("");
        } else if ("TENANT".equals(scope)) {
            category.setDomainCode("");
        }
    }

    private void validateCategory(DictCategoryDO category) {
        requireNonEmpty(category.getCatCode(), "分类编码");
        requireNonEmpty(category.getCatName(), "分类名称");
        if (!SCOPES.contains(category.getScope())) {
            throw new IllegalArgumentException("非法作用域：" + category.getScope());
        }
        if (category.getValueType() != null && !VALUE_TYPES.contains(category.getValueType())) {
            throw new IllegalArgumentException("非法条目值类型：" + category.getValueType());
        }
        if (category.getEntryShape() != null && !ENTRY_SHAPES.contains(category.getEntryShape())) {
            throw new IllegalArgumentException("非法条目形状：" + category.getEntryShape());
        }
        if (category.getParentCode() != null && !category.getParentCode().isEmpty()
                && !parentCategoryExists(category.getParentCode())) {
            throw new IllegalArgumentException("父分类不存在：" + category.getParentCode());
        }
    }

    private boolean parentCategoryExists(String catCode) {
        DictCategoryDO parent = categoryDbService.selectByScope(null, null, catCode);
        return parent != null;
    }

    private DictCategoryDO resolveCategoryDefinition(String catCode, String tenantCode) {
        if (catCode == null || catCode.isEmpty()) return null;
        DictCategoryDO definition = categoryDbService.selectByScope(tenantCode, null, catCode);
        return definition != null ? definition : categoryDbService.selectByScope(null, null, catCode);
    }

    /** 条目作用域继承分类。 */
    private void normalizeEntryScope(DictEntryDO entry, DictCategoryDO category) {
        if (category == null) return;
        if (entry.getTenantCode() == null || entry.getTenantCode().isEmpty()) {
            entry.setTenantCode(category.getTenantCode());
        }
        if (entry.getDomainCode() == null || entry.getDomainCode().isEmpty()) {
            entry.setDomainCode(category.getDomainCode());
        }
    }

    private void validateParentKey(DictEntryDO entry, DictCategoryDO category) {
        if (category == null || !"tree".equals(category.getEntryShape())) return;
        String parentKey = entry.getParentKey();
        if (parentKey == null || parentKey.isEmpty()) return;
        if (parentKey.equals(entry.getDictKey())) {
            throw new IllegalArgumentException("父条目不能是自己");
        }
        DictEntryDO parent = entryDbService.selectByScope(entry.getTenantCode(), entry.getDomainCode(),
                entry.getCategory(), parentKey);
        if (parent == null) {
            throw new IllegalArgumentException("父条目不存在：" + parentKey);
        }
    }

    private boolean hasTreeChildren(String category, String dictKey) {
        List<DictEntryDO> entries = entryDbService.selectByCategory(category);
        for (DictEntryDO entry : entries) {
            if (dictKey.equals(entry.getParentKey())) return true;
        }
        return false;
    }

    /** 当前生效的条目：状态启用且在有效期内。 */
    private DictEntryDO effective(DictEntryDO entry) {
        if (entry == null) return null;
        if (entry.getStatus() != null && entry.getStatus() == 0) return null;
        LocalDateTime now = LocalDateTime.now();
        if (entry.getEffectiveFrom() != null && now.isBefore(entry.getEffectiveFrom())) return null;
        if (entry.getEffectiveTo() != null && now.isAfter(entry.getEffectiveTo())) return null;
        return entry;
    }

    /** 打补丁：只覆盖非 null 且非空的字段。 */
    private void applyPatch(DictCategoryDO target, DictCategoryDO patch) {
        if (patch.getCatCode() != null && !patch.getCatCode().isEmpty()) target.setCatCode(patch.getCatCode());
        if (patch.getCatName() != null && !patch.getCatName().isEmpty()) target.setCatName(patch.getCatName());
        if (patch.getParentCode() != null) target.setParentCode(patch.getParentCode());
        if (patch.getScope() != null && !patch.getScope().isEmpty()) target.setScope(patch.getScope());
        if (patch.getValueType() != null && !patch.getValueType().isEmpty()) target.setValueType(patch.getValueType());
        if (patch.getEntryShape() != null && !patch.getEntryShape().isEmpty()) target.setEntryShape(patch.getEntryShape());
        if (patch.getExtSchema() != null) target.setExtSchema(patch.getExtSchema());
        if (patch.getBuiltin() != null) target.setBuiltin(patch.getBuiltin());
        if (patch.getStatus() != null) target.setStatus(patch.getStatus());
        if (patch.getSortOrder() != null) target.setSortOrder(patch.getSortOrder());
        if (patch.getDescription() != null) target.setDescription(patch.getDescription());
    }

    private void applyEntryPatch(DictEntryDO target, DictEntryDO patch) {
        if (patch.getCategory() != null && !patch.getCategory().isEmpty()) target.setCategory(patch.getCategory());
        if (patch.getDictKey() != null && !patch.getDictKey().isEmpty()) target.setDictKey(patch.getDictKey());
        if (patch.getDictValue() != null) target.setDictValue(patch.getDictValue());
        if (patch.getDataValue() != null) target.setDataValue(patch.getDataValue());
        if (patch.getParentKey() != null) target.setParentKey(patch.getParentKey());
        if (patch.getExtProps() != null) target.setExtProps(patch.getExtProps());
        if (patch.getTags() != null) target.setTags(patch.getTags());
        if (patch.getIcon() != null) target.setIcon(patch.getIcon());
        if (patch.getColor() != null) target.setColor(patch.getColor());
        if (patch.getRefCategory() != null) target.setRefCategory(patch.getRefCategory());
        if (patch.getRefKey() != null) target.setRefKey(patch.getRefKey());
        if (patch.getEffectiveFrom() != null) target.setEffectiveFrom(patch.getEffectiveFrom());
        if (patch.getEffectiveTo() != null) target.setEffectiveTo(patch.getEffectiveTo());
        if (patch.getStatus() != null) target.setStatus(patch.getStatus());
        if (patch.getSortOrder() != null) target.setSortOrder(patch.getSortOrder());
        if (patch.getDescription() != null) target.setDescription(patch.getDescription());
    }

    private boolean isBuiltin(DictCategoryDO category) {
        return category != null && category.getBuiltin() != null && category.getBuiltin() == 1;
    }

    private boolean changed(String oldValue, String newValue) {
        return newValue != null && !newValue.isEmpty() && !Objects.equals(oldValue, newValue);
    }

    private boolean contains(String csv, String value) {
        if (csv == null || csv.isEmpty() || value == null) return false;
        for (String part : csv.split(",")) {
            if (value.equals(part.trim())) return true;
        }
        return false;
    }

    private void requireNonEmpty(String value, String field) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(field + "不能为空");
        }
    }
}



