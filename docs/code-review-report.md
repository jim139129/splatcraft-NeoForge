# Splatcraft NeoForge 1.21.1 重写版代码审查报告

- 审查对象：`splatcraft-NeoForge`（Minecraft 1.21.1 + NeoForge 21.1.233，约 188 个 Java 文件 / 2.6 万行）
- 对照基准：`splatcraft-forge`（1.18.2 Forge 原版）及 `splatcraft-forge/docs/original-implementation-analysis.md`
- 审查日期：2026-07-26
- 审查方式：按子系统（网络/玩家状态、世界墨水、武器物品、实体、方块与配方、命令与数据、客户端与 Mixin、功能覆盖对照）深读全部源码；高危结论均经二次核实（含对 FML loader 4.0.42 的字节码级验证、对原版 1.18.2 源码的逐行对照）。
- **修复状态（2026-07-27，分支 `fix/code-review`）**：全部高危 H1-H5、中危 M1-M18 及可修复低危项已处理，并通过 `compileJava validateSplatcraftResources`。M4-M6（世界墨水渲染）采用相机相对坐标消抖 + 按方块实际 VoxelShape 铺墨 + 视锥剔除并移除会导致远处墨水成片消失/跳变的 4096 扫描序截断；受限于无法在无头环境实机验证渲染，未做完整的 GPU 烘焙缓存（即时模式渲染保留，但已用视锥剔除收敛每帧开销）。下文第三节为审查时的历史发现；少数无可利用路径且改动会扩大行为风险的项已在“低危处置结果”中明确接受依据。

---

## 总体结论

重写版整体迁移质量**较高**：功能覆盖面近乎完整（方块/物品/实体/声音/配方/gamerule 逐项核对零缺失，58 色、35 个武器 settings、196 个配方全部对齐），且原版文档点名的核心安全风险——**客户端计算 charge、C2S 弱校验**——已被系统性修复（蓄力改为服务端权威推导，所有 C2S payload 均有身份/距离/菜单归属/数值范围校验），还顺手修掉了原版若干历史 bug（ColoredShapedRecipe 除法错误、SingleUseSub 复制、工作台部分扣料不回滚、stage 命令跨维度 bug、INK_CACHE 内存泄漏等）。

但存在 **5 个高危问题**，其中最严重的是**发布级缺陷**：mixin 配置未按 NeoForge 1.21.1 的方式声明，正式构建的 jar 中全部 9 个 mixin 会静默失效（开发环境正常，完全掩盖问题）。其余高危为一个可生存利用的刷物品漏洞、服务器重启后计分板静默失效、无上限的大范围命令、以及爆炸铺墨丢失射线遮挡的玩法回归。

---

## 一、高危问题（5 项，均已二次核实）

### H1. mixin 配置未在 neoforge.mods.toml 声明 —— 生产环境全部 mixin 静默失效
**类别：迁移回归 / 发布缺陷**

- `src/main/resources/META-INF/neoforge.mods.toml` 中没有 `[[mixins]]` 段。
- `build.gradle:1336` 使用的是 Forge 1.18 时代的 JAR manifest 属性方式：`'MixinConfigs': 'splatcraft.mixins.json'`。
- **字节码级验证**（FancyModLoader 4.0.42，本项目实际依赖的 loader）：`ModFileParser.getMixinConfigs` 仅从 mods.toml 读取 `mixins` 配置列表（`ldc "mixins"` → `IConfigurable.getConfigList`，子键 `config`/`requiredMods`）；loader 相关类（FMLLoader、LoadingModList、ModFile、ModFileParser、ModValidator）中**不存在** `MixinConfigs` 字符串常量（grep 命中的均为内部记录类名 `ModFileParser$MixinConfig`），即不存在读取该 manifest 属性的代码路径。
- 开发环境能跑通是因为 `build.gradle:47` 向 dev run 注入了 `-mixin.config=splatcraft.mixins.json` 参数；冒烟日志中 mixin 归属显示 `from mod (unknown)` 也印证配置并非经 mods.toml 归属。

**影响**：正式 jar 在玩家环境中，鱿鱼隐身（EntityMixin）、武器冷却禁疾跑、鱿鱼穿栅格（SquidPassthroughMixin）、剪羊毛掉墨色羊毛（SheepMixin）、彩色羊毛层、玩家持武器姿势、创造模式颜色搜索、配方书分类、计分板 criteria 访问器（ObjectiveCriteriaAccessor，H3 的 remove 路径依赖它）全部静默缺失，无任何报错。dev 与冒烟测试全部通过，完全掩盖该回归。

