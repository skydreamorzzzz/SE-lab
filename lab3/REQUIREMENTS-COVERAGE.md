# 需求对应与验证记录

基准为原实验一 requirements.md，未修改原需求。本表记录当前实现与验证事实，不是实验报告正文。外部资金、物流与短信均按课程本地模拟口径，见 README。

| 需求 | 当前实现/入口 | 对应证据 |
|---|---|---|
| REQ-001 | 注册、登录、资料、改密；身份验证页绑定手机号与恢复凭证找回密码 | AccountTest、IdentityMediaTest、Chrome；短信为显式本地模拟 |
| REQ-002 | 普通账号派生BUYER+SELLER双能力（原型普通用户可买卖），ADMIN独立；超级管理员为ADMIN权限属性；普通管理员不可改规则和他人商品 | FinanceTest、AuthorizationTest |
| REQ-003 | 不完整草稿可保存，仅所属卖家可查看，管理员也不能读取草稿 | ProductTest；ProductService.detail |
| REQ-004 | 主图、附图、一个视频、两级分类、成色、价格区间、库存及可选信息 | IdentityMediaTest；Chrome媒体发布与可选字段 |
| REQ-005 | 审核通过/理由驳回到草稿，管理员不能修改正文 | ProductTest、Chrome |
| REQ-006 | 六种商品状态；下架再提审，违规申诉后回待审核，售罄与补货受控 | ProductTest、ModerationTest、Chrome |
| REQ-007 | 关键内容/媒体/可选信息变更复审，库存单独调整 | ProductTest；ProductService.save |
| REQ-008 | 库存0不能买，允许有计划的缺货预约；补货不恢复违规/普通下架商品 | MarketplaceTest |
| REQ-009 | 关键词（名称/品牌/型号）、两级分类、价格、成色、真实距离、卖家累计销量/好评率组合查询及分页 | MarketplaceTest、PersonalTest、PerformanceTest |
| REQ-010 | 价格/时间/收藏/销量/信誉/成色稳定排序；全站及分类日周月成交榜 | InsightsTest；Chrome榜单；RankingService |
| REQ-011 | 加购/更新/删除、检索/分组、重新核验失效原因，同直接购买服务下单 | PersonalTest、Chrome购物车结算 |
| REQ-012 | 同一买家同商品有效预约唯一并合并，计划总量上限、取消后重新排队 | MarketplaceTest、ConcurrencyTest |
| REQ-013 | 补货FIFO部分满足队首，分配后库存不被普通购买抢占 | MarketplaceTest、Chrome |
| REQ-014 | 区间报价、卖家接受锁价及库存15分钟，转单不重复扣减，超时释放 | BargainTest、Chrome |
| REQ-015 | 直接/购物车/议价/预约下单；快照、请求幂等、15分钟未付取消 | MarketplaceTest、BargainTest、PersonalTest、Chrome |
| REQ-016 | 详情30秒刷新，购买前客户端重新查询，服务端锁内检查价格/状态/库存 | ConcurrencyTest、InsightsTest；lab2-detail.js |
| REQ-017 | 模拟支付、发货、买家收货，非法状态与越权拒绝 | MarketplaceTest、Chrome |
| REQ-018 | 仅物流签收后7天自动确认；无签收不触发 | MarketplaceTest.autoReceiptRequiresSevenDaysAfterSigned |
| REQ-019 | 收货后结算，按超级管理员抽成保存快照，售后期间不新增结算 | FinanceTest；Chrome促销结算 |
| REQ-020 | 未发货退款、已发货/已收货退货退款、拒绝恢复履约、全额退款回库存，结算后退款冲正 | FinanceTest、Chrome两种退款 |
| REQ-021 | 48小时未处理自动同意；退货仍需物流和卖家收货；重复定时不重复退款 | FinanceTest.returnRefundTimeoutRequiresPhysicalReturn |
| REQ-022 | 双方独立、每方一次，真实订单权限和评分范围；卖家信誉展示 | MarketplaceTest、Chrome |
| REQ-023 | 收藏/关注唯一，取消可见、浏览/搜索历史归属本人 | PersonalTest、Chrome |
| REQ-024 | 买家单向屏蔽，目录/详情/卖家页过滤，不删历史订单 | PersonalTest、Chrome |
| REQ-025 | 商品/账户举报、原因/图片、上海自然日限频、管理员反馈，不自动处罚 | ModerationTest、Chrome |
| REQ-026 | 违规下架、封禁/解封申诉；申诉通过商品仍需复审；已登录封禁会话限制 | ModerationTest、AuthorizationTest、Chrome |
| REQ-027 | 日期区间聚合；低样本合并，不返回个人订单/手机号/地址 | InsightsTest、Chrome |
| REQ-028 | 核心外键与唯一关系、订单快照/支付流水、预约/购物车/关系/历史持久化、事务回滚 | ConcurrencyTest、MigrationTest；JPA约束 |
| REQ-029 | 提审/审核/治理/预约/议价/交易/支付/退款/结算日志，管理员过滤查询 | ActivityService、Chrome日志页 |
| REQ-030 | 1万商品、10真实HTTP会话；列表/搜索/下单/支付各100次，断言P95阈值 | PerformanceTest，evidence/performance-results.json |
| REQ-031 | 注册/改密/重设统一密码复杂度，提示具体缺项；限制BCrypt输入字节长度 | AccountTest、IdentityMediaTest；AccountService.validatePassword |
| REQ-032 | 主要业务均有Thymeleaf页面，非静态演示按钮 | scripts/browser-smoke.cjs 与 evidence/browser-results.json |
| REQ-033 | H2文件持久化、事务回滚、进程停止重启、在线数据库/媒体备份及独立恢复验证 | ConcurrencyTest、MigrationTest、PerformanceTest备份恢复、Chrome重启 |
| REQ-034 | 管理员统计返回去标识聚合DTO；他人订单只交易双方可见；低样本金额隐去 | InsightsTest、MarketplaceTest、AuthorizationTest |

定时任务检查周期30秒；性能结果为本机本轮实测，详细数据以evidence中保存的JSON为准。浏览器验收使用隔离测试数据，截图不含真实个人信息；包含测试数据库的备份留在忽略的target目录，不提交远程。

本轮直接迁移原型六页，补充公开留言/卖家回复、审核快照、本人记录弹窗和授权后台DTO；设计及实际浏览器证据见 docs/LAB2-ALIGNMENT.md 与 evidence/design/。
