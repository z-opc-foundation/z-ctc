package com.zifang.ctc.sso.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.ctc.sso.model.UserInfo;
import com.zifang.util.http.base.define.RequestMethod;
import com.zifang.util.http.base.pojo.HttpRequestDefinition;
import com.zifang.util.http.base.pojo.HttpRequestLine;
import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.http.client.HttpExecutor;

import javax.annotation.Resource;
import com.zifang.util.core.json.JsonMapperFactory;

public class RemoteTokenService implements TokenService {

    private final ObjectMapper objectMapper = JsonMapperFactory.getDefault();
    @Resource
    private SsoProperties ssoProperties;

    @Override
    public UserInfo verifyToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            HttpRequestLine requestLine = new HttpRequestLine();
            requestLine.setRequestMethod(RequestMethod.GET);
            requestLine.setUrl(ssoProperties.getAuthServerUrl());

            HttpRequestDefinition def = new HttpRequestDefinition();
            def.setHttpRequestLine(requestLine);
            def.getHttpRequestHeader().put("Authorization", "Bearer " + token);

            HttpExecutionResult result = HttpExecutor.getDefault().execute(def);
            if (result.isSuccess() && result.getBody() != null) {
                return objectMapper.readValue(result.getBody(), UserInfo.class);
            }
            return null;
        } catch (Exception e) {
            // 验证失败
            return null;
        }
    }
}