**修复方向**（一行）：在 `neoforge.mods.toml` 添加 `[[mixins]]` `config="splatcraft.mixins.json"`。

### H2. 破坏墨水缸掉落"虚拟输出槽" —— 无限刷带色墨水瓶
**类别：安全（经济性漏洞）/ 继承自原版**

- `blockentity/InkVatBlockEntity.java:43-51` 每 tick 调 `InkVatMenu.updateOutput`（`menu/InkVatMenu.java:112-132`），把输出槽 4 填充为**预览产物**（`new ItemStack(INKWELL, craftable)`）；原料只在取出时经 `removeOutput → consumeIngredients` 扣除（`InkVatBlockEntity.java:132-159`）。
- `block/BlockStateCompatBlocks.java:1753-1759` 的 `onRemove` 调用 `Containers.dropContents(level, pos, inkVat)`——逐槽直接掉落，不经过 `removeItem`、不消耗原料（已核实 `dropContents` 的原版实现）。

**利用方式**：放入成组墨囊/能量蛋/空墨水瓶 → 选颜色 → 破坏墨水缸 → 同时掉落全部原料 + 最多 64 个带色墨水瓶，捡回原料无限循环。原版 1.18.2 存在同样问题（`InkVatBlock.java:159-171`，继承漏洞而非回归），但生存服可零成本利用，应修复（`onRemove` 前清空槽 4 即可）。

### H3. 动态计分板 criteria 重建时机晚于 scoreboard 存档加载 —— 重启后颜色计分静默失效
**类别：迁移回归**

- `scoreboard/SplatcraftScoreboardHandler.java:26-33` 在 `ServerStartedEvent` 中才从 SaveData 重建颜色 criteria。
- 1.21 中 scoreboard 存档在 `MinecraftServer.loadLevel` 阶段（早于 `ServerStartedEvent`）反序列化，1.20.2+ 原版对未知 criteria 名会警告并**降级为 dummy**。
- 冷启动（专服重启 / 单机新 JVM 打开存档）后，之前用 `splatcraft.colorKills.<color>` 等 criteria 建立的 objective 全部变 dummy；随后重建的 criteria 是**新实例**，`forAllObjectives`（`SplatcraftScoreboardHandler.java:90`，按实例查 `objectivesByCriteria`）永远匹配不到——击杀/死亡/胜负计分静默不再更新。
- 原版是在 capability 反序列化期（`SaveInfo.readNBT`，早于 scoreboard 加载）重建，顺序正确；这是重写把重建点移晚导致的回归。同 JVM 内换存档时 `CRITERIA_CACHE` 残留会掩盖此问题，只在冷启动暴露，更难排查。

**修复方向**：把重建点提前到 scoreboard 存档加载之前（如 `ServerAboutToStartEvent`）。

### H4. 大范围命令无体积上限且会同步加载区块 —— 一条 OP 命令可卡死服务器
**类别：性能 / 迁移回归**

- `command/SplatcraftClearInkCommand.java:38`、`SplatcraftReplaceColorCommand.java:84`、`SplatcraftScanTurfCommand.java:89` 坐标版均用 `BlockPosArgument.getBlockPos`（原版用 `getOrLoadBlockPos`，重写版反而退化）。
- 唯一校验 `InkAreaOperations.inWorldBounds`（`InkAreaActions.java:41-43`）只检查角点在世界边界内（水平 ±3000 万格），完全不限制体积；`InkAreaActions.volume()` 已写好却无任何调用方使用（且是会溢出的 int 乘法）。
- 实际遍历（`clearInk`:53-77、`visitColorBlocks`:79-114、`scanTopDown`:157-167）每格调 `getBlockState`/`getBlockEntity`，在 ServerLevel 上会**同步加载区块**。
- 关联：`SplatcraftStageCommand.java:642-652` 的 `stage warp` 对整个 stage 体积逐格 `getBlockEntity` 找 SpawnPad，大型 stage 同样卡顿。

**影响**：`/clearink -1000000 -64 -1000000 1000000 319 1000000` 之类命令直接把主线程卡死到进程被杀。虽限 OP（permission level 2 已正确设置），但误操作即事故。

