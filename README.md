# 东软颐养中心 - 智慧养老服务管理系统

## 📦 项目结构

```
东软颐养中心项目代码/
├── backend/           # Spring Boot 后端
│   ├── src/          # Java 源码
│   ├── pom.xml       # Maven 配置
│   └── target/       # 编译产物（可选）
├── frontend/          # Vue 3 前端
│   ├── src/          # Vue 源码
│   ├── public/       # 静态资源
│   ├── package.json  # npm 配置
│   └── vue.config.js # Vue 配置
├── database/         # 数据库脚本
│   └── database.sql  # 建表及初始化脚本（包含示例数据）
└── README.md         # 本文件
```

## ⚠️ 开始之前请确认

请先确认你电脑上已经安装好以下软件（如果没有，请先下载安装）：

| 工具 | 版本要求 | 下载地址 | 安装验证命令 |
|------|----------|----------|-------------|
| JDK | **17 或更高** | https://www.oracle.com/java/ 或 https://adoptium.net/ | `java -version` |
| Maven | **3.8+** | https://maven.apache.org/ | `mvn -v` |
| Node.js | **18+**（推荐 18 LTS 或 20 LTS） | https://nodejs.org/ | `node -v` |
| npm | 9+（随 Node.js 自带） | — | `npm -v` |
| MySQL | **8.0+** | https://dev.mysql.com/downloads/ | `mysql -V` |
| Git（可选） | 任意 | https://git-scm.com/ | `git --version` |

> ⚠️ **重要**：JDK 必须是 17+，不能用 JDK 8/11。Node.js 必须是 18+。

## 🚀 完整部署步骤

### 第一步：解压并进入项目

将压缩包 `东软颐养中心_项目源码.zip` 解压到一个**没有中文、没有空格**的目录下（例如 `D:\projects\eldercare`）。

### 第二步：初始化数据库

**方式 A：使用 Navicat（推荐）**
1. 打开 Navicat，连接到本地 MySQL（用户名 `root`，密码 `123456`）
2. 右键连接名 → 运行 SQL 文件 → 选择 `database/database.sql`
3. 刷新后会看到 `eldercare` 数据库和所有表

**方式 B：使用命令行**
```bash
# 进入项目根目录
cd 东软颐养中心项目代码

# 登录 MySQL 并执行脚本
mysql -u root -p123456 < database/database.sql
```

> ⚠️ 如果你的 MySQL root 密码不是 `123456`，请修改 [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml) 中的 `username` 和 `password`：
> ```yaml
> spring:
>   datasource:
>     username: root   # 改成你的用户名
>     password: 123456 # 改成你的密码
> ```

### 第三步：启动后端

```bash
cd backend

# 编译并打包（第一次运行需要下载依赖，可能需要几分钟）
mvn clean package -DskipTests

# 运行 jar 包
java -jar target/eldercare-1.0.0.jar
```

**看到这行表示启动成功：**
```
Started EldercareApplication in X.XX seconds
```

后端接口地址：`http://localhost:8080`
接口文档：`http://localhost:8080/swagger-ui.html`

> 💡 **开发模式运行（修改了 Java 代码时用这个更快）**：
> ```bash
> mvn spring-boot:run
> ```

### 第四步：启动前端

打开**另一个**终端窗口（保持后端运行）：

```bash
cd frontend

# 安装依赖（第一次运行需要下载，可能需要几分钟）
npm install

# 启动开发服务器
npm run serve
```

**看到这行表示启动成功：**
```
App running at:
  - Local:   http://localhost:5173/
```

浏览器访问：`http://localhost:5173/`

> 💡 如果 `npm install` 很慢，可以使用国内镜像：
> ```bash
> npm config set registry https://registry.npmmirror.com
> npm install
> ```

### 第五步：登录系统

打开浏览器访问 `http://localhost:5173/`，使用以下账号登录：

