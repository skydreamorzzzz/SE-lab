# 真实开发记录（非实验报告）

- 基线：实验一 main 的 5db534a；原工作区干净，与本地 origin/main 一致。使用 git worktree 创建 lab3-mvp，不改动原交付物。
- 阅读资料：requirements.md；实验二四种 PlantUML；六份 HTML 原型；教师实验三要求。
- 模块 1：Spring MVC/JPA/Thymeleaf/Security 工程基础、BCrypt 账户、角色与资料维护；JUnit 验证注册、权限入口与密码规则。
- UML 对应：User → Account；MarketplaceManager 按职责拆成 Service，Controller 仅做请求绑定。预约独立实体，避免将未成交预约混入订单。
- 环境问题：系统 PATH 无 Java/Maven，工具隔离放在工作目录 .tools，不提交二进制。

- 模块 2：商品草稿、两级分类、价格区间、审核驳回、关键字段复审和公开搜索；ProductTest 2 项通过，Checkstyle 通过。关键操作日志与业务事务一起提交，不含地址或密码。
- 交易预验证：MarketplaceTest 9 项、ConcurrencyTest 1 项通过；最后一件商品双线程抢购仅一笔成功，注入异常验证订单与库存回滚。

- 模块 3：事务订单、幂等下单、15 分钟支付释放、模拟物流签收与 7 天确认；预约合并/取消/先到先得部分分配/卖家确认；双向一次评价。重跑交易与并发测试 10 项通过。锁顺序统一为商品→订单，先查询对象 ID，避免锁前读入旧状态。
