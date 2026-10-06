# 历史阶段记录

以下记录保存迁移过程中的阶段结果，不代表当前工作树状态。当前状态与运行入口见 README.md。

# GTLAdditions 1.21.1 NeoForge 移植

**状态：移植进行中，完整生产构建尚未通过，当前没有可发布的 GTLAdditions JAR。**

Core 完整主工程依赖已编译并 assemble 通过，Additions 已进入自己的完整 Kotlin 语义检查。上次检查为 1,948 项诊断、138 个文件（gtladditions-full-native-4.log）；本轮继续迁移真实 queued machine builder、结构谓词、机器 trait 和 GUI。Core 真实服务端已进入数据包加载，仍缺匹配整合包的 KubeJS 内容，未完成世界加载。

源码基线为 `8caff5e93a5e65914d10dd176d48d66e7ec8c329`。Core 功能基线按用户补充的 AaAdoniSsS skyblock fix3；构建/API 基线按 FortyTwoCn/GregTech-Modern 1.21 的 `c72dc16b52795cbb0456b6e15e13cc4a85c0ed1b`。

用户已取消旧存档兼容性要求。目标是完整原生功能、新建世界加载、1.21.1 内的保存与重载。依赖版本、源码准备、已完成改动和验收范围详见相邻 `GTLCore/migration/README.md`。

## 已验证

- Java 21、Gradle 8.14、ModDevGradle 2.0.141、NeoForge 21.1.248、Parchment 2024.11.17；Kotlin 2.0.21。配置取自固定的 GTCEu 基准。
- Core、GTCEu 和原生 GTMThings 使用源码组合构建，没有用旧 Forge JAR 填充类路径。
- MachineDeltaPacket 和 SyncDimensionMachinesPacket 使用 CustomPacketPayload、RegistryFriendlyByteBuf 和 StreamCodec；3 个测试覆盖字段与维度 ID、空快照、快照复制、非法数量。
- 原生网络注册使用 playToClient；快照可发送空集合，以清除过期的客户端机器列表。真实连接及跨维度处理仍需运行回归。
- 超级并行仓采用原生 ParallelHatchPartMachine、SaveField/SyncToClient 和 ModularUI。Int.MAX_VALUE 上限、服务端输入校验和共享行为保留；该生产类通过 Kotlin API 编译。
- 1.21 数据包目录和合成结果格式已转换，两个 OBJ 模型使用实际的 `neoforge:obj` loader。
- 551 个资源及 133 个基线静态注册字符串没有丢失。新增资源是 SGJourney 原生战利品表的流体 ID 修正；该结果不能证明全部机器/配方的实际注册。
- 已发布外部运行依赖通过旧 Forge JAR 和重复 mod ID 检查。此检查不等于必需依赖全部齐备。

## 配方与原生注册

26 类配方改为 GTRecipeTypeEntry，保留原有 gtceu 类型/serializer ID、四类容量、EU 方向和隐藏类别。2 个线圈候选布局、炼金覆盖层和物质催化剂图标改用 ModularUI；客户端实际布局仍需验证。4 个声音使用原生 holder，并保留 gtladditions 声音 ID 与衰减距离。

GTLAddRegistration 直接使用原生 GTRegistrate；多方块辅助方法改为原生 builder 扩展，工厂由 GT 提供。配方提示延迟读取绑定后的类型，保留旋转/翻转限制。机器本体、结构及全部注册声明仍未完成迁移。联合范围目前为 404 项 JUnit 和 192 项服务端世界测试，通过了 40,791 条配方加载（包含受控测试材料配方）；这不是完整模组的运行证据。RecipesModify 从构造期移动到 common setup，避免提前读取未绑定 entry，配方回调使用原生容量、材料标签和 long 数量规则；完整注册入口仍待联调。

MEStorage 能力提供器改为 NeoForge RegisterCapabilitiesEvent，保持前面查询和实时朝向判断；指定生产类的 API 编译及 2 项测试通过。分子装配器无穷模式复用 Core 原生执行与输出等待逻辑，其 MECraftHandler Mixin 已在真实 FML 中严格注入并验证 Long.MAX_VALUE 数量；Core 生产分子装配 IO 已通过真实 AE 网格、正常机器 tick 和存储盘输出验证，并验证原生分页菜单及超大数量保存重载。Additions 完整分子装配控制器、维度模块、无穷模式与真实 AE CPU 的整条作业路径仍待联调。

