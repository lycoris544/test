# 在线考试系统 - 后端

2026 秋季前后端转正考核项目。实现需求文档中「在线考试系统」的完整后端：登录后区分**教师端**与**学生端**。

---

## 一、快速开始

### 环境要求

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | **1.8** | 本机是 1.8，因此 Spring Boot 只能用 2.7.x（3.x 要求 JDK 17+ 且包名迁移到 `jakarta.*`） |
| Maven | 3.6+ | |
| MySQL | 8.0 | **可选**，默认用 H2 内存库，不装也能跑 |

### 启动（零依赖，H2 内存库）

```bash
mvn spring-boot:run
```

服务起在 `http://localhost:8080/api`，启动时自动建表并灌入演示数据。

- H2 控制台：<http://localhost:8080/api/h2-console>，JDBC URL 填 `jdbc:h2:mem:examdb`，用户名 `sa`，密码留空
- 换端口：`mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090`

> **注意**：H2 是内存库，进程一停数据就没了，每次重启都会回到初始演示数据。
> 这是刻意的——答辩演示时不用担心上一轮操作把数据搞脏。

### 启动（MySQL）

```bash
# 1. 建库
mysql -uroot -p -e "CREATE DATABASE exam_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
# 2. 建表 + 灌数据
mysql -uroot -p exam_db < src/main/resources/db/schema-mysql.sql
mysql -uroot -p exam_db < src/main/resources/db/data.sql
# 3. 用 mysql profile 启动
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=mysql
```

数据库连接可用环境变量覆盖，不用改代码：

```bash
MYSQL_HOST=localhost MYSQL_PORT=3306 MYSQL_DB=exam_db MYSQL_USER=root MYSQL_PASSWORD=你的密码 \
  mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=mysql
```

### 演示账号

密码统一 `123456`。

| 账号 | 角色 | 姓名 | 工号/学号 | 学院 |
|---|---|---|---|---|
| `teacher_li` | 教师 | 李明 | T2001 | 计算机学院 |
| `teacher_wang` | 教师 | 王芳 | T2002 | 计算机学院 |
| `student_zhang` | 学生 | 张三 | 20230001 | 计算机学院 |
| `student_li` | 学生 | 李四 | 20230002 | 计算机学院 |
| `student_zhao` | 学生 | 赵六 | 20230003 | 软件学院 |

演示数据含 3 门课、12 道题（单选/多选/判断/填空四种题型都有），覆盖所有场景。
其中「计算机网络」刻意设为**未开放考试**，「赵六」刻意**没选**数据库原理和计算机网络，
方便演示各种拦截逻辑。

---

## 二、验收方式

### 1. 单元测试（判分逻辑）

```bash
mvn test
```

`GradingServiceTest` 覆盖四种题型的判分边界：多选顺序无关、漏选/多选不得分、
填空忽略大小写与首尾空格、未作答记 0 分、空答案不得被误判为正确等，共 18 个用例。

### 2. 端到端验收脚本

```bash
# 先启动服务（另开一个终端）
mvn spring-boot:run

# 再跑脚本
python scripts/e2e_test.py
```

这个脚本会真实走完整个业务闭环，共 48 项断言：登录 → 开考 → 答题 → 交卷判分 →
查成绩 → 重考 → 教师查看成绩与统计 → 题库增删改查 → 权限与越权拦截。
**退出码 0 表示全部通过。**

它比单元测试更能说明问题：编译通过 ≠ 接口能用。脚本里包含了几条容易忽略的检查，
比如「试卷 JSON 里不能含 `answer` 字段」（防止把答案下发给学生）。

### 3. 接口文档一致性校验

```bash
# 不需要起服务：直接从 controller 源码比对接口清单
python scripts/check_openapi.py

# 需要服务在跑：用真实响应校验字段声明
python scripts/check_openapi_live.py
```

第一个脚本防止「改了接口忘了改文档」；第二个脚本防止「openapi.json 里的字段名是凭记忆写的」。
两个都以退出码 0 表示通过。

---

