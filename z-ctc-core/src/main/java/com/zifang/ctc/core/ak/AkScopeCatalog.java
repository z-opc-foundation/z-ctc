package com.zifang.ctc.core.ak;

import com.zifang.ctc.core.ak.model.AkScope;
import com.zifang.ctc.core.ak.model.AkScopeDomain;
import com.zifang.ctc.core.ak.model.AkScopeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 范围目录 SPI：宿主可注入实际资源树；默认仅开放受控域/动作和全部资源根。 */
public interface AkScopeCatalog {
    List<AkScopeDomain> tree();
    void validate(AkScope scope);
}