联合验证工程位于 Core 的 migration/recipe-tests，直接编译上述 8 个 Kotlin 生产文件及所需 Core 生产类。13 项 Additions 测试验证 26 类真实 FML 注册、容量、方向、隐藏属性、线圈与覆盖层、声音、复合配方类型、builder 限制、真空/氦凝结/混沌炼金/空间处理/激光派生及地脉数量规则；它不加载完整 Additions，也不替代主源码依赖。

联合验证还覆盖 Core 原生 RecipeDB 的输入分组/组件流体索引、标签库存仓、维护/净化/重力条件和快速上传服务。Additions 搜索 Mixin 已与 Core 搜索 Mixin 在同一 FML 中组合验证；7 个完整 Kotlin 搜索调用方使用新接口，但它们及 MutableRecipesLogic 的完整编译仍待完成。多候选模式目前使用原生 RecipeLogic 的公共接口 fixture 验证，不能用此结果宣称 Additions 完整机器已运行。客户端各菜单的日志、截图和源码哈希分别保存；新增维护验证不覆盖 Additions 全部机器 GUI。

联合范围新增 Core 的 HPCA、模拟器、光学计算机、基岩流体/钻机、创造磁盘克隆、Jade long 产物序列化、材料生成、无线数据仓、环境发电机、温室及屠宰场验证；30 个原生基岩流体生产数据生成文件与资源逐项一致。原生多方块预览另有 7 张客户端截图，验证 ModularUI 操作、实际 GT JEI 注册/布局、资源包重载及世界预览；完整 Additions 机器预览及可选 EMI/REI 尚未验证。该轮两个工程的局部 API、回归与外部运行依赖检查通过；其历史全量编译结果为 Core 917 项诊断，当前状态见本文开头。联合范围还验证结构检测与修复生命周期、原生机器模式选择，以及普通样板仓三列控件、真实 108 槽滚动/分页/催化剂弹窗、缓存标记同步和产物预览。模式与 AE 菜单分别保存三张、七张客户端截图；不代表 Additions 全部 GUI。证据与各运行独立输入哈希见 Core 的 validation/native-recipes/manifest.json。

Core 鸿蒙之眼新增两项世界验证，覆盖原生无线账户绑定、精确启动能量/long 气体、拒绝启动时不扣资源、周期一次扣费及服务端菜单保存；戴森球新增三项，覆盖发射/维修、接收器实际光照、真实光纤计算/发电、堵塞门控及保存字段。两者使用缩小结构验证生产控制器，原始大型结构、完整注册和实际客户端未验证。

GT 依赖使用与所给参考同一提交的 GregTech-GTL-1.21，只应用 Core 文档记录的失效 LDLib 可选 Mixin 元数据补丁。两个目标工程检查该唯一补丁及完整 diff。AE2WTLib 19.2.0 的原生制品在专用服务端构造阶段引用客户端界面；现改为组合编译固定提交的完整源码/API，并核对 Core 文档记录的客户端注册隔离补丁。修补后的依赖与 ExtendedAE 已共同通过真实客户端和专用测试服务器加载；完整 Additions 无线终端路径仍待验证。

## SGJourney 原生集成

以 SGJourney 0.6.49 原生 NeoForge JAR 和固定源码 `6033261b6f5d8330f043d5ef9cf5c007ac88a161` 为基准，迁移了 19 个通用、2 个客户端 Mixin。每个 Inject/Redirect 均要求至少一次注入。保留上游拨号校验、连接模拟、事件和多目标传送流程，使用原生能量接口实现免能量行为及保持连接；没有复制旧版整个拨号实现。

DHD 使用原生菜单、缓存和同步；旧 Forge GUI 打开入口已移除。经典环仍按原有功能组装为 Pegasus 星门。客户端 tooltip 与服务端 Mixin 分离。流体保留原有四个 `_ban` 注册 ID；对应的原生 jaffa_house 战利品表修正见 `stargate-resource-overrides.json`。保留原生村庄标签必需的 POI 注册，同时仍禁用原有村民职业与交易。目的地物品使用原生注册表查找，缺失依赖时明确失败，未改为 air。

