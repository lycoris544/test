package com.exam.common.enums;

/**
 * 角色。存库为 int，避免 MySQL/H2 之间 ENUM 类型差异。
 */
public final class Role {

    /** 教师 */
    public static final int TEACHER = 1;
    /** 学生 */
    public static final int STUDENT = 2;

    private Role() {
    }

    public static String nameOf(Integer role) {
        if (role == null) {
            return "未知";
        }
        switch (role) {
            case TEACHER:
                return "教师";
            case STUDENT:
                return "学生";
            default:
                return "未知";
        }
    }

    /** 对应 Spring Security 的权限标识 */
    public static String authorityOf(Integer role) {
        if (role != null && role == TEACHER) {
            return "ROLE_TEACHER";
        }
        return "ROLE_STUDENT";
    }
}