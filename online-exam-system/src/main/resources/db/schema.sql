-- =====================================================================
-- 在线考试系统 - H2 建表脚本（默认 profile 使用）
--
-- H2 以 MODE=MySQL 运行，因此尽量贴近 MySQL 写法，但两者仍有差异：
--   * H2 不支持 MySQL 的 ENGINE= / CHARSET= 表选项
--   * H2 不支持 MySQL 的 COMMENT '...' E2 列注释语法
-- 因此没有和 schema-mysql.sql 合并成一个文件，而是各维护一份。
-- 改表结构时两个文件都要改。
-- =====================================================================

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
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(32)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    real_name   VARCHAR(32)  NOT NULL,
    gender      INT,
    phone       VARCHAR(20),
    user_no     VARCHAR(32),
    department  VARCHAR(64),
    title       VARCHAR(32),
    role        INT          NOT NULL,
    status      INT          NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT uk_user_no UNIQUE (user_no)
);

-- ---------------------------------------------------------------------
-- 课程：一门课程属于一位授课教师
-- ---------------------------------------------------------------------
CREATE TABLE course
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(64)  NOT NULL,
    code             VARCHAR(32)  NOT NULL,
    description      VARCHAR(500),
    teacher_id       BIGINT       NOT NULL,
    duration_minutes INT          NOT NULL DEFAULT 60,
    exam_enabled     INT          NOT NULL DEFAULT 0,
    create_time      DATETIME     NOT NULL,
    update_time      DATETIME     NOT NULL,
    CONSTRAINT uk_course_code UNIQUE (code)
);

CREATE INDEX idx_course_teacher ON course (teacher_id);

-- ---------------------------------------------------------------------
-- 选课关系
-- ---------------------------------------------------------------------
CREATE TABLE course_enrollment
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id   BIGINT   NOT NULL,
    student_id  BIGINT   NOT NULL,
    create_time DATETIME NOT NULL,
    CONSTRAINT uk_enrollment UNIQUE (course_id, student_id)
);

CREATE INDEX idx_enrollment_student ON course_enrollment (student_id);

-- ---------------------------------------------------------------------
-- 题目：选项以 JSON 文本存储，由 JacksonTypeHandler 转换
-- ---------------------------------------------------------------------
CREATE TABLE question
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id   BIGINT        NOT NULL,
    type        INT           NOT NULL,
    content     VARCHAR(2000) NOT NULL,
    options     VARCHAR(4000),
    answer      VARCHAR(500)  NOT NULL,
    score       INT           NOT NULL,
    analysis    VARCHAR(2000),
    sort_order  INT           NOT NULL DEFAULT 0,
    create_time DATETIME      NOT NULL,
    update_time DATETIME      NOT NULL
);

CREATE INDEX idx_question_course ON question (course_id);

-- ---------------------------------------------------------------------
-- 考试记录：一个学生一门课可以有多条（重考），attempt_no 记录次数
-- ---------------------------------------------------------------------
CREATE TABLE exam_session
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id   BIGINT   NOT NULL,
    student_id  BIGINT   NOT NULL,
    attempt_no  INT      NOT NULL DEFAULT 1,
    status      INT      NOT NULL DEFAULT 0,
    start_time  DATETIME NOT NULL,
    deadline    DATETIME,
    submit_time DATETIME,
    score       INT,
    total_score INT,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
);

CREATE INDEX idx_session_student_course ON exam_session (student_id, course_id);
CREATE INDEX idx_session_course ON exam_session (course_id);

-- ---------------------------------------------------------------------
-- 作答明细：交卷时写入，correct_answer / score 为判分当时的快照
-- ---------------------------------------------------------------------
CREATE TABLE answer
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id     BIGINT       NOT NULL,
    question_id    BIGINT       NOT NULL,
    user_answer    VARCHAR(2000),
    correct_answer VARCHAR(500),
    is_correct     INT          NOT NULL DEFAULT 0,
    score          INT          NOT NULL DEFAULT 0,
    full_score     INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_answer_session ON answer (session_id);
