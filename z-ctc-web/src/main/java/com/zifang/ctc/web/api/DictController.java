package com.zifang.ctc.web.api;

import com.zifang.ctc.core.domain.entity.DictCategoryDO;
import com.zifang.ctc.core.domain.entity.DictEntryDO;
import com.zifang.ctc.core.service.DictService;
import com.zifang.ctc.core.service.MembershipService;
import com.zifang.ctc.core.service.PlatformAdminService;
import com.zifang.ctc.core.vo.DictCategoryVO;
import com.zifang.ctc.core.vo.DictEntryVO;
import com.zifang.util.core.meta.Result;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 灵活字典控制器：分类与条目的 REST 面。
 *
 * <p>读权限：平台管理员或租户成员；写权限：平台管理员或租户管理员。</p>
 *
 * @author zifang
 * @since 1.0.8
 */
@RestController
@RequestMapping("/api/ctc/ac/dict")
public class DictController {

    private final DictService dictService;
    private final MembershipService membershipService;
    private final PlatformAdminService platformAdminService;

    public DictController(DictService dictService,
                          MembershipService membershipService,
                          PlatformAdminService platformAdminService) {
        this.dictService = dictService;
        this.membershipService = membershipService;
        this.platformAdminService = platformAdminService;
    }

    /**
     * 可见分类列表。
     *
     * @param tenantCode 租户编码
     * @param keyword    关键字过滤（分类名 / 编码）
     * @return 分类列表
     */
    @GetMapping("/categories/list")
    @BizApi(summary = "可见分类列表", description = "可见分类列表。", tags = { "z-ctc" })
    public Result<List<DictCategoryVO>> listCategories(@RequestParam(required = false) @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码", position = BizParam.Position.QUERY) String tenantCode,
                                                       @RequestParam(required = false)
                                                       @BizParam(name = "keyword", type = BizField.FieldType.STRING, description = "关键字过滤（分类名 / 编码）", position = BizParam.Position.QUERY) String keyword) {
        List<DictCategoryVO> result = new ArrayList<>();
        for (DictCategoryDO category : dictService.listVisibleCategories(tenantCode, null)) {
            if (keyword != null && !keyword.isEmpty()
                    && (category.getCatCode() == null || !category.getCatCode().contains(keyword))
                    && (category.getCatName() == null || !category.getCatName().contains(keyword))) {
                continue;
            }
            result.add(DictCategoryVO.from(category));
        }
        return Result.success(result);
    }

