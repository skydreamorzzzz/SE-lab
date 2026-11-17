# 真实开发记录（非实验报告）

- 基线：实验一 main 的 5db534a；原工作区干净，与本地 origin/main 一致。使用 git worktree 创建 lab3-mvp，不改动原交付物。
- 阅读资料：requirements.md；实验二四种 PlantUML；六份 HTML 原型；教师实验三要求。
- 模块 1：Spring MVC/JPA/Thymeleaf/Security 工程基础、BCrypt 账户、角色与资料维护；JUnit 验证注册、权限入口与密码规则。
- UML 对应：User → Account；MarketplaceManager 按职责拆成 Service，Controller 仅做请求绑定。预约独立实体，避免将未成交预约混入订单。
- 环境问题：系统 PATH 无 Java/Maven，工具隔离放在工作目录 .tools，不提交二进制。