**修复方向**：接入现成的 `volume()` 做上限校验（改 long 乘法），并只遍历已加载区块。

### H5. InkExplosion 丢失射线遮挡 —— 隔墙/封闭方块全部被铺墨
**类别：迁移回归（核心玩法规则）**

- 原版 `InkExplosion.doExplosionA`（`splatcraft-forge/util/InkExplosion.java:107-114`）对 16×16×16 方向做 `level.clip(ClipContext.Block.COLLIDER)` 射线追踪，只有射线**首个命中**的方块进入铺墨集合，墙体天然阻挡墨水。
- 重写版 `worldink/InkExplosion.java:51-84` 改为纯球形收集：半径内每个方块只要 `canReachInkableFace` 为 true 就铺墨，而 `canInkFromFace`（`InkBlockUtils.java:108`）对普通实心方块（不在 `deters_ink` tag）恒为 true——完全封闭的方块也命中。

**影响**：任何爆炸打在墙上，墙背面、地下封闭方块全部被铺墨。玩法上隔墙涂色；数据上每个封闭方块进入 chunk attachment 并逐个发同步包，衰减 tick、全量 chunk 同步（单包理论上限 16×16×384 条目，可能逼近 1 MiB payload 上限断开客户端）、渲染候选集都被放大——玩家看不见（六面被遮挡剔除）却持续占用存储/网络/衰减预算。

---

## 二、中危问题（14 项）

### 战斗与伤害

**M1. 墨水伤害对所有目标清零无敌帧（原版仅 colored 实体）** — `worldink/InkDamageUtils.java:99-103`
`bypassDamageCooldown` 时对**任意** LivingEntity 执行 `target.invulnerableTime = 0`；原版只对 `IColoredEntity`（鱿鱼靶等）重置，普通生物/玩家保留原版 10 tick 无敌窗口。两版 `inkDamageCooldown` gamerule 默认都是 `false`，即**默认配置下 bypass 分支就是激活的**——速射武器对玩家/生物的实际 DPS 远高于原版（每发全额结算，不再被无敌帧吞掉），战斗平衡明显改变。已对照原版源码逐行核实。

**M2. Burst Bomb 直接命中可双重结算** — `entity/sub/BurstBombEntity.java:27-38` + `AbstractSubWeaponEntity.java:146-151,180-204`
直接命中先施加 `direct_damage`，随后 `expire() → explode()` 的 InkExplosion 又对同一实体结算范围伤害；叠加 M1 的无敌帧清零，两段都会落地。

### 世界墨水渲染（客户端）

**M3. WorldInkRenderer 双重注册，墨水每帧画两遍** — `client/WorldInkRenderer.java:30` + `client/SplatcraftClient.java:141`
类头 `@EventBusSubscriber(Dist.CLIENT)` 已自动注册到 game bus，`SplatcraftClient` 构造器又手动 `addListener` 一次。每帧 `AFTER_TRANSLUCENT_BLOCKS` 执行两次：alpha=210 的半透明面叠两层（观感明显更不透明、glitter 双倍亮），顶点构建与邻块查询开销翻倍。已核实全库仅此一处双重注册。**修复为删一行。**

**M4. 即时模式逐帧全量重建墨水面，无缓存** — `client/WorldInkRenderer.java:66-141`
每帧扫描 17×17=289 个 chunk attachment，每个墨水块 6 方向各一次 `isInked` 邻块查询（内含 `getChunk`）+ 光照查询，上限 `MAX_RENDERED_ENTRIES=4096` → 单帧最多约 2.4 万次邻块查询、上万 quad 即时重建，无视锥剔除。原版烘进 chunk mesh 只在重建时付费。大规模涂地对战（核心场景）FPS 开销显著，叠加 M3 实际翻倍；超 4096 后按扫描顺序截断，远处墨水成片消失且随玩家移动跳变。

**M5. 墨水一律渲染为整格立方体外壳** — `client/WorldInkRenderer.java:160-165`
`renderFace` 以完整 1×1×1 坐标绘制，不读方块实际形状。楼梯、半砖、grate 等被铺墨后显示为悬浮的整格半透明壳（下半砖上方浮着半格"空气墨"），与原版贴合模型的效果差异极大。

**M6. 顶点用世界绝对坐标转 float，远离原点抖动** — `client/WorldInkRenderer.java:56,272`
世界绝对坐标直接 `(float)` 进矩阵，坐标数万格外 float 精度不足 0.002 的 SURFACE_OFFSET，墨水面相对方块可见抖动/Z-fighting。

