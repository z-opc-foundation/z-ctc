package com.zifang.ctc.core.ak;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.zifang.ctc.core.ak.model.AkScope;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 调用日志脱敏：失败一律丢弃原文；绝不回退为未脱敏文本。 */
public final class AkCallPayloadSanitizer {
    public static final String REDACTED = "***";
    private static final JsonFactory JSON = new JsonFactory()
            .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    private static final Set<String> SENSITIVE = new HashSet<>(Arrays.asList(
            "password", "passwd", "pwd", "secret", "clientsecret", "token", "accesstoken",
            "refreshtoken", "idtoken", "apikey", "ak", "sk", "akcode", "xfbak",
            "secretkey", "accesskey", "accesskeyid", "accesskeysecret", "authorization",
            "cookie", "setcookie", "signature", "sign", "credential", "credentials",
            "phone", "mobile", "phonenumber", "idcard", "identitycard", "idcardnumber"));
    private static final Pattern URL_PARAMETER = Pattern.compile("([?&#])([^?&#=]+)=([^&#]*)");
    private static final Pattern URL_USER_INFO = Pattern.compile("(https?://)[^/@\\s]+@",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BEARER = Pattern.compile("(?i)\\b(Bearer|Basic)\\s+[A-Za-z0-9._~+/=-]+");
    private static final Pattern TOKEN_VALUE = Pattern.compile(
            "(?i)\\b(secret|token|password|api[_-]?key|access[_-]?key)\\b\\s*[:=]\\s*[^\\s,;]+");

    private AkCallPayloadSanitizer() {
    }

    public static boolean isSensitive(String name) {
        String key = name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return SENSITIVE.contains(key) || key.endsWith("token") || key.endsWith("password")
                || key.endsWith("secret") || key.endsWith("signature")
                || key.startsWith("xamz") || key.startsWith("xoss");
    }

    public static Map<String, String> headers(Map<String, String> source) {
        Map<String, String> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey();
            String normalized = key.toLowerCase(Locale.ROOT);
            // AK 是认证秘密，连掩码或哈希也不进入日志。
            if (normalized.equals("x-ak") || normalized.equals("x-fb-ak")) {
                continue;
            }
            result.put(key, isSensitive(key) ? REDACTED : safeText(entry.getValue()));
        }
        return result;
    }

    public static String body(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        if (value.length() > 65536) {
            return "[REDACTED:payload-too-large]";
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            copy(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)), output);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "[REDACTED:invalid-payload]";
        }
    }

    public static String query(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.length() > 8192) {
            return "[REDACTED:query-too-large]";
        }
        try {
            String[] parts = value.split("&", -1);
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                int separator = part.indexOf('=');
                String key = separator < 0 ? part : part.substring(0, separator);
                String parameterValue = separator < 0 ? "" : part.substring(separator + 1);
                String decodedKey = URLDecoder.decode(key, "UTF-8");
                String sanitized = isSensitive(decodedKey) ? REDACTED : redactUrlParameters(redactEmbedded(parameterValue));
                if (i > 0) result.append('&');
                result.append(key).append(separator < 0 ? "" : '=').append(sanitized);
            }
            return URL_USER_INFO.matcher(result).replaceAll("$1***@");
        } catch (Exception ex) {
            return "[REDACTED:invalid-query]";
        }
    }

    public static String safeText(String value) {
        if (value == null) {
            return "";
        }
        String text = URL_USER_INFO.matcher(redactEmbedded(value)).replaceAll("$1***@");
        return text.length() > 1000 ? text.substring(0, 1000) : text;
    }

    /** 在结构化脱敏前清除本次 AK 明文，覆盖回显到任意非敏感字段的情况。 */
    public static String redactExact(String value, String secret) {
        if (value == null || secret == null || secret.isEmpty()) return value;
        return value.replace(secret, REDACTED);
    }

    private static String redactEmbedded(String value) {
        if (value == null) return "";
        String text = BEARER.matcher(value).replaceAll("$1 " + REDACTED);
        return TOKEN_VALUE.matcher(text).replaceAll("$1=" + REDACTED);
    }

    /** 结构化 scope 只接受受控标识符；禁止通配 domain/action 与嵌入通配。 */
    public static AkScope normalize(AkScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("scope 不能为空");
        }
        String domain = identifier(scope.getDomain(), "domain");
        String action = identifier(scope.getAction(), "action");
        String resource = scope.getResource() == null ? "" : scope.getResource().trim();
        if (resource.isEmpty() || resource.length() > 512
                || !resource.matches("[A-Za-z0-9_./:*@-]+")
                || (resource.indexOf('*') >= 0 && !resource.equals("*"))) {
            throw new IllegalArgumentException("scope.resource 非法");
        }
        return new AkScope(domain, action, resource, safeName(scope.getName()));
    }

    private static String identifier(String value, String field) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z][A-Z0-9_]{0,63}") || normalized.contains("*")) {
            throw new IllegalArgumentException("scope." + field + " 非法");
        }
        return normalized;
    }

    private static String safeName(String value) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        return text.length() > 255 ? text.substring(0, 255) : text;
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        try (JsonParser parser = JSON.createParser(input);
             JsonGenerator generator = JSON.createGenerator(output)) {
            if (parser.nextToken() == null) {
                throw new IOException("empty JSON");
            }
            copyValue(parser, generator, 0);
            if (parser.nextToken() != null) {
                throw new IOException("multiple root values");
            }
        }
    }

    private static void copyValue(JsonParser parser, JsonGenerator out, int depth) throws IOException {
        if (depth > 128) {
            throw new IOException("too deep");
        }
        JsonToken token = parser.currentToken();
        if (token == JsonToken.START_OBJECT) {
            out.writeStartObject();
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() != JsonToken.FIELD_NAME || parser.nextToken() == null) {
                    throw new IOException("invalid object");
                }
                String name = parser.currentName();
                out.writeFieldName(name);
                if (isSensitive(name)) {
                    parser.skipChildren();
                    out.writeString(REDACTED);
                } else {
                    copyValue(parser, out, depth + 1);
                }
            }
            out.writeEndObject();
        } else if (token == JsonToken.START_ARRAY) {
            out.writeStartArray();
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.currentToken() == null) {
                    throw new IOException("invalid array");
                }
                copyValue(parser, out, depth + 1);
            }
            out.writeEndArray();
        } else if (token == JsonToken.VALUE_STRING) {
            out.writeString(sanitizeUrls(parser.getText()));
        } else if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
            out.writeNumber(parser.getText());
        } else if (token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
            out.writeBoolean(token == JsonToken.VALUE_TRUE);
        } else if (token == JsonToken.VALUE_NULL) {
            out.writeNull();
        } else {
            throw new IOException("invalid value");
        }
    }

    private static String sanitizeUrls(String value) throws IOException {
        return URL_USER_INFO.matcher(redactUrlParameters(redactEmbedded(value))).replaceAll("$1***@");
    }

    private static String redactUrlParameters(String value) throws java.io.UnsupportedEncodingException {
        Matcher matcher = URL_PARAMETER.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = URLDecoder.decode(matcher.group(2), "UTF-8");
            String replacement = isSensitive(key)
                    ? matcher.group(1) + matcher.group(2) + "=" + REDACTED : matcher.group();
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
