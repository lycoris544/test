# 接口文档

**Base URL**：`http://localhost:8080/api`

**认证**：除登录/注册外，所有接口都需要在请求头带上 token：

```
Authorization: Bearer <token>
```

**统一响应体**：

```json
{ "code": 0, "message": "success", "data": { } }
```

- `code == 0` 表示成功，其余为失败，失败时 `message` 是可直接展示给用户的中文提示
- 业务失败返回 **HTTP 200** + `code != 0`；只有鉴权失败（未登录 401 / 越权 403）才用非 200 状态码
- 响应中值为 `null` 的字段会被省略（Jackson `non_null` 配置），前端取值时请做好缺字段判断

**分页响应**：

```json
{ "code": 0, "data": { "total": 25, "current": 1, "size": 10, "pages": 3, "records": [ ] } }
```

**枚举取值**：

| 枚举 | 取值 |
|---|---|
| 角色 role | `1` 教师 / `2` 学生 |
| 题型 type | `1` 单选 / `2` 多选 / `3` 判断 / `4` 填空 |
| 考试状态 status | `0` 进行中 / `1` 已交卷 / `2` 超时交卷 / `-1` 未参加（仅统计用，不入库） |
| 性别 gender | `1` 男 / `2` 女 |
| 是否开放考试 examEnabled | `0` 关闭 / `1` 开放 |

---

## 一、认证

### 1.1 登录

`POST /auth/login` —  无需 token

教师和学生共用同一入口，靠返回体里的 `role` 决定前端跳转到哪个界面。

**请求**

```json
{ "username": "student_zhang", "password": "123456" }
```

**响应**

```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 3,
    "username": "student_zhang",
    "realName": "张三",
    "role": 2,
    "roleName": "学生",
    "expiresIn": 86400000
  }
}
```

**失败情况**：用户名或密码错误 → `code=401`；账号被禁用 → `code=403`。
（用户名不存在与密码错误返回同一句提示，避免被用来枚举账号。）

### 1.2 注册

`POST /auth/register` — 无需 token

**请求**

```json
{
  "username": "student_new",
  "password": "123456",
  "realName": "王五",
  "role": 2,
  "userNo": "20230009",
  "phone": "13900000009",
  "gender": 1,
  "department": "计算机学院",
  "title": null
}
```

**响应**：`{ "code": 0, "message": "注册成功", "data": { "userId": 6 } }`

**失败情况**：用户名已存在 / 工号学号已被注册 / 角色取值不正确。

---

## 二、个人信息（教师端 + 学生端共用）

### 2.1 查看个人信息

`GET /profile`

**教师响应**（`teachingCourses` 即文档里的「教授课程」）

```json
{
  "code": 0,
  "data": {
    "id": 1, "username": "teacher_li", "realName": "李明",
    "gender": 1, "phone": "13800000001", "userNo": "T2001",
    "department": "计算机学院", "title": "教授",
    "role": 1, "roleName": "教师",
    "teachingCourses": [
      { "id": 1, "name": "Java 程序设计", "code": "CS101", "questionCount": 5, "totalScore": 50 }
    ]
  }
}
```

**学生响应**（`enrolledCourses` 即文档里的「所选课程」）

```json
{
  "code": 0,
  "data": {
    "id": 3, "username": "student_zhang", "realName": "张三",
    "gender": 1, "phone": "13900000001", "userNo": "20230001",
    "department": "计算机学院",
    "role": 2, "roleName": "学生",
    "enrolledCourses": [
      { "id": 1, "name": "Java 程序设计", "code": "CS101" }
    ]
  }
}
```

### 2.2 修改个人信息

`PUT /profile`

只允许改资料字段。**用户名、工号/学号、角色不在请求体里** —— 它们是账号标识，
改了会牵连登录和成绩归属。

**请求**（字段都可选，只传要改的）

```json
{ "realName": "张三", "gender": 1, "phone": "13900000009", "department": "信息学院", "title": null }
```

