# 拾物集 · 二手交易系统 MVP

这是实验三的运行与验收说明。开发事实见 DEVELOPMENT.md，不包含实验报告正文。

## 启动

需要 Java 21 和 Maven 3.6.3+。从本项目目录执行：

```powershell
cd E:\lab3\implementation\lab3
mvn test
mvn package
java -jar target/marketplace-1.0.0.jar
```

本机已隔离安装 JDK/Maven 到 `E:\lab3\.tools`，无需修改系统配置，可以使用：

```powershell
.\mvn-local.ps1 test
.\mvn-local.ps1 package
.\run-local.ps1
```

启动脚本会询问首次管理员用户名与密码，不写入文件。密码至少8位，包含大写、小写、数字、特殊字符。已有同名管理员不会被重设密码。其他环境也可在首次启动前设置 `MARKET_ADMIN_USER`、`MARKET_ADMIN_PASSWORD` 环境变量，启动后清除密码变量。没有预设密码。浏览器打开 http://localhost:8080 。

数据库保存在项目 `data/market.mv.db`，正常重启保留数据。更换运行目录会更换数据库相对位置；启动脚本始终使用项目目录。服务停止后可备份整个 data 目录。测试使用独立 H2 内存库，浏览器测试用 target 下独立文件库。

## 结构

- `src/main/java/edu/market`：Account/Product/PurchaseOrder/Reservation/Review 实体；Repository 数据访问；Service 权限与状态转换；Controller 页面绑定；SecurityConfig 登录和 CSRF。
- `src/main/resources/templates`：首页、登录、注册、商品详情、卖家编辑/管理、审核、订单、预约、个人中心、卖家评价、日志。
- `src/main/resources/static/css/market.css`：继承实验二主页 CSS，增加真实表单布局；sample.svg 为本地演示插图。
- `src/test/java`：业务、并发、事务回滚、页面与安全测试。
- `scripts/browser-smoke.cjs`：本机 Chrome 端到端验收；`scripts/count-source.py`：源码统计。

类文件放在同一业务包，依赖层次仍为 Controller → Service → Repository → Entity。没有微服务和独立前端。订单保存价格、商品名和地址快照，商品行锁串行化库存与预约变更；所有交易操作均在数据库事务中完成。

## 人工验收路径

1. 注册卖家，创建商品草稿。分类填 `数码电子/相机`，图片可填 `/images/sample.svg`；定价可把最低价和最高价设成一样。保存后在商品管理增加库存，再提交审核。
2. 管理员登录审核后台，通过商品；也可填写原因驳回，检查商品回到草稿。
3. 注册买家，在个人中心填写地址；搜索商品，下单、模拟支付。卖家在订单页模拟发货，买家确认收货，双方分别评价；商品详情和卖家主页应显示评价。
4. 库存售罄后，买家申请预约，重复申请会合并；取消释放额度，再次预约按新顺序排队。卖家补货后在预约队列确认已分配数量，生成买家待支付订单。小量补货先部分满足队首。
5. 试验越权、重复评价、库存不足；修改在售商品描述应进入待审核。未付款订单15分钟后自动取消（扫描周期30秒），取消释放库存；只有物流签收后满7天才自动确认收货。

## 测试

Checkstyle 绑定 Maven validate，test/package 自动执行。规则在 checkstyle.xml，检查命名、星号导入、必需大括号等；没有声称执行完整 Google 风格规则集。

浏览器回归需 Node.js、Playwright 和 Chrome，可使用：

```powershell
npm install --prefix target/browser playwright
$env:PLAYWRIGHT_MODULE = "$PWD/target/browser/node_modules/playwright"
node scripts/browser-smoke.cjs
```

可设置 `CHROME_PATH` 和 `JAVA_HOME`。脚本启动18080端口，随机生成验收账户与密码，执行交易、预约、权限和重启恢复检查；结果及截图写入 `target/browser-evidence`，不向远程提交数据库和账户。端口应保持空闲。

## 范围与限制

实现三角色账户、资料和改密；商品完整审核生命周期（违规治理与申诉除外）、库存与组合搜索；直接下单和本地支付/物流模拟；预约排队和取消；双向评价及卖家信誉；关键操作日志。

预约重复合并遵循实验一 REQ-012，不创建第二条有效预约。区间价在当前 MVP 以公开最低价成交；未实现议价，页面对此有说明。媒体采用图片地址引用，暂不提供文件上传、多图和视频。角色在注册时固定，管理员通过部署配置建立。

未实现：退款、议价、购物车、收藏、举报与申诉、封号、超级管理员/抽成结算、密码找回/手机验证、榜单统计。未做实验一1万商品性能指标验收。支付与物流均为本地模拟，无真实资金。

实验一原始需求和实验二原型未修改。实验三分支继承原实验一 Git 历史。不要将本说明或开发记录直接当作实验报告；教师要求报告由本人独立撰写。