独立 NeoForge/FML 测试工程直接编译、加载生产 Mixin 和资源，7 项测试通过，覆盖实际 Mixin 目标、能量开关、多目标连接 tick、模拟拨号、流体 ID、POI/职业注册和原生流体组件解码。测试配置与生产星门 Mixin 清单自动核对。

实际客户端已验证新世界创建、三个 DHD 拨号界面、空手组装经典环为 Pegasus、保存退出、重启客户端及重载世界。重载后原生 `/data get block 102 64 73 id` 仍返回 `sgjourney:pegasus_stargate`，已保存的 DHD 可重新打开。截图、测试 XML 与测试范围见 `validation/sgjourney/manifest.json`。这是独立测试工程的验证；完整模组、真实跨维度玩家传送和完整专用服务器尚未验证。

## 验证命令

使用 JDK 21，先按 Core 文档准备两个固定源码依赖和修补后的 ModularUI，再在本仓库运行：

```sh
./gradlew compileNativeMachineApiJava compileNativeMachineApiKotlin migrationRegression verifyNativeRuntime
./gradlew compileNativeStargateApiJava
./gradlew -p migration/stargate-tests test
./gradlew -p migration/stargate-tests runClient
python migration/audit.py --baseline 8caff5e93a5e65914d10dd176d48d66e7ec8c329 --output migration/audit-report.json
```

独立验证源集 `migrationApi`、`migrationRegression`、`nativeMachineApi`、`nativeStargateApi` 及星门测试工程均不替代生产 `main`，不进入发行 JAR。

必须单独完成的生产验收：

```sh
./gradlew compileKotlin compileJava
./gradlew build
./gradlew runData
./gradlew runServer
./gradlew runClient
```

此前 `compileKotlin` 被 Core 全量编译阻塞；现已进入 Additions 自身的 Kotlin 语义检查。Additions 仍有旧 GTCEu、Forge、LDLib、Mixin、配方和物品 NBT API；完整 Kotlin 语义编译、启动、新世界、GUI、多方块和集成回归尚未完成。

本次测试 XML 位于 `build/test-results/migrationRegression/`。详细工作区日志位于 `/workspace/cloud-setup/gtladditions-*.log`。静态审计的限制及允许的资源转换列在 `audit-report.json` 和 Core 文档中。

Core 通用机器 tick 已保留原生脏字段/保存步骤，新增世界测试覆盖启动预算、关键豁免、取消/延期恢复、实际工具周期暂停及 LDLib 异步服务端排队。性能监视器另有三张实际客户端截图，验证受控服务端计时数据的 S2C 刷新、列表释放、滚轮末行点击及 15 秒维度/坐标高亮；实际 tick 采样有独立世界测试。本批 Core/Additions 局部 API、回归及外部依赖检查均通过；完整构建及整包启动仍未通过。

联合范围新增 Core 通配符及库存型通配符总成的原生配方、缓存、备货事务和两个客户端菜单验证。多个 Long.MAX_VALUE 槽位通过分段返还保持总量，实际拆机/BlockItem 放置不会丢失或重复返还物料；最新 398 项 JUnit、183 项世界测试和 30 项数据生成校验通过。六张客户端截图保留早于返还队列的独立源码哈希；外部通配符模组/编辑器与完整注册仍待迁移。

联合范围还迁移并验证 GT/GTMThings 三种原生 ME 输出部件，覆盖精确 long 组件输出、重复键容量预留、真实控制器配方、回流周期及实际拆装恢复；三个客户端菜单另有 9 张截图和独立源码哈希。最新全量 Core 编译为 950 项诊断，仍阻塞完整 Additions。

共享物品/流体催化剂已改用原生组件键和独立 long 需求匹配，实际控制器配方、重载及清空缓存通过验证。研磨球仓采用原生工作回调、保存字段和 ModularUI，世界测试验证实际 40 点伤害、创造模式豁免、菜单绑定与闲置/工作中拆机掉落规则。

## 超级样板仓与 FOA 模式