**响应**：返回保存后的完整个人信息，前端可直接刷新页面。`title` 仅教师有效，学生传了会被忽略。

---

## 三、教师端

> 全部以 `/teacher` 开头，仅 `role=1` 可访问。
> 所有课程相关接口都会校验「课程是否属于当前教师」，不属于则返回 `code=403`。

### 3.1 我的课程

`GET /teacher/courses`

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| current | int | 否 | 1 | 页码 |
| size | int | 否 | 10 | 每页条数，上限 200 |
| keyword | string | 否 | | 按课程名或编码模糊搜索 |
| examEnabled | int | 否 | | `1` 只看已开放，`0` 只看已关闭 |

**响应记录字段**：`id`、`name`、`code`、`description`、`durationMinutes`、
`examEnabled`、`teacherName`、`questionCount`（题目数）、`totalScore`（卷面总分）

### 3.2 课程详情

`GET /teacher/courses/{courseId}` → 单条课程对象（字段同上）

### 3.3 新建课程

`POST /teacher/courses`

```json
{
  "name": "数据结构",
  "code": "CS301",
  "description": "线性表、树与图",
  "durationMinutes": 90,
  "examEnabled": 0
}
```

响应：`{ "code": 0, "data": { "courseId": 4 } }`

失败：课程编码已存在。**授课教师自动取当前登录教师**，不需要（也不能）在请求体里指定。

### 3.4 修改课程

`PUT /teacher/courses/{courseId}` — 请求体同新建

### 3.5 开放 / 关闭考试

`PUT /teacher/courses/{courseId}/exam-enabled?examEnabled=1`

学生只有在开放状态下才能开考。

### 3.6 删除课程

`DELETE /teacher/courses/{courseId}`

已有学生选课时会拒绝，提示改用「关闭考试」。**不做级联删除** —— 成绩是考核结果，
不能因为删课程就消失。

### 3.7 课程选课名单

`GET /teacher/courses/{courseId}/students`

**响应记录字段**：`studentId`、`studentName`、`studentNo`、`department`、
`finishedCount`（该生已考次数）、`lastScore`（最高分）

---

### 3.8 题目：分页查询

`GET /teacher/courses/{courseId}/questions`

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| current | int | 否 | 1 | |
| size | int | 否 | 20 | |
| type | int | 否 | | 按题型过滤 |
| keyword | string | 否 | | 按题干模糊搜索 |

**响应记录**：完整题目对象，**含 `answer` 和 `analysis`**（教师端需要）。

```json
{
  "id": 1, "courseId": 1, "type": 2, "content": "以下哪些属于 Java 的基本数据类型？",
  "options": [ { "key": "A", "content": "int" }, { "key": "B", "content": "String" } ],
  "answer": "ACD", "score": 10, "analysis": "String 是引用类型……", "sortOrder": 2
}
```

### 3.9 题目：全部（不分页）

`GET /teacher/courses/{courseId}/questions/all` → 题目数组，用于组卷预览

### 3.10 题目：详情

`GET /teacher/questions/{questionId}` → 单条题目对象

### 3.11 题目：新增

`POST /teacher/courses/{courseId}/questions`

响应：`{ "code": 0, "data": { "questionId": 13 } }`

**各题型的字段要求**：

| 题型 | `options` | `answer` |
|---|---|---|
| `1` 单选 | 至少 2 个，`key` 不可重复 | 单个选项 key，如 `"B"`（小写会自动转大写） |
| `2` 多选 | 至少 2 个 | 至少 2 个 key，如 `"ACD"`（顺序无所谓，入库前会排序） |
| `3` 判断 | 不传则自动补 `A=正确` / `B=错误` | `"A"` 或 `"B"` |
| `4` 填空 | 忽略，不需要传 | 答案原文；多个可接受写法用 `\|` 分隔，如 `"北京\|首都北京"` |

**请求示例（多选）**

