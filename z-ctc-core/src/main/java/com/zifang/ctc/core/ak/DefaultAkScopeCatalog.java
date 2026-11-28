package com.zifang.ctc.core.ak;

import com.zifang.ctc.core.ak.model.AkScope;
import com.zifang.ctc.core.ak.model.AkScopeDomain;
import com.zifang.ctc.core.ak.model.AkScopeNode;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 保守默认目录：未知 domain/action 一律拒绝，不把空范围解释为不限制。 */
@Component
public class DefaultAkScopeCatalog implements AkScopeCatalog {
    private static final Map<String, List<String>> ACTIONS = new LinkedHashMap<>();

    static {
        ACTIONS.put("KB", Arrays.asList("READ", "WRITE", "CALL"));
        ACTIONS.put("ONTOLOGY", Arrays.asList("READ", "MODEL", "CALL"));
        ACTIONS.put("SYSTEM", Arrays.asList("VERIFY"));
    }

    @Override
    public List<AkScopeDomain> tree() {
        List<AkScopeDomain> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : ACTIONS.entrySet()) {
            AkScopeDomain domain = new AkScopeDomain();
            domain.setDomain(entry.getKey());
            domain.setDisplayName(entry.getKey());
            for (String action : entry.getValue()) {
                AkScopeNode node = new AkScopeNode();
                node.setName(action + " / 全部资源");
                node.setScope(new AkScope(entry.getKey(), action, "*", null));
                domain.getNodes().add(node);
            }
            result.add(domain);
        }
        return result;
    }

    @Override
    public void validate(AkScope source) {
        AkScope scope = AkCallPayloadSanitizer.normalize(source);
        List<String> actions = ACTIONS.get(scope.getDomain());
        if (actions == null || !actions.contains(scope.getAction())) {
            throw new IllegalArgumentException("未注册的 scope domain/action");
        }
    }
}
