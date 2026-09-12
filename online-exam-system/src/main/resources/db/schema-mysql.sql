-- =====================================================================
-- 在线考试系统 - MySQL 建表脚本
--
-- 用法：
--   CREATE DATABASE exam_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
--   mysql -uroot -p exam_db < src/main/resources/db/schema-mysql.sql
--   mysql -uroot -p exam_db < src/main/resources/db/data.sql
--   启动时加 --spring.profiles.active=mysql
--
-- 注意：本文件和 schema.sql（H2 版）的字段结构必须保持一致，
-- 改表结构时两边都要改。
-- =====================================================================

SET NAMES utf8mb4;

DROP TABLE IF EXISTS answer;
DROP TABLE IF EXISTS exam_session;
DROP TABLE IF EXISTS course_enrollment;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS sys_user;

-- ---------------------------------------------------------------------
-- 用户：教师和学生共用，role 区分（1 教师 / 2 学生）
-- ---------------------------------------------------------------------
CREATE TABLE sys_user
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(32)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    real_name   VARCHAR(32)  NOT NULL COMMENT '姓名',
    gender      INT COMMENT '性别：1 男 / 2 女',
    phone       VARCHAR(20) COMMENT '手机号',
    user_no     VARCHAR(32) COMMENT '工号（教师）/ 学号（学生）',
    department  VARCHAR(64) COMMENT '所在学院',
    title       VARCHAR(32) COMMENT '职称，仅教师',
    role        INT          NOT NULL COMMENT '角色：1 教师 / 2 学生',
    status      INT          NOT NULL DEFAULT 1 COMMENT '状态：0 禁用 / 1 正常',
    create_time DATETIME     NOT NULL COMMENT '创建时间',
    update_time DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_no (user_no)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ---------------------------------------------------------------------
-- 课程：一门课程属于一位授课教师
-- ---------------------------------------------------------------------
CREATE TABLE course
(
    id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name             VARCHAR(64) NOT NULL COMMENT '课程名称',
    code             VARCHAR(32) NOT NULL COMMENT '课程编码',
    description      VARCHAR(500) COMMENT '课程简介',
    teacher_id       BIGINT      NOT NULL COMMENT '授课教师 ID',
    duration_minutes INT         NOT NULL DEFAULT 60 COMMENT '考试时长（分钟）',
    exam_enabled     INT         NOT NULL DEFAULT 0 COMMENT '是否开放考试：0 关闭 / 1 开放',
    create_time      DATETIME    NOT NULL COMMENT '创建时间',
    update_time      DATETIME    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_code (code),
    KEY idx_course_teacher (teacher_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='考试课程表';

-- ---------------------------------------------------------------------
-- 选课关系
-- ---------------------------------------------------------------------
CREATE TABLE course_enrollment
(
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    course_id   BIGINT   NOT NULL COMMENT '课程 ID',
    student_id  BIGINT   NOT NULL COMMENT '学生 ID',
    create_time DATETIME NOT NULL COMMENT '选课时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_enrollment (course_id, student_id),
    KEY idx_enrollment_student (student_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='选课关系表';

-- ---------------------------------------------------------------------
-- 题目：选项以 JSON 文本存储
-- ---------------------------------------------------------------------
CREATE TABLE question
(
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    course_id   BIGINT        NOT NULL COMMENT '所属课程 ID',
    type        INT           NOT NULL COMMENT '题型：1 单选 / 2 多选 / 3 判断 / 4 填空',
    content     VARCHAR(2000) NOT NULL COMMENT '题干',
    options     VARCHAR(4000) COMMENT '选项 JSON，如 [{"key":"A","content":"..."}]',
    answer      VARCHAR(500)  NOT NULL COMMENT '正确答案；填空多答案用 | 分隔',
    score       INT           NOT NULL COMMENT '分值',
    analysis    VARCHAR(2000) COMMENT '解析',
    sort_order  INT           NOT NULL DEFAULT 0 COMMENT '排序号',
    create_time DATETIME      NOT NULL COMMENT '创建时间',
    update_time DATETIME      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_question_course (course_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='题目表';

-- ---------------------------------------------------------------------
-- 考试记录：重考会产生多条，attempt_no 记录是第几次
-- ---------------------------------------------------------------------
CREATE TABLE exam_session
(
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    course_id   BIGINT   NOT NULL COMMENT '课程 ID',
    student_id  BIGINT   NOT NULL COMMENT '学生 ID',
    attempt_no  INT      NOT NULL DEFAULT 1 COMMENT '第几次考试，从 1 开始',
    status      INT      NOT NULL DEFAULT 0 COMMENT '状态：0 进行中 / 1 已交卷 / 2 超时交卷',
    start_time  DATETIME NOT NULL COMMENT '开考时间',
    deadline    DATETIME COMMENT '最晚交卷时间',
    submit_time DATETIME COMMENT '实际交卷时间',
    score       INT COMMENT '得分',
    total_score INT COMMENT '试卷总分快照',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_session_student_course (student_id, course_id),
    KEY idx_session_course (course_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='考试记录表';

-- ---------------------------------------------------------------------
-- 作答明细：交卷时写入，correct_answer / score 是判分当时的快照
-- ---------------------------------------------------------------------
CREATE TABLE answer
(
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id     BIGINT        NOT NULL COMMENT '考试记录 ID',
    question_id    BIGINT        NOT NULL COMMENT '题目 ID',
    user_answer    VARCHAR(2000) COMMENT '学生作答',
    correct_answer VARCHAR(500) COMMENT '正确答案快照',
    is_correct     INT           NOT NULL DEFAULT 0 COMMENT '是否答对：0 否 / 1 是',
    score          INT           NOT NULL DEFAULT 0 COMMENT '本题得分',
    full_score     INT           NOT NULL DEFAULT 0 COMMENT '本题满分',
    PRIMARY KEY (id),
    KEY idx_answer_session (session_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='作答明细表';