```json
{
  "type": 2,
  "content": "以下哪些属于 Java 的基本数据类型？",
  "options": [
    { "key": "A", "content": "int" },
    { "key": "B", "content": "String" },
    { "key": "C", "content": "boolean" },
    { "key": "D", "content": "double" }
  ],
  "answer": "ACD",
  "score": 10,
  "analysis": "String 是引用类型，不是基本数据类型。",
  "sortOrder": null
}
```

`sortOrder` 不传则自动排到该课程最后。

**失败提示**（都有明确中文说明）：

- `单选题只能有一个正确答案`
- `多选题至少要有两个正确答案，若只有一个请改用单选题`
- `答案 C 不在选项范围内`
- `选项标识重复：A`
- `选择题至少需要 2 个选项`
- `判断题答案只能是 A（正确）或 B（错误）`

### 3.12 题目：修改

`PUT /teacher/questions/{questionId}` — 请求体同新增

### 3.13 题目：删除

`DELETE /teacher/questions/{questionId}`

已交卷的历史成绩不受影响（判分结果已快照进 `answer` 表）。

---

### 3.14 考试情况：所有考生成绩

`GET /teacher/scores`

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| current | int | 否 | 1 | |
| size | int | 否 | 10 | |
| courseId | long | 否 | | 按课程过滤 |
| keyword | string | 否 | | 按学生姓名或学号搜索 |

只返回**已结束**的考试（`status != 0`）——进行中的还没有成绩，计入会让平均分失真。
且只能看到当前教师自己课程的成绩。

**响应记录字段**：`id`（场次 ID）、`studentName`、`studentNo`、`department`、
`courseName`、`courseCode`、`attemptNo`、`status`、`score`、`totalScore`、
`startTime`、`submitTime`

### 3.15 考试情况：课程统计

`GET /teacher/courses/{courseId}/stats`

```json
{
  "code": 0,
  "data": {
    "courseId": 1, "courseName": "Java 程序设计", "courseCode": "CS101",
    "totalScore": 50,
    "studentCount": 3, "examinedCount": 1, "notExaminedCount": 2, "attemptCount": 1,
    "averageScore": 40.0, "highestScore": 40, "lowestScore": 40, "passRate": 100.0,
    "students": [
      { "studentId": 3, "studentName": "张三", "studentNo": "20230001",
        "attemptNo": 1, "status": 1, "score": 40, "totalScore": 50,
        "submitTime": "2026-09-12T12:46:16.852" },
      { "studentId": 4, "studentName": "李四", "studentNo": "20230002",
        "attemptNo": 0, "status": -1 }
    ]
  }
}
```

- `students` 里每人一行，取**最新一次**成绩
- **没参加过考试的学生也会出现**，`status = -1`、无 `score` 字段，
  教师能看到谁还没考
- 及格线按满分 60% 计算
- 无人考试时 `averageScore` / `highestScore` / `lowestScore` / `passRate` 均为 `null`（字段被省略）

### 3.16 考试情况：某考生该课程历次成绩

`GET /teacher/students/{studentId}/courses/{courseId}/attempts`

对应文档「点击进入某考生的该课程考试情况，若该课程重考多次，展示每一次考试的成绩」。

```json
{
  "code": 0,
  "data": {
    "studentId": 3, "studentName": "张三", "studentNo": "20230001", "department": "计算机学院",
    "courseId": 1, "courseName": "Java 程序设计", "courseCode": "CS101",
    "attemptCount": 2, "bestScore": 40, "totalScore": 50,
    "attempts": [
      { "attemptNo": 1, "status": 1, "score": 40, "totalScore": 50,
        "startTime": "...", "submitTime": "..." },
      { "attemptNo": 2, "status": 0, "startTime": "..." }
    ]
  }
}
```

`attempts` 按次数升序，进行中的场次 `status = 0` 且没有 `score`。

### 3.17 某场考试的逐题作答明细