### 世界墨水逻辑（服务端）

**M7. 衰减采样只扫玩家周围 3 chunk** — `worldink/WorldInkHandler.java:26,88-107`
`PLAYER_CHUNK_SAMPLE_RADIUS = 3` 只采样每玩家 7×7 chunk；原版从全维度所有含墨 ticking chunk 抽样。视距通常 ≥8 chunk，玩家 3 chunk 外但可见/被 chunk loader 加载的墨水**永不衰减**，玩家能直接看到远处墨水不消退。另有 off-by-one：`nextInt(MAX_DECAYABLE_CHUNKS + 1)` vs 原版 `nextInt(MAX_DECAYABLE_CHUNKS)`。

### 网络

**M8. 皮肤 overlay 上传无频率限制 + 无解码尺寸校验** — `network/payload/PlayerSkinOverlayPayload.java:60-71` + `client/PlayerInkColoredSkinLayer.java:80`
`receiveUpload` 只校验身份与 256KB 上限，无节流、无内容去重，每次上传无条件 `sendToAllPlayers`。恶意客户端每 tick 上传 256KB → 服务端以 `256KB × 在线人数` 转发（带宽放大 DoS）。且服务端不校验字节是否为合理尺寸的 PNG，客户端 `NativeImage.read` 对高压缩比超大分辨率 PNG 不抛异常而是分配巨大纹理（解压炸弹）。建议：内容未变不广播 + 上传冷却 + 解码前限制尺寸（如 64×64）。

**M9. WorldInkPayload.decode 集合无上限** — `network/payload/WorldInkPayload.java:69`
`size` 未封顶，与其他 S2C payload（GameRules=64、PlayerColors=1024、WeaponSettings=1024 均有上限）不一致。恶意/被篡改服务端可使客户端 decode 阶段 OOM。

### 方块与菜单

**M10. InkVat 任何输入槽变化都重置已选颜色** — `menu/InkVatMenu.java:324-334,233-251`
槽 0/1/2 与 filter 槽的 `setChanged` 都触发 `refreshRecipes(true)` → `setPointer(-1); setColor(-1)`；原版只有 filter 槽重置。shift 移入原料（vanilla `moveItemStackTo` 会调 `setChanged`）也触发。玩家补料即丢失颜色选择、输出消失，"边补料边产出"不可用，与原版工作流不符。

**M11. 近满漏斗抽取输出会静默销毁原料** — `blockentity/InkVatBlockEntity.java:132-159`
漏斗从底面抽输出走 `removeItem(4,1)` 先扣三格原料；vanilla 漏斗放不下时只把物品**放回输出槽**，原料不回滚，下一 tick `updateOutput` 按已减原料重算。漏斗剩余空间放不下该色 inkwell 时每个冷却周期销毁一组原料（继承自原版架构）。

**M12. world ink 的 team 是只写黑洞** — `worldink/InkAreaActions.java:406-431` + `command/SplatcraftStageCommand.java:442-448`
`WorldInkColorBlock.team()` 恒返回 `""`、`setTeam` 为空操作，但 `teams set` 仍计入 `affectedBlocks` 并发方块更新（计数虚高）；`removeTeam` 与 `/replacecolor <stage> <color> only|keep <team>` 按 team 过滤时 world ink 永不命中——team 语义在地面墨水上静默失效。

### 数据加载

**M13. ink color tag 解析无异常保护，坏 JSON 可致服务端启动失败** — `data/SplatcraftData.java:89-99`
`getAsJsonObject`/`getAsJsonArray("values")` 未 try/catch；一个格式错误的 `tags/ink_colors/*.json`（顶层写成数组、漏写 values）会让初次数据包加载抛异常、服务端起不来，`/reload` 整体失败。同文件 `WeaponSettingsReloadListener` 走 codec `resultOrPartial` 只 warn 不炸，两个 listener 健壮性标准应拉齐。

**M14. 计分板 remove/add 产生陈旧实例 + CRITERIA_CACHE 跨存档泄漏** — `scoreboard/SplatcraftScoreboardHandler.java:117-124`
`removeColorCriterion` 只从 CRITERIA_CACHE 移除，不触碰仍引用旧实例的 objective；再次 add 同色会 new 新实例，旧 objective 按实例查找断裂。服务器停止时从不清理 CRITERIA_CACHE，单机同 JVM 换存档残留上个世界的 criteria（该残留恰好掩盖 H3 在同 JVM 复开时的表现）。延续原版"直接操作原版内部静态缓存"的已知高风险方案。