#### 管理员账号
| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin | 系统管理员 |
| admin1 | admin1 | 系统管理员 |
| admin2 | admin2 | 系统管理员 |

#### 健康管家账号
| 用户名 | 密码 | 角色 |
|--------|------|------|
| manager01 | 123456 | 健康管家张宁 |
| manager02 | 123456 | 健康管家李芳 |
| manager03 | 123456 | 健康管家王强 |

## 📱 功能模块一览

### 管理员端
- 📊 工作台数据概览
- 👥 客户入住登记与管理
- 🛏️ 床位示意图查看（4 楼，含套房/单人间/双人间/多人间）
- 📋 床位使用记录
- 💊 护理项目管理
- 📈 护理级别配置
- 🎫 客户护理服务设置
- 📝 护理记录查看
- 🚪 外出/退住审批
- 👩‍⚕️ 健康管家管理
- ⚙️ 系统用户管理

### 健康管家端
- 👤 我的服务对象
- 🩺 日常护理执行
- 🚶 外出申请
- ↩️ 退住申请

## 🗄️ 数据库表结构

| 表名 | 说明 |
|------|------|
| sys_user | 系统用户表（管理员 + 健康管家） |
| room | 房间信息表（含楼栋、楼层、房型、面积、容量） |
| bed | 床位信息表 |
| customer | 客户信息表 |
| bed_usage | 床位使用记录 |
| care_level | 护理级别表 |
| care_item | 护理项目表 |
| care_level_item | 级别-项目关联表 |
| customer_service | 客户护理服务表 |
| nursing_record | 护理记录表 |
| outing_request | 外出申请表 |
| checkout_request | 退住申请表 |

## 🏢 房间类型说明

| 类型标识 | 类型名称 | 面积 | 床位数 | 说明 |
|----------|----------|------|--------|------|
| SINGLE | 单人间 | 25㎡ | 1 张 | 独立卫浴 |
| DOUBLE | 双人间 | 35㎡ | 2 张 | 标准配置 |
| MULTI | 多人间 | 55㎡ | 4 张 | 经济型 |
| APARTMENT | 套房 | 85-95㎡ | 2-3 张 | 高端配置 |

## 🛠️ 常见问题排查

### Q1：`mvn` 命令找不到？
A：请确认 Maven 已安装并配置环境变量。Windows 用户检查 `M2_HOME` 和 `Path` 是否添加了 Maven 的 `bin` 目录。

### Q2：`npm install` 很慢或超时？
A：使用国内镜像：`npm config set registry https://registry.npmmirror.com`

### Q3：后端启动报数据库连接失败？
A：
1. 确认 MySQL 服务已启动
2. 确认账号密码与 `application.yml` 中一致
3. 确认 `eldercare` 数据库已通过 `database.sql` 创建

### Q4：前端请求接口报 404？
A：确保后端已启动在 8080 端口。开发模式下前端会自动代理 `/api` 请求。

### Q5：页面空白或白屏？
A：
1. 打开浏览器开发者工具（F12）查看 Console 是否有报错
2. 确认后端正常运行
3. 尝试清除浏览器缓存

### Q6：编译报错找不到 Lombok？
A：执行 `mvn clean install` 重新编译，或安装 IDE 的 Lombok 插件。

### Q7：端口被占用（8080 或 5173）？
A：
- 后端修改 [application.yml](backend/src/main/resources/application.yml) 中的 `server.port`
- 前端修改 [vue.config.js](frontend/vue.config.js) 中的 `devServer.port`

### Q8：忘记密码怎么办？
A：使用管理员账号登录后，在"系统用户管理"中点击"重置密码"按钮（密码会被重置为该用户手机号后 6 位）。

## 📞 技术栈

- **后端**：Spring Boot 3.3.1 + MyBatis-Plus 3.5.7
- **前端**：Vue 3.4 + Element Plus 2.7
- **数据库**：MySQL 8.0
- **认证**：JWT (jjwt 0.12.5)
- **构建**：Maven + npm