`GET /teacher/sessions/{sessionId}/answers` → 作答明细数组（含题干、学生答案、正确答案、解析）

---

## 四、学生端

> 全部以 `/student` 开头，仅 `role=2` 可访问。
> 学生只能操作自己的数据，接口内部一律以当前登录用户为准，不接受传入的 studentId。

### 4.1 我的课程（含考试状态）

`GET /student/courses`

文档要求：「课程列表会保存考试状态。若已参加过该课程考试，旁边显示分数」。

```json
{
  "code": 0,
  "data": [
    {
      "courseId": 1, "courseName": "Java 程序设计", "courseCode": "CS101",
      "teacherName": "李明", "durationMinutes": 60, "examEnabled": 1,
      "questionCount": 5, "totalScore": 50,
      "examStatus": 1, "lastScore": 40, "attemptCount": 2,
      "ongoingCount": 1, "finishedCount": 1, "canStart": true
    }
  ]
}
```

| 字段 | 含义 |
|---|---|
| `lastScore` | 展示用分数，取已结束考试中的**最高分**；没考过则省略该字段 |
| `attemptCount` | 该课程的总作答次数（含重考） |
| `ongoingCount` | 开考后未交卷的记录数 |
| `finishedCount` | 已交卷的记录数 |
| `examStatus` | 最近一次已结束考试的状态；只有进行中的则显示 `0` |
| `canStart` | 见下表 |

`canStart` 的取值规则：

| 情形 | `canStart` | 前端应表现 |
|---|---|---|
| 从未考过 | `true` | 显示「开始考试」 |
| 有进行中的考试 | `true` | 显示「继续考试」 |
| 已考过且无进行中记录 | `false` | 显示分数 + 「重考」按钮 |

### 4.2 可选课程

`GET /student/courses/available?keyword=` → 尚未选的课程数组

### 4.3 选课

`POST /student/courses/{courseId}/enroll` → `{ "code": 0, "message": "选课成功" }`

失败：课程不存在 / 你已选择该课程。

### 4.4 退选

`DELETE /student/courses/{courseId}/enroll`

已有考试记录的课程会拒绝退选（成绩要留档）。

---

### 4.5 开始考试

`POST /student/courses/{courseId}/exam/start?restart=false`

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| restart | boolean | 否 | false | 是否重考 |

**行为**：

1. 已有进行中的考试 → **返回该场考试继续作答**（无论 `restart` 是什么），
   对应文档「中途退出网页后重进仍可返回考试」
2. 已考过且 `restart=false` → 拒绝，提示「该课程已考过，如需重新考试请点击「重考」」
3. 已考过且 `restart=true` → 开新场次，`attemptNo` 递增
4. 其他情况 → 开新场次

**响应**

```json
{
  "code": 0,
  "data": {
    "sessionId": 1, "courseId": 1, "courseName": "Java 程序设计", "courseCode": "CS101",
    "attemptNo": 1, "totalScore": 50, "questionCount": 5,
    "startTime": "2026-09-12T12:45:53.477",
    "deadline": "2026-09-12T13:45:53.477",
    "remainingSeconds": 3599,
    "questions": [
      {
        "id": 1, "number": 1, "type": 1, "typeName": "单选题",
        "content": "下列哪个关键字用于在 Java 中定义常量？",
        "options": [ { "key": "A", "content": "static" }, { "key": "B", "content": "final" } ],
        "score": 10
      }
    ]
  }
}
```

> **注意**：`questions` 里**没有 `answer` 和 `analysis` 字段**，这是刻意的 —— 否则学生
> 按 F12 就能看到答案。答题用的题目结构不含答案，交卷后才下发。

**开考前的校验**：课程存在 → 已选该课 → 考试已开放 → 课程有题目，任一不满足都会拒绝。

### 4.6 恢复进行中的答卷

`GET /student/courses/{courseId}/exam/current`