Additions 超级样板仓与代理使用原生 GT block entity、SaveField/SyncToClient、AE2 19 组件样板和 ModularUI。保留默认 162 槽及可配置的 1 至 1800 槽容量、页码和物理槽位索引；单一滚动视口修复横纵滚动后的裁剪和点击。原有三条特殊配方按当前原生 RecipeManager 查询，匹配完整组件和 Ingredient 谓词；额外循环容器、非循环产物倍率、1 至 30 倍限制及精确 long 运算保留。数据棒使用独立且带维度的原生组件标记，普通/超级绑定互不混用，研究数据和其他组件不丢失。

新增五项 JUnit 与四项世界测试验证实际 AE 网格发布、控制器消耗与一次性产物交付、原生菜单 C2S、保存重载、原始特殊规则的更新，以及仅含代理的控制器通过原生 RecipeHelper 完成配方。三个客户端菜单和九张截图验证 162/1800/1 槽布局、FOA 和页码双向同步、两个滚动方向、末格组件样板产物预览与催化剂中键弹窗。各运行日志和独立输入哈希见 Core 的 validation/native-recipes/super-pattern-client/。验证采用独立注册入口，完整 FOA 控制器、模块、原始生产注册和物理多人连接仍未验证。两个主工程尚未完成全量构建与启动。

### 原生标签过滤与传输覆盖板

物品/流体标签过滤器改用 GT 1.21 的统一 TagFilter，保留 Core 的无命名空间表达式、布尔运算和错误拒绝规则，缓存、组件保存及 GUI 继续由原生 GT 负责。实际仓口能力验证纯模拟、组件传输、条件修改后缓存失效及保存重载。

无限水覆盖板保留原生安装、每 20 tick 供水及订阅生命周期，供水量仍为 Integer.MAX_VALUE。输送带、机械臂、泵和流体调节器接入原生构造流程，保留十五个电压等级的 Core 速率；ULV 调节器保留 long 预算，通过多次原生 int 能力调用传输超过 300 亿 mB、十四种组件流体，无预算刷新重复产出。八个生产盖板定义保留原 gtceu 注册路径，改为原生 Holder 延迟注册及延迟客户端 renderer 工厂。

新增 3 项 JUnit、7 项世界测试通过，涵盖真实大仓口、订阅移除/重载及设置恢复。原始 Core 盖板物品注册、盖板客户端渲染与物理多人连接仍待完整主工程联调。证据日志为 gtl-native-cover-transfer-full-validation-3.log；该轮 395/176/30 结果已归档，旧客户端证据保持各自运行时的独立源码哈希。最新两个主工程构建仍失败，Core 为 830 项诊断，Additions 受 Core 编译阻塞。


## 原生无限流体钻机

Core 无限钻机接入原生附加 RecipeLogic、基岩流体 Holder、真实能量仓及 long 流体 Ingredient，保留 256 倍产量、最多高一个电压等级和 50% 超频增产。共享计算先用 long 乘法再除以剩余次数，消除 int 溢出；Core 不消耗矿脉次数。原生 GT 钻机通过严格注入保留自身耗电与消耗矿脉生命周期，配方产量支持 long，其原生 int 显示接口采用饱和值。

完成后暂停、满输出等待、恢复交付及运行中保存重载经过实际控制器验证；两种暂停标记均由原生 SaveField 保存，等待输出不会额外耗电、丢弃产物或提前消耗矿脉。3 项新增 JUnit 与 7 项世界测试通过，联合验证为 398/183/30。三种实际客户端菜单验证鼠标启停 C2S/S2C、只完成一轮及准确耗电、精确 long 服务端显示和实体重载。九张截图、日志和独立源码哈希见 Core 的 validation/native-recipes/fluid-rig-client；紧凑原生滚动列表使标题栏、侧栏和背包在 854×480、GUI 缩放 2 下完整显示。

测试使用独立注册入口、受控矿脉及缩小结构；原始生产注册、完整结构、星球生成及物理多人连接仍待整模组联调。Core 全量编译仍有 950 项诊断，完整 Additions 仍受其阻塞；两个局部原生 API、回归及外部运行依赖检查通过。

## Core 高级无限钻机的联合验证