## 三、接口文档

### 在线调试（Apifox / Postman）

项目根目录的 **`openapi.json`** 是 OpenAPI 3.0 规范文件，**Apifox 可直接导入**：

1. Apifox → 「项目设置 → 导入数据 → OpenAPI/Swagger」
2. 选 `openapi.json`，导入模式选「智能合并」或「覆盖」
3. 导入后会按 tag 自动建成 7 个目录：认证 / 个人信息 / 教师端-课程管理 /
   教师端-题库管理 / 教师端-考试情况 / 学生端-选课 / 学生端-考试，共 31 个接口
4. 环境里把 Base URL 设为 `http://localhost:8080/api`

**自动带 token 的配置**（省得每个请求手填）：

- 在「登录」接口的后置操作里加一条「提取变量」，表达式 `data.token`，存到环境变量 `token`
- 在「项目设置 → 认证」里选 `Bearer Token`，值填 `{{token}}`

因为 spec 里声明了 `securitySchemes.bearerAuth`，导入后 Apifox 会自动识别鉴权方式。

### 写在文档里的版本

见 **[API.md](API.md)**，包含每个接口的字段说明、业务规则和失败提示。

> `API.md` 和 `openapi.json` 描述的是同一套接口。为了保证两者都不跑偏，
> `scripts/check_openapi.py` 会**从 controller 源码里提取所有 `@*Mapping`**，
> 与 spec 里的接口清单做双向比对（多一个、少一个都会报出来），同时校验 JSON 合法性、
> `$ref` 可解析性、`operationId` 唯一性。改完接口跑一下，就不会出现文档和代码不一致。

---

## 四、需求对照

需求文档中的每一条功能点落在哪个接口，见 **[REQUIREMENTS.md](REQUIREMENTS.md)**。

---

## 五、技术选型与理由

| 选择 | 理由 |
|---|---|
| Spring Boot 2.7.18 | 本机 JDK 1.8 所允许的最高版本 |
| MyBatis-Plus 3.5.3.1 | 单表 CRUD 零 SQL；复杂查询用 XML 手写，兼顾开发速度与 SQL 可控性 |
| JWT（jjwt 0.11.5） | 前后端分离，无状态鉴权，不需要 session 共享 |
| Spring Security | 路径级角色隔离（`/teacher/**` vs `/student/**`），比自己写拦截器可靠 |
| BCrypt | 自带盐值，不存明文也不存 MD5 |
| H2 + MySQL 双支持 | 默认零环境依赖；答辩/部署可切 MySQL |
| Lombok | 减少样板代码 |

---

## 六、目录结构

```
src/main/java/com/exam/
├── ExamApplication.java          启动类
├── common/                       统一响应 Result/PageResult、BizException、全局异常处理
│                                 Role / QuestionType / SessionStatus 常量
├── config/
│   ├── SecurityConfig.java       鉴权规则、CORS、密码加密器
│   └── MybatisPlusConfig.java    分页插件、全表更新拦截
├── security/                     JWT 工具、认证过滤器、登录态、当前用户读取
├── entity/                       表实体
├── mapper/                       MyBatis-Plus Mapper（复杂查询走 XML）
├── dto/                          请求/响应对象，按模块分包
├── service/                      业务逻辑
│   ├── AuthService               登录注册
│   ├── ProfileService            个人信息（两端共用）
│   ├── CourseService             课程管理
│   ├── QuestionService           题库管理（含题型校验）
│   ├── StudentService            选课
│   ├── ExamService               考试核心流程
│   ├── GradingService            判分（纯函数，可单测）
│   └── CourseStatsService        成绩统计
└── controller/                   HTTP 接口

src/main/resources/
├── application.yml               默认配置（H2）
├── application-mysql.yml         MySQL profile
├── db/schema.sql                 H2 建表
├── db/schema-mysql.sql           MySQL 建表
├── db/data.sql                   演示数据（两边通用）
└── mapper/*.xml                  手写 SQL

src/test/java/com/exam/service/GradingServiceTest.java
scripts/e2e_test.py               端到端验收脚本
scripts/check_openapi.py          校验 openapi.json 与 controller 接口清单一致
scripts/check_openapi_live.py     用真实响应校验 openapi.json 的字段声明

README.md                         本文件
API.md                            接口文档（写给后端/前端看的）
REQUIREMENTS.md                   需求对照表
openapi.json                      OpenAPI 3.0 规范（给 Apifox / Postman 导入的）
```

