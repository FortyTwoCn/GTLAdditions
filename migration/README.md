# GTLAdditions 原生 NeoForge 迁移

状态：完整 Additions 主源码已原生移植并完成本地构建、完整世界/客户端及普通安装器验收。最终生产 JAR 与 Core 在普通 NeoForge 1.21.1 环境中启动、保存和实际磁盘重载，物理客户端的机器菜单、指南公式、资源重载及无线终端检查通过。具体范围、未定位的 JVM 崩溃风险和其余未覆盖路径见 Core 文档；不将这些检查扩展为所有功能与长期负载的无条件保证。

功能基线为 `8caff5e93a5e65914d10dd176d48d66e7ec8c329`（3.2.8Custom-fix2），Core 使用用户补充的 skyblock 1.2.3.2-fix3；API 与构建基线为 FortyTwoCn/GregTech-Modern 1.21 的 `c72dc16b52795cbb0456b6e15e13cc4a85c0ed1b`。Java 21、Gradle 8.14、ModDevGradle 2.0.141、NeoForge 21.1.248、Parchment 2024.11.17、Kotlin 2.0.21，依赖使用原生制品或固定源码构建。旧 Forge JAR 不进入运行类路径。

用户已取消旧存档兼容性要求；仍验证新 1.21.1 世界的保存和重载。源码准备、精确依赖版本、整合包资产、证据边界和剩余工作详见相邻 [Core 迁移文档](../../GTLCore/migration/README.md)。

首次构建先在 Core 运行 `python3 migration/prepare_native_workspace.py prepare --build-modularui` 准备固定源码及 GT 参考使用的 ModularUI 补丁版；`verify` 模式可只检查依赖及源码哈希。

## 完整生产验收入口

先用 Core 的 `migration/pack-reference/prepare_native_profile.py` 从固定 GregTech Leisure 1.4.5.2 ZIP 准备 `migration/full-gametest` / `migration/full-client`。脚本提供真实注册内容和资产，保护自定义文件；完整整合包服务端配方和任务未加载。客户端需要在 `saves/Native Production Validation` 中准备已在原生组合下验证的新 1.21.1 世界，可使用该脚本的 `--seed-world`。

独立联机验证还需用 Core 的 `migration/pack-reference/prepare_native_machine_recipes.py` 为 `migration/full-dedicated/server` 准备来源和哈希可核对的原始太空电梯运行配方。它保留原始耗时、电压、计算量与电路要求，其他整合包服务端脚本不加载。

```sh
./gradlew assemble verifyGtceuBaseline verifyNativeRuntime --no-daemon
./gradlew verifyNativeProductionWorld -Pnative_production_tests=true \
  -Pkotlin.compiler.execution.strategy=in-process --no-daemon
./gradlew verifyNativeProductionClient -Pnative_production_tests=true \
  -Pkotlin.compiler.execution.strategy=in-process --no-daemon
# 先准备 migration/full-dedicated/server 和 client 两个验证配置。
./gradlew verifyNativeProductionDedicated -Pnative_production_tests=true \
  -Pkotlin.compiler.execution.strategy=in-process --no-daemon
```

验证模组只在显式开启属性时加入，不打包进发布 JAR。Gradle 任务必须看到各项明确的 PASS/完成日志；进程返回 0 不等于测试完成。

完整生产注册的世界测试已覆盖 88 个 Additions 自有机器实现，独立 trait、默认 NBT 往返和真实世界 BE 替换，以及 82 个允许独立打开的服务端原生菜单。原本注册到 GT/GTMThings 命名空间的仓口继续使用其分类，Additions 自有机器分类按真实注册 ID 逐项核对。完整客户端第 22 轮启用原生 Wildcard 后实际绘制并同步两个模组的 231 个菜单、执行 8 次维护仓点击 C2S，并在资源重载前后两次核对 2,164 个机器模型；62 个 Additions 自有分类机器 ID 完整。证据见 [Core 完整客户端记录](../../GTLCore/migration/validation/full-production-client/editor-reopen/evidence.json)。完整生产世界第 25 轮实际验证 148 个最小原始结构，并覆盖 3 个定义的 9 个较大重复页（含各自最大页）、真实蒸汽活塞锤的注册配方付费加工、星门原始结构/网络/物品跨维度；14 项 required GameTest 全部通过，见 [世界记录](../../GTLCore/migration/validation/full-production-world/stargate-native/evidence.json)。全部重复组合和全部机器实际付费加工仍需验收。