### 客户端界面

**M15. InkVatScreen 奇数颜色数（>16）时最后一色永远不可选** — `client/InkVatScreen.java:203-209`
滚动到底时 `startIndex = maxStartIndex - maxStartIndex % COLOR_ROWS` 向下取整；17 色时索引 16 永不可见（应向上取整）。数据包定义奇数个墨水颜色配方即触发。

**M16. InkVat 一次点击走两条 C2S 通道** — `client/InkVatScreen.java:84-88` + `menu/InkVatMenu.java:140-142`
屏幕先经 `clickMenuButton` 触发自定义 payload，又发 vanilla 按钮包，服务端对同一点击处理两次。当前两条路径写相同值故幂等，但两套机制必须永远保持一致，一旦其一改逻辑即状态漂移。另注意 vanilla 按钮包路径使用**过期的 recipes 缓存**做校验（`InkVatMenu.java:135-148`，漏斗改容器不触发菜单刷新），恶意客户端可借此选中当前 filter 不允许的颜色（不涉及刷物品，影响仅为产出不该可选的颜色）。

### 界面性能

**M17. WeaponWorkbenchScreen 每帧多次全量 stream + sort 全部配方** — `client/WeaponWorkbenchScreen.java:77-93,506-521`
`clientVisibleTabs` 对全部 tab 排序且每 tab 内再对全部配方排序 + 进度查询，`renderTooltip`/`renderLabels`/`clampState` 每帧各来一遍；`hasIngredients` 每次复制整个背包（约 41 个 ItemStack）× 每帧约 9 次。配方多的整合包中开工作台明显掉帧。

**M18. SubWeaponItemRenderer 每帧每个副武器物品新建实体** — `client/SubWeaponItemRenderer.java:68`
手持/掉落物/展示框路径每帧 `AbstractSubWeaponEntity.create(...)`（含 SynchedEntityData 初始化、UUID 生成），多人同屏手持时 GC 压力放大。原版按 EntityType 缓存渲染实体。

---

## 三、低危问题（摘要，按子系统分组）

### 网络与玩家状态
- `player/PlayerInfo.java:108`：NBT `InkType` 只写不读（死字段，由 inkBand 派生）。
- PlayerInfo attachment `copyOnDeath` 连 `squid` 一起复制，重生/克隆不重置——鱿鱼形态下死亡，重生后仍是鱿鱼状态。
- `SplatcraftPlayerInfoEvents.java:70-81`：`onStartTracking` 向所有追踪者全量广播而非只发新追踪者（带宽浪费）。

### 实体
- `entity/InkProjectileEntity.java:642-656`：每 tick 多次 `JsonObject.deepCopy()`（trail/gravity 等取值路径），弹幕密集时 GC 压力。
- `entity/sub/SplatBombEntity.java:29-35`：`lifetimeTicks = Integer.MAX_VALUE` 且引信只在 `onGround` 累积，卡住的炸弹永不引爆（边缘实体泄漏）。
- 弹体入水 `discard()` 无消散粒子，与子武器入水行为不一致（视觉细节）。
- SquidBumper 被附加原版会跳过的墨水覆盖层数据（无 `handleInkOverlay` opt-out）。
- `blockentity/SpawnPadBlockEntity.java:22-42`：chunk 重载窗口期 BE ticker 先于实体加载运行，可能短暂产生重复 SpawnShield；旧 shield 靠自身 tick 自愈，但 `kill()` 被覆盖为绑定期不可 kill，边界 chunk 长期 block-ticking 时残留不可 `/kill` 的实体。

### 世界墨水
- `worldink/InkBlockUtils.java:42-61`：`inkBlock` 判定顺序与原版颠倒（SpawnShield → 特殊方块 → isUninkable），泡水/inkproof 的 splat switch/canvas/crate 仍会被激活/上色；原版 `PASS` 结果枚举被删，crate 固定返回 `FAIL` 不计入爆炸统计。
- `worldink/WorldInkHandler.java:82-86`：NeighborNotify 清墨丢失破碎粒子（原版有 `addInkDestroyParticle`）。
- `worldink/WorldInk.java:137-163`：`replaceColor` 修改数据但从不同步客户端——当前无人调用（死代码），一旦启用即客户端脱同步。
- `worldink/WorldInkStorage.java:18-19`：铺墨路径 `getChunkAt` 可同步加载区块（原版相同，但 H5 的球形收集使跨界概率升高）；`activeInkChunksNearPlayers` 用 `ArrayList.contains` 去重 O(n²)。

