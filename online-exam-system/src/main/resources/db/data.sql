-- =====================================================================
-- 在线考试系统 - 演示数据
--
-- H2 和 MySQL 通用，两边都能直接执行。
--
-- 演示账号（密码统一为 123456）
--   教师： teacher_li  / 123456   李明   工号 T2001   计算机学院
--           teacher_wang / 123456  王芳   工号 T2002   计算机学院
--   学生： student_zhang / 123456 张三   学号 20230001 计算机学院
--           student_li    / 123456 李四   学号 20230002 计算机学院
--           student_zhao  / 123456 赵六   学号 20230003 软件学院
--
-- 密码为 BCrypt 哈希（同一明文 123456 的哈希，已验证可登录）。
-- 注意：这里刻意不预置考试记录。考试数据请通过接口真实跑一遍生成，
-- 这样 scores / total_score / answer 表之间一定自洽——
-- 手写假成绩很容易和题目分值对不上，答辩演示时反而出丑。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 用户
-- ---------------------------------------------------------------------
INSERT INTO sys_user (username, password, real_name, gender, phone, user_no, department, title, role, status, create_time, update_time)
VALUES ('teacher_li', '$2a$10$cYBWxg3iavB8BW1roZ5kAO5w1eGCNUBZJGi.DWDEWsFI0WQRsJhfO', '李明', 1, '13800000001', 'T2001', '计算机学院', '教授', 1, 1, NOW(), NOW()),
       ('teacher_wang', '$2a$10$cYBWxg3iavB8BW1roZ5kAO5w1eGCNUBZJGi.DWDEWsFI0WQRsJhfO', '王芳', 2, '13800000002', 'T2002', '计算机学院', '副教授', 1, 1, NOW(), NOW()),
       ('student_zhang', '$2a$10$cYBWxg3iavB8BW1roZ5kAO5w1eGCNUBZJGi.DWDEWsFI0WQRsJhfO', '张三', 1, '13900000001', '20230001', '计算机学院', NULL, 2, 1, NOW(), NOW()),
       ('student_li', '$2a$10$cYBWxg3iavB8BW1roZ5kAO5w1eGCNUBZJGi.DWDEWsFI0WQRsJhfO', '李四', 1, '13900000002', '20230002', '计算机学院', NULL, 2, 1, NOW(), NOW()),
       ('student_zhao', '$2a$10$cYBWxg3iavB8BW1roZ5kAO5w1eGCNUBZJGi.DWDEWsFI0WQRsJhfO', '赵六', 2, '13900000003', '20230003', '软件学院', NULL, 2, 1, NOW(), NOW());

-- ---------------------------------------------------------------------
-- 课程（id 由自增生成，按插入顺序为 1、2、3）
-- ---------------------------------------------------------------------
INSERT INTO course (name, code, description, teacher_id, duration_minutes, exam_enabled, create_time, update_time)
VALUES ('Java 程序设计', 'CS101', 'Java 语言基础、面向对象、集合与异常处理', 1, 60, 1, NOW(), NOW()),
       ('数据库原理', 'CS102', '关系模型、SQL、事务与索引', 1, 45, 1, NOW(), NOW()),
       ('计算机网络', 'CS201', 'TCP/IP 协议栈与网络编程基础', 2, 90, 0, NOW(), NOW());

-- ---------------------------------------------------------------------
-- 选课（赵六刻意不选 CS102、CS201，用于演示「可选课程」列表）
-- ---------------------------------------------------------------------
INSERT INTO course_enrollment (course_id, student_id, create_time)
VALUES (1, 3, NOW()),
       (2, 3, NOW()),
       (3, 3, NOW()),
       (1, 4, NOW()),
       (2, 4, NOW()),
       (1, 5, NOW());

-- ---------------------------------------------------------------------
-- 题目
-- 题型：1 单选 / 2 多选 / 3 判断 / 4 填空
-- 选项存 JSON；判断题固定 A=正确、B=错误
-- 每题分值 10 分，四门课卷面总分见注释
-- ---------------------------------------------------------------------

