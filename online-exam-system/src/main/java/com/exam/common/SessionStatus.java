package com.exam.common;

/**
 * 一场考试（答题记录）的状态。
 */
public final class SessionStatus {

    /** 进行中：已开始答题，尚未交卷 */
    public static final int ONGOING = 0;
    /** 已交卷：学生主动提交，已判分 */
    public static final int SUBMITTED = 1;
    /** 超时强制交卷：服务端发现答题时长已超过课程限时，
     * 在学生下次请求时自动结算（见 {@code ExamService#submit}）。 */
    public static final int TIMEOUT = 2;

    /**
     * 未参加考试。
     *
     * <p>这个值不会写进数据库——它只出现在教师端统计里，
     * 用来表示「该学生选了课但一次都没考」这一行。之所以占用 -1 这个哨兵值，
     * 是为了让统计列表能和学生列表用同一个对象承载，
     * 前端拿到 -1 就知道要显示「未参加」而不是某个分数。</p>
     */
    public static final int NOT_TAKEN = -1;

    private SessionStatus() {
    }

    public static String nameOf(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case NOT_TAKEN:
                return "未参加";
            case ONGOING:
                return "进行中";
            case SUBMITTED:
                return "已交卷";
            case TIMEOUT:
                return "超时交卷";
            default:
                return "未知";
        }
    }

    /** 是否已结束（结束即已判分，不可继续作答） */
    public static boolean isFinished(Integer status) {
        return status != null && status != ONGOING;
    }
}