五张实际 Avaritia 极限合成配方已在原生 Tier 4 9×9 合成库存中验证匹配与合成，并拒绝低等级和缺少输入。Infinity 仓非默认验证覆盖命名组件、精确 long 库存、原生配方分组、共享催化剂、世界重载和 Native SyncDataHolder；这些检查已在完整生产世界第 12 / 16 轮通过。

SGJourney 的原生 0.6.49 集成、超级样板仓/代理、无线能量与数据、光学计算、双重仓和多配方逻辑已经迁移到当前 Native API。此前独立测试的缩小结构和单项菜单不代替完整生产验证。原生 Wildcard 已完整构建并联调实际 AE2 网络和编辑器；星阵压缩持久缓冲已通过输出堵塞、停止、世界实体重载、实际控制器掉落和精确重试交付。独立客户端及服务器通过原生 GUI 和实际 TCP 验证精确 AE long、组件、库存、真实输入/滚轮及磁盘重启，见 [独立 AE 证据](../../GTLCore/migration/validation/full-production-dedicated/ae-exact/evidence.json)。成型控制器的完整客户端验收见下述第 36 轮；多个并发客户端和更多外部集成路径仍未覆盖。每项验证必须保留其实际加载范围。

早期完整编译诊断及历史单项记录见 [historical-notes.md](historical-notes.md)，其中旧失败状态不代表当前工作树。

独立第 12 轮进一步验证了真实实体数据包中的 GT 腐败组件，并修复物理客户端解码时访问服务端世界的断线问题；真实玩家通过原生星门进入受限 Lantea，非法进入被拦截、许可被消耗、客户端切换维度与物品组件完整，磁盘重启后复验通过。见 [联机与传送记录](../../GTLCore/migration/validation/full-production-dedicated/protected-travel-spoilage/evidence.json)。

第 36 轮完整客户端已进一步验证 148 个成型控制器、24 类实际绑定部件、6 个线圈纹理菜单、602 个原始翻译键及资源重载，零 ModularUI 布局与字段 Codec 错误，见 [成型/翻译记录](../../GTLCore/migration/validation/full-production-client/formed-localized/evidence.json)。原始 JSON 配方 ID 与真实 GT 查找库的重复问题已在 Core 兼容层修复；原生父/子配方回调及第 41 轮的 14 项完整 GameTest 通过，见 [配方记录](../../GTLCore/migration/validation/full-production-world/native-recipe-ids/evidence.json)。三个实际 EMC 可选组合的记录及发布闭包规则见 Core 文档。

最新局部原生回归为 [514 项 JUnit 与 237 项世界测试](../../GTLCore/migration/validation/native-regressions-514/evidence.json)，包括恒定耗时超频、两个线圈构造路径的 Codec 注册修复及原生无线库存包装层的吞吐与网格切换追踪；测试采用明确选定的生产类，完整主工程验收单独记录。

真实运行的太空电梯客户端还发现了固定 GT 的 Bloom 缓冲生命周期错误。Core 已加入严格客户端 Mixin 修复，并以实际原生缓冲和开启 Bloom 的电梯支架作回归。第 33 轮完整独立联机和实际磁盘重启均通过，包含三个真实无线终端、组件吞吐数据、七级火箭月球旅行、原始付费太空电梯火星旅行；见 [完整独立联机记录](../../GTLCore/migration/validation/full-production-dedicated/wireless-rocket-elevator-bloom/evidence.json)。电梯的原始电压、计算量和耗时保持不变，有限供电夹具只向实际能量仓转入预算内的 EU，不代表外部电池与能量线缆路径已验证。该轮使用测试专用 JIT 诊断参数，原因和实际覆盖边界详见 Core 文档，默认 JVM 的发布运行另行验证。

生产构建须执行 `assemble check exportNativeRuntime`，然后用 `migration/stage_native_distribution.py` 验证并分发完整运行依赖。官方 KotlinForForge 的语言加载器及运行库须进入闭包；普通安装器的验证记录单独保留。最终 [独立构建](../../GTLCore/migration/validation/fresh-build-final/evidence.json)、[完整世界及可选集成](../../GTLCore/migration/validation/full-production-world/final-optional-integrations/evidence.json)、[发布服务器](../../GTLCore/migration/validation/packaged-server/packaged-server-evidence.json) 和 [发布客户端](../../GTLCore/migration/validation/packaged-client/evidence.json) 均通过。最终显示文本修正未改变 2,470 个生产类的字节码；完整普通客户端功能检查和最终文本复验的具体制品哈希分别保留。