Core 高级无限钻机、long 产量/EU、冷却优先级、温度传感器和 300 tick 熔毁已加入原生验证，工具等级 10 保留原值并使用原生掉落限制标签。联合 404/192/30 通过；七张客户端图、日志和独立输入哈希见 Core 的 validation/native-recipes/advanced-fluid-rig-client。高速按钮、组件钻头的鼠标取放、只完成一轮且准确耗电、只读同步文字、滚动/换行和实体重载均验证。独立注册、简化结构与稀疏材料夹具不代表完整 Core/Additions 已启动，完整生产结构、材料及物理多人连接仍待验证。最新 Core 主工程有 917 项编译错误，Additions 仍受其阻塞。

## CWU 并行与碎岩机流体条件原生迁移

CWU capability 实现原生 getMaxParallelByInput，预览使用实际光学提供器的模拟预算，按提供器身份去重并使用 long 聚合；不分配 CWU 或扣电。实际光学网络世界测试验证原生 ParallelLogic 调用正确限制并行。碎岩机条件迁移到原生 AdjacentFluidCondition：原始 fluidA/fluidB 数据每次解析，形成的多方块检查所有输入槽位，排除输出仓且不消耗环境流体；普通机器保留原始水平邻接规则，显式原生条件保留其空间语义与 codec，反向条件只应用一次。

新增 4 项 JUnit、3 项世界测试通过，联合结果 408/195/30，日志 gtl-native-cwu-rock-full-validation-4.log。证据已归档为 935 个输入文件、237 项证据；此前客户端证据保留各自运行时的源码哈希。本轮两个工程的指定原生 API 检查通过；Core 全量诊断为 909 项，Additions 仍受 Core 编译阻塞，完整模组启动尚未通过。

## 裂变反应堆原生热量与配方生命周期

生产控制器改用原生保存/同步字段、结构回调及 RecipeModifier。燃料与冷却单元计数、邻接关系来自实际接受的原生谓词位置；配方预览不扣输入/电力或写运行热量，接受配方时才保存有效并行和 FRheat。保留原有冷却需求、邻接升温、冷却加速、825K 钠钾输出切换、空闲散热/损伤维修和过热爆炸规则，使用 long 中间量及精确流体 Ingredient。冷却交换先检查完整输出与输入，避免输出堵塞扣掉冷却剂；零输出不会扣水，红石输出限制在 0..15。

付费配方在产物堵塞时保留完整进度和完成后暂停，恢复时只交付一次。仓口卸载使热量/配方订阅停止，重载重新绑定原生结构与当前实体并保留已付费周期。4 项新增 JUnit、7 项新增世界测试通过，联合结果 412/202/30，日志 gtl-native-fission-full-validation-7.log。归档含 941 个输入文件、238 项证据；旧客户端证据仍保持其运行时的独立源码哈希。验证使用真实 GT 能量/物品/流体仓口、简化结构及稀疏冷却材料；原始完整结构、生产材料注册和裂变 GUI 客户端仍未验证。


## AE2 吞吐量监控原生迁移

生产监控部件改用 AE2 19 的物品交互、RegistryFriendlyByteBuf、注册表感知 NBT 和原生顶点接口，保留统计周期、插入/提取中较大方向的显示规则及原纹理路径。真实 AE 存储 Mixin 保留手动合成库存预留，嵌套监听去重和 CompositeStorage 拓扑更新通过原生接口验证。GT MetaMachine 已直接继承 BlockEntity，来源解析优先保留机器朝向，避免被通用实体分支吞掉。

3 项新增 JUnit、3 项新增世界测试通过。玩家实际配置监控项后，在通电 AE 电缆主机、真实驱动器和 GT 来源节点中验证模拟不记账、组件键、精确交易、来源维度/坐标/朝向、采样、设置及部件/主机重载。联合结果 415/205/30，日志 gtl-native-throughput-full-validation-5.log；归档含 952 个输入文件、239 项证据，旧客户端证据保持独立源码哈希。测试使用独立部件与配置物品注册；完整生产注册、无线终端流量和客户端趋势图标仍未验证。