### 武器与物品
- `ColorChangerItem.java:46-48`、`SquidBumperItem.java:49-51`、`ColoredArmorItem.java:41-44`：每 tick 无条件 `setColor`（无相等性检查），每次 `CustomData.update` 新建 tag 标脏 → 槽位每 tick 网络重同步（`WeaponItem`/`ColoredBlockItem` 有正确的守卫模式可对齐）。
- `WeaponItem.use:187-194`：不可达死代码分支。
- `SubWeaponItem.useFromStack:100`：抛投音效在 `isClientSide` 块外，双端重叠播放。

### 命令与数据
- `SplatcraftSuperJumpCommand.java:70-73,96-98`：`noPhysics` 只改服务端（客户端移动权威，穿墙实际无效、可能 rubber-band）；目标实体未校验维度，跨维度时朝错误坐标飞。
- `SplatcraftScanTurfCommand.java:180-186`：对未参战玩家也触发 SCAN_TURF 进度。
- `data/Stage.java:192-196`：损坏 stage 数据静默回退 (0,0,0)/overworld，不打日志。
- `data/SplatcraftSaveData.java`：1.18 存档的 stage/colorScores（ForgeCaps）不迁移，升级世界静默丢失（跨大版本可接受，建议文档明示）。
- `registry/SplatcraftGameRules.java:72-91`：局部 gamerule 每次 stream 全部 stage（铺墨/伤害热路径），stage 多时可测热点。
- `registry/SplatcraftBlocks.java:28`：`CORALITE_STAIRS` 注册回调内直接 `CORALITE.get()`，依赖字段顺序，脆弱。
- `Splatcraft.java:132`：仅注册 CLIENT config，但 `SplatcraftConfig` 被 common 类（`InkTankItem.java:140,301`）引用——当前仅客户端渲染路径调用无碍，未来任何服务端路径调用即抛异常。
- `criteria/ChangeInkColorTrigger.java:68-75`：颜色匹配不做 RL 规范化，数据包写 `"orange"`（省略命名空间）的进度永不触发。
- loot 概率/数量 off-by-one 与 CrateFeature 覆盖非可替换方块：与原版逐字一致，属忠实移植的历史行为，非回归。

### 方块与配方
- `menu/InkVatMenu.java:176-225`：输出槽 quickMove"先给后扣"顺序脆弱（当前不变量保证不可利用，改动易引入刷物品）。
- `block/BlockStateCompatBlocks.java`：多处 `updateShape` 中带副作用（Canvas/SplatSwitch 直接 `setBlock`:660-677,2453-2469；Crate `stateIndex()` getter 内 `setColor`+发包:960-973）——违反无副作用约定，惯例应 `scheduleTick`。
- `BlockStateCompatBlocks.java:2621-2649`：StageVoid 非玩家实体在客户端也 `discard()`（无 isClientSide 判定），短暂 ghost/desync。
- `BlockStateCompatBlocks.java:1554-1563`：Tarp/GlassCover 非玩家破坏（爆炸/流体）只掉 1 个，多面放置的其余面物品丢失。
- `BlockStateCompatBlocks.java:2064-2077`：SpawnPad edge 可被爆炸单独摧毁留下结构缺口（pad 继续工作但 3x3 残缺，无自动修复）。
- `BlockStateCompatBlocks.java:807-829`：旧版 InkedBlock 还原用 flag 2（不触发邻居更新），依附的火把等不响应。
- StageBarrier/ColoredBarrier/inkwell 每方块每 tick 全量实体 AABB 查询（双侧执行，连片使用时热点；原版同设计）。
- `recipe/WeaponWorkbenchTabRecipe.java:23`：tab `name` 按 literal 解析不再自动翻译（内置 JSON 未写 name + 客户端有回退，仅影响第三方数据包）。
- loot table `ink_stained_wool.json` 等：掉落只拷 `Color` 不拷 `Inverted`，与 pick-block 不一致。
- `menu/WeaponWorkbenchMenu.java`：双参构造器 `ContainerLevelAccess.NULL`（当前死代码，误用即失去距离校验）；`consumeIngredients` 遍历全部 41 槽，穿戴中盔甲可被当材料消耗（与原版一致）；count>1 产物只 award 1 次统计；不存在的 advancement id 视为已解锁（拼错即静默解锁）。