顶层还有 `pom.xml`、`.gitignore`、`settings.xml`（项目本地 Maven 配置）。

---

## 七、关键设计说明

### 1. 成绩不会被后来的改动影响

考试是「结果一旦产生就不可篡改」的场景，因此做了两层隔离：

- **判分快照**：交卷时把正确答案和每题得分写进 `answer` 表。教师之后修改题目答案、
  调整分值、甚至删掉题目，都不会改写已经产生的成绩。
- **总分快照**：交卷时把试卷总分写进 `exam_session.total_score`。教师事后加题导致
  卷面总分变化，历史成绩的「40/50」仍然显示 40/50。

### 2. 试卷绝不下发答案

考试接口返回的是 `ExamQuestion`，而不是 `Question` 实体。后者带 `answer` 和 `analysis`
字段，直接返回等于把答案送给学生——前端按 F12 就能看到。
`scripts/e2e_test.py` 里有一条断言专门守这个。

### 3. 重考与考试状态

同一个学生同一门课可以有多条 `exam_session`，`attempt_no` 记录是第几次。
课程列表的状态由 `CourseEnrollmentMapper.xml` 算出，三种情形：

| 情形 | `canStart` | 行为 |
|---|---|---|
| 从未考过 | `true` | 可直接开考 |
| 有进行中的考试 | `true` | 继续未完成的答卷（中途退出后回来） |
| 已考过且无进行中记录 | `false` | 需显式调 `restart=true`（重考按钮）才开新场次 |

「继续进行中考试」优先于「已考过需重考」，避免同一门课同时存在两场进行中的考试。

### 4. 超时处理

开考时把 `deadline = 开考时间 + 课程时长` 算好存库，之后教师改课程时长不影响进行中的考试。

交卷时若已超过 `deadline`，**答案仍然照收并正常判分**，但状态记为「超时交卷」
（`status=2`），返回体里 `timeout=true`，前端应给出明确提示。

这里有一个已知取舍，说明如下：理想做法是「到 deadline 那一刻把已答内容冻结」，
那需要每答一题就落库（或前端定时上报）。本次实现是交卷时一次性落库，
因此**超过 deadline 后继续作答的内容也会被计入**，只是被标记为超时交卷。
如果考核要求严格封卷，需要改成逐题保存 —— 这是设计选择，不是遗漏。

### 5. 多选题不给部分分

多选只有「全部选对」才得分，漏选、多选、错选均为 0 分。
判分时对答案做**排序 + 去重 + 大写**归一化，所以学生选 `CA` 和 `AC` 等价。
（这一条是被单元测试发现的：最初的实现漏了排序，`CA` 会被判错。）

### 6. 填空题的自动判分

填空题用字符串精确匹配（忽略大小写与首尾空格），教师在答案里用 `|` 分隔多个
可接受写法，例如 `北京|首都北京`。不做模糊匹配——同义词、全角标点这类规则
需要具体业务约定，硬编码进判分逻辑反而容易误判。

### 7. 删除策略

没有用逻辑删除，而是用「有约束才允许删」的规则，理由是这样数据模型更简单，
也不需要担心自定义 SQL 忘记过滤 `deleted` 标记：

- **删除课程**：已有学生选课时拒绝，提示改成「关闭考试」
- **删除题目**：允许。历史成绩已快照，不受影响
- **退选课程**：已有考试记录时拒绝，成绩要留档

### 8. 横向越权防护

教师只能操作自己的课程。每个写操作都过 `CourseService.requireOwnedCourse()`，
比对课程的 `teacher_id` 与当前登录用户。否则教师 A 只要改一下 URL 里的
`courseId` 就能改教师 B 的题库。端到端脚本里有一条专门验证这个。