-- === 课程 1：Java 程序设计（5 题，卷面 50 分）===
INSERT INTO question (course_id, type, content, options, answer, score, analysis, sort_order, create_time, update_time)
VALUES (1, 1, '下列哪个关键字用于在 Java 中定义常量？',
        '[{"key":"A","content":"static"},{"key":"B","content":"final"},{"key":"C","content":"const"},{"key":"D","content":"immutable"}]',
        'B', 10, 'final 修饰的变量只能赋值一次，即常量。static 表示类级别，const 在 Java 中是保留字但未使用。', 1, NOW(), NOW()),

       (1, 2, '以下哪些属于 Java 的基本数据类型？（多选）',
        '[{"key":"A","content":"int"},{"key":"B","content":"String"},{"key":"C","content":"boolean"},{"key":"D","content":"double"}]',
        'ACD', 10, 'String 是引用类型，不是基本数据类型。Java 的八种基本类型：byte/short/int/long/float/double/char/boolean。', 2, NOW(), NOW()),

       (1, 3, 'Java 支持类的多继承，一个类可以同时继承多个父类。',
        '[{"key":"A","content":"正确"},{"key":"B","content":"错误"}]',
        'B', 10, 'Java 类只支持单继承，多继承通过接口实现。这是 Java 与 C++ 的重要区别。', 3, NOW(), NOW()),

       (1, 4, 'Java 中用于捕获异常的完整语法关键字是 try-catch-finally，其中无论是否发生异常都会执行的是 ____ 块。',
        NULL, 'finally', 10, 'finally 块无论是否抛出异常都会执行，常用于释放资源。', 4, NOW(), NOW()),

       (1, 1, 'ArrayList 与 LinkedList 相比，随机访问元素的时间复杂度是？',
        '[{"key":"A","content":"O(1)"},{"key":"B","content":"O(log n)"},{"key":"C","content":"O(n)"},{"key":"D","content":"O(n^2)"}]',
        'A', 10, 'ArrayList 底层是数组，支持下标随机访问，复杂度 O(1)；LinkedList 需从头遍历，为 O(n)。', 5, NOW(), NOW());

-- === 课程 2：数据库原理（4 题，卷面 40 分）===
INSERT INTO question (course_id, type, content, options, answer, score, analysis, sort_order, create_time, update_time)
VALUES (2, 1, 'SQL 中用于去除查询结果重复行的关键字是？',
        '[{"key":"A","content":"UNIQUE"},{"key":"B","content":"DISTINCT"},{"key":"C","content":"DIFFERENT"},{"key":"D","content":"REMOVE"}]',
        'B', 10, 'DISTINCT 用于对查询结果去重。UNIQUE 是约束，不是查询关键字。', 1, NOW(), NOW()),

       (2, 2, '以下哪些属于数据库事务的 ACID 特性？（多选）',
        '[{"key":"A","content":"原子性 Atomicity"},{"key":"B","content":"一致性 Consistency"},{"key":"C","content":"隔离性 Isolation"},{"key":"D","content":"持久性 Durability"}]',
        'ABCD', 10, 'ACID 分别指原子性、一致性、隔离性、持久性，四者缺一不可。', 2, NOW(), NOW()),

       (2, 3, '在 MySQL 的 InnoDB 引擎中，主键索引属于聚簇索引。',
        '[{"key":"A","content":"正确"},{"key":"B","content":"错误"}]',
        'A', 10, 'InnoDB 中主键索引即聚簇索引，叶子节点直接存放整行数据；二级索引叶子节点存放主键值。', 3, NOW(), NOW()),

       (2, 4, 'SQL 中用于对查询结果分组的关键字是 ____。',
        NULL, 'GROUP BY|group by', 10, 'GROUP BY 按指定列分组，通常与聚合函数（COUNT/SUM/AVG 等）配合使用。答案不区分大小写。', 4, NOW(), NOW());

-- === 课程 3：计算机网络（3 题，卷面 30 分，考试未开放）===
INSERT INTO question (course_id, type, content, options, answer, score, analysis, sort_order, create_time, update_time)
VALUES (3, 1, 'TCP 协议位于 OSI 参考模型的哪一层？',
        '[{"key":"A","content":"网络层"},{"key":"B","content":"传输层"},{"key":"C","content":"会话层"},{"key":"D","content":"应用层"}]',
        'B', 10, 'TCP 和 UDP 都工作在传输层，负责端到端的可靠/不可靠数据传输。', 1, NOW(), NOW()),

       (3, 3, 'TCP 建立连接需要经过三次握手。',
        '[{"key":"A","content":"正确"},{"key":"B","content":"错误"}]',
        'A', 10, '三次握手：SYN、SYN+ACK、ACK，目的是确认双方的收发能力都正常。', 2, NOW(), NOW()),

       (3, 4, 'HTTP 协议默认使用的端口号是 ____。',
        NULL, '80', 10, 'HTTP 默认端口 80，HTTPS 默认端口 443。', 3, NOW(), NOW());