### 客户端与 Mixin
- `client/ClientAdvancementAccess.java:51-63`：反射扫描"第一个 Map 字段"代替 accessor mixin，对 loader 补丁/其他 mod 脆弱；失败 fail-open 仅影响客户端展示（服务端有复核，无法作弊）。
- `client/WeaponWorkbenchScreen.java:71`：`tickTime++` 在 `render()` 中，原料轮换动画速度随 FPS 变化（应放 `containerTick`）。
- `client/InkAccessoryLayer.java:58-62`：每帧每玩家 3-4 次 ResourceManager 线性查找，应缓存。
- `client/PlayerInkColoredSkinLayer.java:58`：`NativeImage.read` 返回值未 close，确定性 native 内存泄漏（每登录一次，量小）。
- `mixin/GuiGraphicsMixin.java:17`：用 `@Redirect` 而非项目已依赖的 MixinExtras `@WrapOperation`，与其他 mod redirect 同一调用点会硬冲突。
- `mixin/SquidPassthroughMixin.java:19-41`：注入实体碰撞最热路径（每次碰撞查询执行 tag 检查），多实体场景可测量；注入本身正确。
- `mixin/EntityMixin.java:27-34`：`setSprinting` HEAD 注入内再调 `setSprinting(false)` 受控递归，依赖隐式约定；`hasSplatcraftWeaponCooldown` 每 tick 全背包扫描。
- `client/SplatcraftClient.java:616-651`：每帧改写用户 `bobView` 真实设置项再恢复，渲染异常跳过恢复或期间 `options.save()` 会持久改掉玩家设置；应改用 ViewportEvent 系或至少 try/finally。

---

### 低危处置结果（2026-07-27）

- 网络与玩家状态：移除未读取的 `InkType` NBT；重生清除鱿鱼形态并刷新尺寸；追踪同步只发送给新追踪者。
- 实体：消除弹体每 tick 的 JSON 深拷贝；炸弹增加 30 秒寿命上限；入水消散改为服务端事件；Squid Bumper 不再接收墨水覆盖层；Spawn Pad 对 chunk 重载窗口延迟重建并可清理失效护盾。
- 世界墨水：恢复不可染色方块的判定优先级、Crate 正确计入普通铺墨流程、邻居清墨粒子与 `replaceColor` 同步；只访问已加载 chunk，并移除 Stage 局部规则查询的 stream 热路径。
- 物品、命令与数据：颜色组件仅在实际变化时写入；副武器音效只由服务端播放；清除不可达武器分支；Super Jump 同步物理状态并拒绝跨维度目标；Scan Turf 只统计参战者；损坏 Stage 数据会告警；颜色 advancement 规范化资源位置；旧 ForgeCaps Stage/颜色计分数据不迁移已在 `docs/migration.md` 明示。
- 方块、配方与战利品：方块形状更新改为延迟 tick；旧 InkedBlock 还原通知邻居；多面覆盖物按面数掉落；Spawn Pad 边缘损毁会清理中心；Stage Void 实体处理仅服务端执行；工作台扣料边界、统计、advancement 与翻译键均已收紧；有色方块掉落保留 `Inverted`。
- 客户端与 Mixin：进度读取改为 accessor；工作台动画随游戏 tick 更新；饰品资源查找缓存并在登出和资源包重载时失效；皮肤图像由 `DynamicTexture` 接管并在超尺寸拒绝路径显式关闭；Mixin 冲突风险、鱿鱼碰撞热路径、冲刺递归和 bobView 异常恢复均已处理。后续实测发现漏迁移的玩家鱿鱼替代渲染已补回：未潜入同色墨水时显示墨鱼模型，潜入时保持原版隐藏行为。
- 经复核接受：`CORALITE` 在同一 `DeferredRegister` 中先注册基块再创建楼梯，当前注册顺序确定；替换为无关基态会改变 `StairBlock` 的基态语义。墨水缸输出 shift-click 在服务端单线程中先按当前原料计算 `craftable`，随后按完全相同数量扣料，不存在失败后保留产物的路径。墨水罐配置读取仅在物品栏耐久条的客户端呈现回调中发生，未进入专用服务器逻辑。