GT 原有机器/激光仓口配方加载器已初步采用原生 MachineEntry 和 GTCEu.Mods 接口，保留配方 ID、数量及高等级未启用时的空入口检查；完整生产配方加载尚未验证。最新完整 Core 构建仍有 855 项诊断，Additions 被 Core 编译阻塞，日志分别为 gtlcore-full-native-throughput.log 和 gtladditions-full-native-throughput.log。


## 仓室终端注册表感知快照

生产分块传输改用 RegistryFriendlyByteBuf，保留 128 KiB 分块与 16 MiB 完整快照上限。发送端先完成全部编码；接收端在全部块按顺序收到、成功解码且没有剩余字节后才发布菜单内容。目录图标、文本、AE 物品/流体键及 long 数量采用 1.21 原生 codec，原消息 ID 保留。无线快照消息修正原生 IPayloadContext 分发；普通机器远程菜单交互初步采用原生物品/无物品两阶段接口及原生触及距离。

5 项新增 JUnit 验证组件键、空堆叠、带样式的翻译文本、Long.MAX_VALUE 数量、跨块重组、乱序/缺块/版本冲突、未完成快照替换、越界/截断/尾随数据及失败后恢复。联合 420/205/30 检查通过，日志 gtl-native-chamber-snapshot-full-validation.log；归档含 954 个输入文件、240 项证据。完整仓室菜单、无线消息注册及客户端实际网络收发仍未验证；专项 codec 通过不代表整个无线系统已完成移植。


## 2026-10-04 原生 ExtendedAE、无限单元箱子与消音仓

原始 ExtendedAE 样板修改器的 item、host、menu、screen 和已注册原生数据包直接使用于验证。Core 范围修改、整数倍率上限、精确大数替换、强制插入/删除、交换、名称/物理数量及组件持久化已迁移；非结构元数据复制不会恢复旧样板配方。实际客户端从明确范围初始状态操作范围倍率、翻页、中键弹窗、键入 Long.MAX_VALUE 和替换动作，通过真实 C2S/S2C，并检查输出与名称保留。4 张截图与独立源码哈希见 validation/native-recipes/extendedae-client；完整合成样板替换和物理多人连接仍未验证。

AE2 已删除的 ICellGuiHandler 入口改为严格注入原生 MEChestBlockEntity.openGui：两种 Core 无限单元进入原生存储菜单，普通单元保持原生菜单及类型过滤能力。世界测试使用实际服务器玩家和原生网络包接收器，未声称箱子客户端界面已验收。消音仓严格注入原生 modifyRecipe，保留原生前方空气与恢复规则，向所有实际绑定控制器同步堵塞原因并处理脱离控制器的状态；真实合金高炉结构验证堵塞、解除与脱离。

旧 LDLib 1 库存能力包装器在原生 LDLib 2 中已不存在，能力直接走 NeoForge；原有 long 存储适配保留。服务器停止阶段的查找保护移到 GTCapabilityHelper 原生入口，正常能力查找已随世界测试执行，真实停止阶段尚未单独验证。Ad Astra 火箭按原生 Common Storage Lib 的 FluidResource/Predicate 迁移 7 种燃料与 16000 mB 容量/耗量。编译使用原始嵌套 API JAR，外层制品、3 个内层 JAR 哈希与真实方法签名见 native-adastra-api.json；完整火箭材质注册、启动和耗油未验证。

本轮联合结果为 437 项 JUnit、216 项世界测试及 30 项世界生成检查，日志 gtl-native-extended-chest-full-validation-22.log。归档包含 1418 个输入文件和 287 项证据；此前客户端证据保留各自源码哈希。研磨球掉落测试已改用覆盖实际放置范围的 32³ 模板，并只统计本次产生的实体，避免相邻场景干扰。主工程仍未编译或整包启动成功；专项验证不代表完整迁移完成。

## 2026-10-04：联合无线与创造能源验证

Core 原生探矿器、无线监控器、中子加速器接收封面及两种创造仓的适配详见相邻 Core migration/README.md。联合回归通过 447 项 JUnit、221 项世界测试及 30 项世界生成检查；Core 原生客户端另验证 11 张能源界面截图及真实 C2S/S2C。证据保留各轮独立源码哈希。该结果不证明 Additions 全部机器、无线控制器或完整模组可构建和运行。