页面刷新后调用，返回结构同 4.5。没有进行中的考试时返回「当前没有进行中的考试」。

### 4.7 交卷

`POST /student/exam/sessions/{sessionId}/submit`

**请求**

```json
{
  "answers": [
    { "questionId": 1, "userAnswer": "B" },
    { "questionId": 2, "userAnswer": "AC" },
    { "questionId": 3, "userAnswer": "B" },
    { "questionId": 4, "userAnswer": "finally" }
  ]
}
```

- 未作答的题目可以不传，服务端按 0 分处理
- 多选题答案顺序无所谓（`CA` 与 `AC` 等价），也容错 `A,C` / `A C` 这类写法
- 同一题重复出现时保留第一条

**响应**（判分后直接返回，前端拿到即可展示成绩页）

```json
{
  "code": 0,
  "message": "交卷成功",
  "data": {
    "sessionId": 1, "courseId": 1, "courseName": "Java 程序设计", "attemptNo": 1,
    "score": 40, "totalScore": 50, "correctCount": 4, "questionCount": 5,
    "accuracy": 80.0,
    "startTime": "2026-09-12T12:45:53.477", "submitTime": "2026-09-12T12:46:16.852",
    "status": 1, "statusName": "已交卷", "timeout": false,
    "items": [
      {
        "questionId": 1, "number": 1, "type": 1, "typeName": "单选题",
        "content": "下列哪个关键字用于在 Java 中定义常量？",
        "options": [ { "key": "A", "content": "static" }, { "key": "B", "content": "final" } ],
        "userAnswer": "B", "userAnswerText": "B. final",
        "correctAnswer": "B", "correctAnswerText": "B. final",
        "correct": true, "score": 10, "fullScore": 10,
        "analysis": "final 修饰的变量只能赋值一次……"
      },
      {
        "questionId": 2, "number": 2, "type": 2, "typeName": "多选题",
        "userAnswer": "AC", "correctAnswer": "ACD",
        "correct": false, "score": 0, "fullScore": 10
      }
    ]
  }
}
```

- `userAnswerText` / `correctAnswerText` 把选项 key 翻译成了可读文本（如 `"A. int；C. boolean"`），
  前端可以直接展示，不用自己查选项
- `timeout = true` 表示**超过截止时间交卷**，前端应给出明确提示
- 题目若已被教师删除，`questionDeleted = true`，题干为空但得分和作答仍在

**失败情况**：考试记录不存在 / 无权操作他人记录 / **该场考试已交卷，不能重复提交**。

### 4.8 查看某场考试成绩详情

`GET /student/exam/sessions/{sessionId}/result` → 结构同 4.7 的响应

用于交卷后重新进入查看，或从历史记录点进某一场。未交卷的场次会拒绝访问。

### 4.9 历史考试记录

`GET /student/exam/history?current=1&size=10&courseId=`

包含进行中的记录（`status = 0`），学生能在列表里看到未完成的考试并继续作答。
按时间倒序。

### 4.10 某门课的历次考试

`GET /student/courses/{courseId}/attempts` → 场次数组，按次数升序

---

## 五、常见错误码

| code | HTTP | 含义 | 典型场景 |
|---|---|---|---|
| 0 | 200 | 成功 | |
| 400 | 200 | 参数或业务规则不满足 | 题目校验失败、重复交卷、未开放考试 |
| 401 | 401 | 未登录 / token 失效 / 密码错误 | 未带 token、token 过期 |
| 403 | 403 | 无权访问 | 学生访问教师接口、教师操作他人课程、未选课就开考 |
| 404 | 200 | 资源不存在 | 课程 / 题目 / 考试记录不存在 |
| 500 | 200 | 服务端异常 | 已记录完整堆栈，响应只给通用提示 |

鉴权失败（401/403）用真实 HTTP 状态码返回，其余业务错误统一 HTTP 200 + `code != 0`，
前端拦截器按 HTTP 状态码处理登录跳转即可。