## 四、有意的行为改变（非缺陷，需知情确认）

1. **世界墨水渲染方案**：原版 mixin 烘进 chunk mesh（贴合方块形状、不透明）→ 重写版每帧半透明覆盖面（normal alpha=210、clear=96）。观感与性能特征均不同（相关缺陷见 M3-M6）。
2. **副武器热键**：不再做真实 offhand 物品交换（原版 `SwapSlotWithOffhandPacket`），改为服务端按槽位直接驱动（`UseSubWeaponPayload`）。目的等价，但第一人称表现和与其他 mod 的 offhand 交互不同。
3. **charge 网络协议**：`ReleaseChargePacket`/`UpdateChargeStatePacket` 取消，蓄力改服务端 use-tick 权威推导——这是文档第 19 节建议方向，属**安全改进**。
4. **`UpdateColorScoresPacket` 无对应**：criteria 只在服务端。理论无碍（objective 同步不需 criteria 对象），但需运行验证专服下 InkVat omni filter 颜色列表正确。
5. **自定义 Brigadier 参数类型未迁移**（`splatcraft:ink_color`、`color_criterion`→字符串+补全）：依赖原参数类型的外部数据包/命令方块失效。
6. **InkColor 从自定义 registry 变为静态数据类**：颜色零缺失，但第三方 addon 注册新颜色的扩展点消失。
7. **Sodium/Embeddium 兼容层有意放弃**：新渲染方案不注入 chunk 管线，理论上兼容，但未实测。
8. **SpawnPad 为 3×3（中心+8 edge）**：与原版代码一致（分析文档"2x2"描述不准）。

## 五、已验证的安全改进（相对原版）

- charge 服务端权威，无任何携带 charge 数值的网络包，改包无法瞬间满蓄力。
- 全部 11 个 payload handler 正确 `enqueueWork` + side 隔离；dedicated server 无 client 类加载路径（`PlayerSkinOverlayPayload` 用可注入 clientHandler 模式）。
- `UpdateBlockColorPayload` 校验菜单归属/spectator/stillValid/pointer 边界/颜色-配方一致性（原版无任何校验）。
- 工作台合成：服务端完整复核配方/子类型边界/advancement/两阶段模拟-提交原子扣料/先扣后给；修复了原版部分扣料不回滚的物品销毁 bug。
- Dualie dodge/副武器包：spectator/isUsingItem/Float.isFinite/类型/±1 限幅校验齐全。
- 皮肤上传身份校验（不能冒名）、C2S 字节数组限长、登出清理缓存。
- 墨水消耗一律"先扣后射"，容量严格 clamp，settings 缺失安全降级（EMPTY fallback + `Math.max(1,…)` 防除零）。
- 局部坐标 `Math.floorMod`（负坐标经典 bug 未发生）、NBT 全面对称、衰减防 CME、墨水同步改定向发送（消除原版 INK_CACHE 泄漏与全维度广播）。
- 命令权限全部 `hasPermission(2)`；stage 版命令修复原版跨维度 bug。
- 9 个 mixin 注入点全部对照 1.21.1 源码验证有效（但受 H1 影响生产环境不加载）。

---

## 六、修复优先级建议

| 优先级 | 项目 | 工作量 |
|---|---|---|
| P0 | H1 mods.toml 加 `[[mixins]]` | 一行 |
| P0 | H2 InkVat onRemove 前清空输出槽 | 数行 |
| P0 | H3 计分板 criteria 重建提前到 scoreboard 加载前 | 小 |
| P1 | H4 大范围命令接入体积上限 + 只遍历已加载区块 | 小 |
| P1 | H5 InkExplosion 恢复射线遮挡 | 中 |
| P1 | M1 无敌帧清零限定 colored 实体；M3 删双重注册 | 极小 |
| P2 | M7 衰减采样范围、M8/M9 网络防护、M10 InkVat 重置回归、M13 tag 解析保护 | 小 |
| P2 | M4-M6 世界墨水渲染重构（烘焙/缓存方案） | 大 |
| P3 | 其余中低危逐项清理 | 按需 |

> 注：H2、M11 及第三节标注"继承"的条目在原版 1.18.2 中同样存在，修复它们属于超越原版的改进而非回归修复。