    /**
     * 按主键取分类。
     *
     * @param id 分类主键
     * @return 分类详情
     */
    @GetMapping("/categories")
    @BizApi(summary = "按主键取分类", description = "按主键取分类。", tags = { "z-ctc" })
    public Result<DictCategoryVO> getCategory(@PathVariable("id") @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "分类主键", position = BizParam.Position.PATH) Long id) {
        DictCategoryDO category = dictService.findCategoryById(id);
        if (category == null) return Result.<DictCategoryVO>fail("分类不存在").code(404);
        return Result.success(DictCategoryVO.from(category));
    }

    /**
     * 新建分类。
     *
     * @param category  分类
     * @param operator  操作人
     * @return 新分类主键
     */
    @PostMapping("/categories")
    @BizApi(summary = "新建分类", description = "新建分类。", tags = { "z-ctc" })
    public Result<Long> createCategory(@RequestBody @BizParam(name = "category", type = BizField.FieldType.STRING, description = "分类", position = BizParam.Position.BODY) DictCategoryDO category,
                                        @RequestHeader(value = "X-User-Id", required = false)
                                        @BizParam(name = "X-User-Id", type = BizField.FieldType.STRING, description = "操作人", position = BizParam.Position.HEADER) String operator) {
        Result<Long> forbidden = forbidden();
        if (forbidden != null) return forbidden;
        return Result.success(dictService.createCategory(category, operator));
    }

    /**
     * 改分类。
     *
     * @param id        分类主键
     * @param patch     补丁
     * @param operator  操作人
     * @return 空
     */
    @PutMapping("/categories")
    @BizApi(summary = "改分类", description = "改分类。", tags = { "z-ctc" })
    public Result<Void> updateCategory(@PathVariable("id") @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "分类主键", position = BizParam.Position.PATH) Long id,
                                       @RequestBody @BizParam(name = "patch", type = BizField.FieldType.STRING, description = "补丁", position = BizParam.Position.BODY) DictCategoryDO patch,
                                       @RequestHeader(value = "X-User-Id", required = false)
                                       @BizParam(name = "X-User-Id", type = BizField.FieldType.STRING, description = "操作人", position = BizParam.Position.HEADER) String operator) {
        Result<Void> forbidden = forbidden();
        if (forbidden != null) return forbidden;
        dictService.updateCategory(id, patch, operator);
        return Result.success();
    }

    /**
     * 删分类。
     *
     * @param id 分类主键
     * @return 空
     */
    @DeleteMapping("/categories")
    @BizApi(summary = "删分类", description = "删分类。", tags = { "z-ctc" })
    public Result<Void> deleteCategory(@PathVariable("id") @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "分类主键", position = BizParam.Position.PATH) Long id) {
        Result<Void> forbidden = forbidden();
        if (forbidden != null) return forbidden;
        dictService.deleteCategory(id);
        return Result.success();
    }

    /**
     * 列条目。
     *
     * @param category   分类编码
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @return 条目列表
     */
    @GetMapping("/entries/list")
    @BizApi(summary = "列条目", description = "列条目。", tags = { "z-ctc" })
    public Result<List<DictEntryVO>> listEntries(@RequestParam("category") @BizParam(name = "category", type = BizField.FieldType.STRING, description = "分类编码", position = BizParam.Position.QUERY) String category,
                                                  @RequestParam(required = false)
                                                  @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码", position = BizParam.Position.QUERY) String tenantCode,
                                                  @RequestParam(required = false)
                                                  @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码", position = BizParam.Position.QUERY) String domainCode) {
        List<DictEntryVO> result = new ArrayList<>();
        for (DictEntryDO entry : dictService.listEntries(category, tenantCode, domainCode)) {
            result.add(DictEntryVO.from(entry));
        }
        return Result.success(result);
    }

    /**
     * 按主键取条目。
     *
     * @param id 条目主键
     * @return 条目详情
     */
    @GetMapping("/entries")
    @BizApi(summary = "按主键取条目", description = "按主键取条目。", tags = { "z-ctc" })
    public Result<DictEntryVO> getEntry(@PathVariable("id") @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "条目主键", position = BizParam.Position.PATH) Long id) {
        DictEntryDO entry = dictService.findEntryById(id);
        if (entry == null) return Result.<DictEntryVO>fail("条目不存在").code(404);
        return Result.success(DictEntryVO.from(entry));
    }

    /**
     * 新建条目。
     *
     * @param entry    条目
     * @param operator 操作人
     * @return 新条目主键
     */
    @PostMapping("/entries")
    @BizApi(summary = "新建条目", description = "新建条目。", tags = { "z-ctc" })
    public Result<Long> createEntry(@RequestBody @BizParam(name = "entry", type = BizField.FieldType.STRING, description = "条目", position = BizParam.Position.BODY) DictEntryDO entry,
                                     @RequestHeader(value = "X-User-Id", required = false)
                                     @BizParam(name = "X-User-Id", type = BizField.FieldType.STRING, description = "操作人", position = BizParam.Position.HEADER) String operator) {
        Result<Long> forbidden = forbidden();
        if (forbidden != null) return forbidden;
        return Result.success(dictService.createEntry(entry, operator));
    }

    /**
     * 改条目。
     *
     * @param id       条目主键
     * @param patch    补丁
     * @param operator 操作人
     * @return 空
     */
    @PutMapping("/entries")
    @BizApi(summary = "改条目", description = "改条目。", tags = { "z-ctc" })
    public Result<Void> updateEntry(@PathVariable("id") @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "条目主键", position = BizParam.Position.PATH) Long id,
                                     @RequestBody @BizParam(name = "patch", type = BizField.FieldType.STRING, description = "补丁", position = BizParam.Position.BODY) DictEntryDO patch,
                                     @RequestHeader(value = "X-User-Id", required = false)
                                     @BizParam(name = "X-User-Id", type = BizField.FieldType.STRING, description = "操作人", position = BizParam.Position.HEADER) String operator) {
        Result<Void> forbidden = forbidden();
        if (forbidden != null) return forbidden;
        dictService.updateEntry(id, patch, operator);
        return Result.success();
    }

    /**
     * 删条目。
     *
     * @param id 条目主键
     * @return 空
     */
    @DeleteMapping("/entries")
    @BizApi(summary = "删条目", description = "删条目。", tags = { "z-ctc" })
    public Result<Void> deleteEntry(@PathVariable("id") @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "条目主键", position = BizParam.Position.PATH) Long id) {
        Result<Void> forbidden = forbidden();
        if (forbidden != null) return forbidden;
        dictService.deleteEntry(id);
        return Result.success();
    }

    /**
     * 按分类 + 键查条目。
     *
     * @param category   分类编码
     * @param key        条目键
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @return 条目
     */
    @GetMapping("/lookup")
    @BizApi(summary = "按分类 + 键查条目", description = "按分类 + 键查条目。", tags = { "z-ctc" })
    public Result<DictEntryVO> lookup(@RequestParam("category") @BizParam(name = "category", type = BizField.FieldType.STRING, description = "分类编码", position = BizParam.Position.QUERY) String category,
                                       @RequestParam("key")
                                       @BizParam(name = "key", type = BizField.FieldType.STRING, description = "条目键", position = BizParam.Position.QUERY) String key,
                                       @RequestParam(required = false)
                                       @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码", position = BizParam.Position.QUERY) String tenantCode,
                                       @RequestParam(required = false)
                                       @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码", position = BizParam.Position.QUERY) String domainCode) {
        DictEntryDO entry = dictService.lookup(category, key, tenantCode, domainCode);
        if (entry == null) return Result.<DictEntryVO>fail("条目不存在或未生效").code(404);
        return Result.success(DictEntryVO.from(entry));
    }

    /**
     * 批量查多个分类的条目。
     *
     * @param categories 分类编码列表
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @return 分类编码 → 条目列表
     */
    @GetMapping("/resolve")
    @BizApi(summary = "批量查多个分类的条目", description = "批量查多个分类的条目。", tags = { "z-ctc" })
    public Result<Map<String, List<DictEntryVO>>> resolve(@RequestParam("categories") @BizParam(name = "categories", type = BizField.FieldType.JSON, description = "分类编码列表", position = BizParam.Position.QUERY) List<String> categories,
                                                           @RequestParam(required = false)
                                                           @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码", position = BizParam.Position.QUERY) String tenantCode,
                                                           @RequestParam(required = false)
                                                           @BizParam(name = "domainCode", type = BizField.FieldType.STRING, description = "域编码", position = BizParam.Position.QUERY) String domainCode) {
        Map<String, List<DictEntryVO>> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, List<DictEntryDO>> e : dictService.resolve(categories, tenantCode, domainCode).entrySet()) {
            List<DictEntryVO> vos = new ArrayList<>();
            for (DictEntryDO entry : e.getValue()) vos.add(DictEntryVO.from(entry));
            result.put(e.getKey(), vos);
        }
        return Result.success(result);
    }

    private boolean isPlatformAdmin() {
        return platformAdminService != null && platformAdminService.isPlatformAdmin(null);
    }

    private boolean canRead(String tenantCode) {
        return isPlatformAdmin();
    }

    private boolean canManage(String tenantCode) {
        return isPlatformAdmin();
    }

    private <T> Result<T> forbidden() {
        if (canManage(null)) return null;
        return Result.<T>fail("无权操作字典").code(403);
    }
}



