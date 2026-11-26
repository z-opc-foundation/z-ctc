package com.zifang.ctc.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.ctc.core.domain.entity.UserDO;
import com.zifang.ctc.core.domain.service.UserDbService;
import com.zifang.ctc.core.vo.StaffSimpleVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link AcStaffSimpleServiceImpl} 字段映射自测（内存桩，不依赖 DB）。
 *
 * <p><b>最关键的一条是 {@code extend != null}</b>：消费方
 * {@code TeamStaffAdapter#parseStaffResponse} 里有
 * {@code if (extend == null || extend.isEmpty()) continue;}，
 * 返回 null 会让<b>整条记录被静默丢弃</b>。这正是"接口 200 但数据恒空"的
 * 一种典型形态，必须钉死。
 *
 * <p>运行：javac + java（无 JUnit），见类末尾 main。
 */
public class AcStaffSimpleServiceTest {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) {
        // ── 1. 空数据 ────────────────────────────────────────────────
        eq("无用户返回空列表", 0, svc(Collections.emptyList()).listSimpleStaff("default").size());
        eq("null 输入返回空列表", 0, svc(null).listSimpleStaff("default").size());

        // ── 2. 单条映射 ─────────────────────────────────────────────
        List<StaffSimpleVO> one = svc(Collections.singletonList(
                user(1L, "U001", "alice", "爱丽丝", "13800000001", "a@x.com", "http://av/1.png", 1)))
                .listSimpleStaff("default");
        eq("返回 1 条", 1, one.size());
        StaffSimpleVO v = one.get(0);
        eq("id", Long.valueOf(1L), v.getId());
        eq("jobNumber ← user_no", "U001", v.getJobNumber());
        eq("accountNo ← username", "alice", v.getAccountNo());
        eq("name ← nickname", "爱丽丝", v.getName());
        eq("telephone ← phone", "13800000001", v.getTelephone());
        eq("extend 非 null（消费方会丢弃空 extend）", true, v.getExtend() != null);
        eq("extend 含 email", "a@x.com", v.getExtend().get("email"));
        eq("extend 含 flowerName（消费方读它做花名）", "爱丽丝", v.getExtend().get("flowerName"));
        eq("extend 不含密码", false, v.getExtend().containsKey("passwordHash"));
        eq("extend 不含 password_hash", false, v.getExtend().containsKey("password_hash"));

        // ── 3. nickname 为空时回落 username（名录不能有匿名条目）─────
        List<StaffSimpleVO> fallback = svc(Collections.singletonList(
                user(2L, "U002", "bob", null, null, null, null, 1))).listSimpleStaff("default");
        eq("nickname 空时 name 回落 username", "bob", fallback.get(0).getName());
        eq("nickname 为空串时也回落", "alice",
                svc(Collections.singletonList(user(3L, "U003", "alice", "  ", null, null, null, 1)))
                        .listSimpleStaff("default").get(0).getName());

        // ── 4. 敏感字段绝不外泄 ─────────────────────────────────────
        StaffSimpleVO sec = svc(Collections.singletonList(
                user(4L, "U004", "carol", "卡罗", "13900000002", "c@x.com", null, 1)))
                .listSimpleStaff("default").get(0);
        eq("vo 上无 passwordHash 属性", false, hasField(sec, "passwordHash"));
        eq("vo 上无 password 属性", false, hasField(sec, "password"));

        // ── 5. 雪花 ID 无损（19 位）──────────────────────────────────
        String snow = "1791049245658912345";
        StaffSimpleVO s19 = svc(Collections.singletonList(
                user(Long.parseLong(snow), "U019", "dave", "戴夫", null, null, null, 1)))
                .listSimpleStaff("default").get(0);
        eq("19 位 id 无损", snow, String.valueOf(s19.getId()));

        // ── 6. id 为 null 的脏数据被跳过（不该产出无名条目）──────────
        List<UserDO> dirty = new ArrayList<>();
        dirty.add(user(null, "U000", "ghost", "幽灵", null, null, null, 1));
        dirty.add(user(9L, "U009", "real", "真实", null, null, null, 1));
        List<StaffSimpleVO> cleaned = svc(dirty).listSimpleStaff("default");
        eq("id 为 null 的被跳过", 1, cleaned.size());
        eq("保留有效那条", Long.valueOf(9L), cleaned.get(0).getId());

        // ── 7. tenantCode 任意取值都不影响结果（本表无租户维度）──────
        List<StaffSimpleVO> t1 = svc(Collections.singletonList(
                user(1L, "U001", "alice", "爱丽丝", null, null, null, 1))).listSimpleStaff("default");
        List<StaffSimpleVO> t2 = svc(Collections.singletonList(
                user(1L, "U001", "alice", "爱丽丝", null, null, null, 1))).listSimpleStaff("other-tenant");
        eq("换 tenantCode 结果一致（不做错误过滤）", t1.size(), t2.size());

        System.out.println("\n----------------------------------------");
        System.out.println("AcStaffSimpleService selftest: pass=" + pass + " fail=" + fail);
        System.out.println("----------------------------------------");
        if (fail > 0) {
            System.exit(1);
        }
    }

    private static boolean hasField(Object o, String name) {
        for (java.lang.reflect.Method m : o.getClass().getMethods()) {
            if (m.getName().equals("get" + name.substring(0, 1).toUpperCase() + name.substring(1))) {
                return true;
            }
        }
        return false;
    }

    private static AcStaffSimpleServiceImpl svc(List<UserDO> users) {
        return new AcStaffSimpleServiceImpl(new UserDbService() {
            @Override public int insert(UserDO e) { throw new UnsupportedOperationException(); }
            @Override public int updateById(UserDO e) { throw new UnsupportedOperationException(); }
            @Override public int deleteById(Long i) { throw new UnsupportedOperationException(); }
            @Override public UserDO selectById(Long i) { return null; }
            @Override public UserDO selectByUserNo(String n) { return null; }
            @Override public UserDO selectByEmail(String e) { return null; }
            @Override public UserDO selectByPhone(String p) { return null; }
            @Override public UserDO selectByUsername(String u) { return null; }
            @Override public List<UserDO> selectByQuery(QueryWrapper<UserDO> w) { return users; }
        });
    }

    private static UserDO user(Long id, String userNo, String username, String nickname,
                               String phone, String email, String avatar, int status) {
        UserDO u = new UserDO();
        u.setId(id);
        u.setUserNo(userNo);
        u.setUsername(username);
        u.setNickname(nickname);
        u.setPhone(phone);
        u.setEmail(email);
        u.setAvatar(avatar);
        u.setStatus(status);
        u.setPasswordHash("SHOULD_NEVER_LEAK");
        return u;
    }

    private static void eq(String name, Object expected, Object actual) {
        if (java.util.Objects.equals(expected, actual)) {
            pass++;
        } else {
            fail++;
            System.out.println("FAIL  " + name + "  expected=" + expected + " actual=" + actual);
        }
    }
}