---

## 八、文档中未明确、由本实现补充的部分

需求文档有几处没写清楚，以下是本实现的选择（都留了改动的口子）：

| 问题 | 本实现的选择 | 如何改成别的 |
|---|---|---|
| 没有提注册 | 提供 `POST /auth/register`，可注册任意角色 | 若要求「教师账号只能由已有教师创建」，改 `AuthService#register` 里的角色校验，注释已标注位置 |
| 没写学生怎么选课 | 提供选课/退选接口（`/student/courses/*/enroll`） | 若改为教师端指派，把接口挪到 `TeacherController` 即可 |
| 没写题型 | 实现单选/多选/判断/填空四种，全部可自动判分 | 加题型改 `QuestionType` + `QuestionService#normalizeAndValidate` + `GradingService` |
| 没写及格线 | 按满分 60%，`CourseStatsService.PASS_RATIO` | 改这一个常量 |
| 没写多次考试怎么展示分数 | 列表显示**最高分**：多次考试取最好的一次 | 改 `CourseEnrollmentMapper.xml` 里的 `MAX(s.score)` |
| 没写考试时长 | 挂在课程上（`course.duration_minutes`） | 如需按场次配置，加一列到 `exam_session` |

---

## 九、已知限制

诚实列一下，避免答辩时被问住：

1. **超时封卷不够严格** —— 见上文「关键设计说明 4」。
2. **`attempt_no` 有并发竞态** —— 用 `COUNT(*)+1` 生成，同一学生极端并发下可能重号。
   正常单端操作不会触发。
3. **考试期间教师改题会影响正在答题的学生** —— 试卷每次请求都从题库实时读取，
   没有做「开考时锁定试卷版本」。要严格的话应在开考时把题目 ID 列表快照进
   `exam_session`。
4. **没有防作弊** —— 无切屏检测、无随机组卷、无防复制。文档未要求。
5. **没有文件上传** —— 文档未要求，因此题目不支持图片。
6. **JWT 无法主动失效** —— 退出登录只是前端丢弃 token，服务端没有黑名单。
   需要的话可加 Redis 存吊销列表。
7. **`jwt.secret` 有开发默认值** —— 生产环境必须用环境变量 `JWT_SECRET` 覆盖。

---

## 十、本机环境的两个坑（已处理）

记录下来，换机器时可能会再遇到：

1. **`E:\apache-maven-3.9.14\conf\settings.xml` 是损坏的**。XML 注释被压平成一行，
   且所有开标签被写成 `-<tag>`（如 `-<settings>`），导致 Maven 完全无法启动，
   报 `Non-parseable settings`。已修复，原文件备份为 `settings.xml.bak-before-fix`。

   修复过程中还发现同一文件里另一处问题：`nexus-aliyun` 镜像被写在了 `</mirrors>`
   闭合标签**外面**，Maven 直接忽略它并在每次构建时打一行 `Unrecognised tag: 'mirror'`；
   同时 `al imaven` 镜像的 URL 里混进了空格（`http://maven. aliyun. com/...`），
   这个地址永远拉不到包。两处都已修好（备份为 `settings.xml.bak-before-mirrors-fix`）。
   之所以之前没暴露，是因为本地仓库已有全部依赖，根本没走网络。
2. **Spring 的 SQL 初始化脚本默认用平台编码读取**。中文 Windows 上是 GBK，
   会把 UTF-8 的种子数据读成乱码（表现为接口返回的姓名是「寮犱笁」）。
   已在 `application.yml` 里显式配置 `spring.sql.init.encoding: UTF-8`。

另外 `pom.xml` 里 MySQL 驱动用的是新坐标 `com.mysql:mysql-connector-j`：
Spring Boot 2.7 的 BOM 已不再管理旧坐标 `mysql:mysql-connector-java` 的版本，
沿用旧坐标会报 `'dependencies.dependency.version' is missing`。
