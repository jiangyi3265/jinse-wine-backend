# jinse-wine-backend

金色酒业系统的 Java 后端基础工程，为管理后台提供系统管理 API，并预留小程序业务扩展位置。

## 项目简介

本项目基于 RuoYi 3.9.1，当前代码包含验证码登录、JWT 认证、用户与角色、部门与岗位、菜单权限、字典与参数、通知公告、个人资料、操作与登录日志、在线用户、服务器与缓存监控、文件上传等基础能力。

酒类商品、订单、支付、优惠券、积分、红包及门店兑奖等专属业务接口尚未实现；小程序当前使用本地演示数据。仓库中的 `ruoyi-quartz`、`ruoyi-generator` 保留源码，但已在根 POM 和启动模块依赖中停用，不属于当前启动服务的已启用功能。

## 技术栈

- Java 8 编译目标、Maven 多模块工程。
- Spring Boot 2.5.15、Spring Security、JWT。
- MyBatis（XML Mapper）、PageHelper 1.4.7；不是 MyBatis Plus。
- MySQL、Druid 1.2.27、Redis / Lettuce。
- Springfox Swagger 3、Bean Validation、Apache POI、Logback。

## 关联仓库

| 项目 | 说明 | GitHub |
| --- | --- | --- |
| jinse-wine-backend | 后端服务 | [jinse-wine-backend](https://github.com/jiangyi3265/jinse-wine-backend) |
| jinse-wine-admin | 管理后台 | [jinse-wine-admin](https://github.com/jiangyi3265/jinse-wine-admin) |
| jinse-wine-app | 用户端 | [jinse-wine-app](https://github.com/jiangyi3265/jinse-wine-app) |

管理后台对接本服务的现有系统接口；小程序的业务 API 计划在本服务中扩展，当前尚未联调。

## 快速启动

准备 JDK 8 或兼容 JDK、Maven 3、MySQL 和 Redis。先在本机建立一个新的空数据库，再导入 `sql/init.example.sql`；该示例含建表与基础字典数据，会重建同名表，请勿用于已有业务数据库。

公开 SQL 不含账号口令、密码哈希、个人联系电话或邮箱。首次登录前，在本机通过 `SecurityUtils.encryptPassword` 生成自己选定密码的 BCrypt 哈希，并写入 `sys_user` 中 `admin` 的 `password` 字段；没有可直接使用的默认密码。新建用户时也需显式设置密码。

将以下两个示例复制到 `ruoyi-admin/src/main/resources/` 对应文件名（仅首次克隆时执行，不覆盖已有本地配置）：

```powershell
Copy-Item config/examples/application.yml ruoyi-admin/src/main/resources/application.yml
Copy-Item config/examples/application-druid.yml ruoyi-admin/src/main/resources/application-druid.yml
```

通过本机环境变量或被忽略的本地配置设置：

| 变量 | 用途 |
| --- | --- |
| `DB_URL` | JDBC 连接地址，例如本机 MySQL 的 `jdbc:mysql://localhost:3306/jinse_wine?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai` |
| `DB_USERNAME`、`DB_PASSWORD` | 本地数据库账号及口令 |
| `REDIS_HOST`、`REDIS_PASSWORD` | Redis 地址及口令，示例默认 localhost，口令可空 |
| `JWT_SECRET` | 自行生成的高强度签名密钥，必须设置，无固定默认值 |
| `UPLOAD_PATH` | 上传目录，默认 `./uploads` |

从仓库根目录构建并启动：

```bash
mvn clean package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

默认服务地址 `http://localhost:8080`。登录与菜单需要数据库、Redis 正常可用；示例未填充完整业务菜单，可在数据库中按实际权限配置。Druid 管理页面在公开示例中默认关闭。不要将数据库口令和 JWT 密钥提交到仓库。

## 项目结构

```text
ruoyi-admin/       启动入口、系统及监控 Controller
ruoyi-framework/   安全认证、Web 配置、数据源与缓存
ruoyi-system/      系统业务 Service、Domain、Mapper
ruoyi-common/      公共实体、注解、工具、异常处理
ruoyi-quartz/      定时任务源码，当前未启用
ruoyi-generator/   代码生成源码，当前未启用
config/examples/  无真实凭据的服务配置示例
sql/              脱敏初始化示例，原始本地 SQL 不入库
```

## 简历描述示例

参与金色酒业后端基础工程整理，基于 Spring Boot、Spring Security、MyBatis 和 Redis 梳理用户、角色、菜单权限、日志及文件服务，为管理端对接与小程序业务扩展建立工程基础。

以上为基于现有代码的表述示例，请按个人实际贡献调整，不应描述为已完成支付、红包或订单业务开发。

## 配置与许可

本地 `application*.yml`、原始数据库 SQL、IDE 配置、日志、构建包及本地环境使用手册不会上传。公开配置与 SQL 位于示例目录。保留上游 RuoYi 的 MIT 许可与版权声明，见 [LICENSE](LICENSE)。
