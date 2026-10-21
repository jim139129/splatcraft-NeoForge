# 移植代码补充审查（2026-10-22）

审查基线：`52c136f`，Minecraft 1.21.1 / NeoForge 21.1.248。

本轮重点检查容器交易、客户端到服务端网络包、副武器运动、变形与蓄力、世界墨水同步，并对照本机 Minecraft/NeoForge 源码与 `splatcraft-forge` 的 1.18.2 实现。以下问题描述和行号保留审查时的基线；修复状态及实际验证结果见下节。不宣称覆盖了全部代码路径。

## 修复状态（2026-10-22）

修复版本：`5.1.1`。版本递增后重新执行离线 `build`，构建及资源校验通过；发布 JAR 中的版本为 `5.1.1`，不包含 GameTest 类或测试结构。

六项发现均已修复，并额外修复了 GameTest 暴露的区块卸载/关服卡死：

- 墨水缸输出的数字键和副手交换改用 `Slot.safeTake`，经过与普通取物相同的扣料和统计路径；取出前重算预览。
- 颜色修改包先核对当前菜单及其绑定坐标、区块加载状态和交互有效性，再访问方块实体。
- 斯普拉炸弹和冰壶炸弹的投掷位移通过 `Entity.move` 解析方块碰撞，维护落地状态；静止时检测支撑，移除支撑后恢复下落。斯普拉炸弹低速碰地会落稳，冰壶恢复原版仅撞天花板时竖直反弹的行为。
- 吸盘炸弹沿命中面外法线偏移，保证爆心位于方块外。
- 切换鱿鱼形态时先释放正在使用的蓄力武器以保存服务端蓄力，然后结束使用；背包 tick 保留按配置递减的保存期限。
- 墨水增量包仅刷新本包中的变更位置；全量包仍刷新旧、新位置的并集。
- **额外修复**：`InkSurfaceEffects.replaceInkSurfaceSound` 在读表面信息前检查区块是否仍加载。首轮 GameTest 退出时，通过线程栈确认了 `BeaconBlockEntity.setRemoved -> 播放音效 -> 墨水表面查询 -> 请求正在卸载的区块` 的等待链；修复后正常保存并退出。

新增独立的 `gameTest` 源集，测试代码及结构模板不进入发布 JAR。运行命令：

```powershell
.\gradlew.bat --gradle-user-home F:\neomod\.gradle-user-home --offline runGameTestServer build --console=plain
```

最终结果：`BUILD SUCCESSFUL`，**12 / 12 GameTest 通过**，资源校验通过。测试覆盖输出交换、普通半栈取物、背包仅余一个空间的 Shift 取物、自动化模拟/提交、非法与合法颜色包、多种高度的斯普拉炸弹落地起爆、冰壶落稳及失去支撑、吸盘六面附着及邻格铺墨、两类武器蓄力保存/过期、5000 条墨水记录的增量/全量更新、未加载区块的音效处理。服务端日志确认所有维度保存完成并正常退出。

尚未进行图形客户端的人工观感测试或多人性能采样。

## 1. [P1] 墨水缸输出可通过数字键或副手交换免费取走

位置：`src/main/java/net/splatcraft/neoforge/menu/InkVatMenu.java:383`，`OutputSlot.onTake`；关联 `blockentity/InkVatBlockEntity.java` 的 `removeItem` / `removeOutput`。

输出槽存放的是未扣料的预览物品。目前普通取物通过 `removeItem(4, ...) -> removeOutput -> consumeIngredients` 扣料，Shift 点击另行扣料，但 `OutputSlot.onTake` 只增加统计并刷新配方。

Minecraft `AbstractContainerMenu.doClick` 的 `ClickType.SWAP` 在目标快捷栏为空时直接执行：

```text
inventory.setItem(button, slot.getItem())
slot.onSwapCraft(count)
slot.setByPlayer(ItemStack.EMPTY)
slot.onTake(player, originalStack)
```

该路径不调用 `Slot.remove`，本项目也未重写 `onSwapCraft` 扣料。因此取出的预览变成真实物品，而原料保持不变；`onTake` 又立即生成下一份预览。副手交换的槽位 40 走相同路径。

回归步骤：生存模式放入墨囊、能量蛋、空墨水瓶并选色；留一个空快捷栏，鼠标指向输出槽按对应数字键；检查产物已进入背包，但原料未减少。移走快捷栏产物后可以重复。

修复方向：统一输出交易的扣料入口，覆盖普通取物、Shift 点击、数字键、副手交换及自动化提取，同时防止重复扣料。仅在现有 `onTake` 中无条件再扣一次会破坏普通取物路径。

## 2. [P1] 颜色修改包在权限/归属校验前加载任意区块

位置：`src/main/java/net/splatcraft/neoforge/network/payload/UpdateBlockColorPayload.java:42`。

处理器只检查玩家类型和旁观模式，就执行 `player.level().getBlockEntity(payload.pos)`；墨水缸菜单、对象归属及距离检查都在之后。此包注册为 `playToServer`，坐标由客户端提供。

Minecraft `Level.getBlockEntity` 在服务端主线程调用 `getChunkAt`，最终请求 FULL 区块。因此，普通已连接客户端即使没有打开墨水缸，也能通过包内未加载的有效高度坐标触发区块加载，未生成区块还会触发生成。后续菜单校验不能撤销已经发生的加载；重复不同坐标会占用服务端主线程、区块生成资源与存储。

回归验证：在隔离测试服记录区块加载事件，用普通玩家发送一次指向远处未加载区块的颜色修改包；菜单未打开时应在访问世界前直接拒绝，且不应产生区块加载。

修复方向：先验证当前菜单、菜单绑定方块坐标及距离，再使用不触发加载的区块查询。最好从已验证的菜单取得墨水缸对象，而非先信任包内坐标查询世界。

## 3. [P1] 斯普拉炸弹不更新落地标志，正常引信不会计时

位置：`src/main/java/net/splatcraft/neoforge/entity/sub/SplatBombEntity.java:30`，关联该类 `onHitBlock` 与 `AbstractSubWeaponEntity.tick`。

`fuseTime` 仅在 `onGround()` 为真时递增，但当前继承的 `ThrowableProjectile.tick` 使用射线检测和 `setPos` 移动，不执行产生落地状态的 `Entity.move`。本项目的方块碰撞回调只修改速度，也没有设置落地状态。

新投出的炸弹因此一直保持初始的未落地状态，无法正常累计配置中的 20 tick 引信，只能等待新增的 600 tick 总寿命上限到期，或因其他原因移除。1.18.2 原版的 `SplatBombEntity.tick` 明确调用了 `move(MoverType.SELF, ...)`，移植时遗漏了这部分运动逻辑。

回归步骤：向普通石头平地投掷斯普拉炸弹，观察落地后的引信计时与闪烁；应按配置起爆，而不是等到投出约 30 秒。

修复方向：恢复能维护碰撞与落地状态的移动逻辑，并验证反弹、静止及离开平台时的状态变化。冰壶炸弹也依赖 `onGround()` 控制重力和摩擦，修复公共运动机制时应一并验证。

## 4. [P1] 吸盘炸弹贴进实体方块内部，爆炸铺墨和伤害被自身遮挡

位置：`src/main/java/net/splatcraft/neoforge/entity/sub/SuctionBombEntity.java:70`。

命中面的法线指向方块外侧，但代码将炸弹定位为 `hitLocation.subtract(normal * 0.05)`，因此位于碰撞面内侧。例如石头上表面位于 y=1 时，炸弹中心变成 y=0.95。

`AbstractSubWeaponEntity.explode` 以该位置作为爆心；当前 `InkExplosion.collectAffectedBlocks` 只采纳各射线的首个碰撞。普通完整石块中的爆心会使射线首先命中附着石块，周围方块无法正常铺墨；伤害使用的 `Explosion.getSeenPercent` 从目标采样点射向同一个内部爆心，同样会被石块挡住。该问题与已修复的爆炸遮挡逻辑直接相互作用。

回归步骤：把吸盘炸弹贴在普通石墙或石头地面上，在暴露的一侧放置不同墨色的生物或玩家，检查周围铺墨范围和爆炸伤害。

修复方向：将炸弹/爆心置于命中面的外侧，例如沿外法线正向偏移；分别验证墙面、地面、天花板和非完整方块。

## 5. [P2] 变形保存蓄力的回调被绕过，保存后也会立即清空

位置：`src/main/java/net/splatcraft/neoforge/player/SquidFormEvents.java:76` 和 `src/main/java/net/splatcraft/neoforge/item/WeaponItem.java:105`。

蓄力保存只在 `WeaponItem.releaseUsing` / `releaseSplatling` 检测到鱿鱼形态时执行，但变形 tick 调用的是 `player.stopUsingItem()`。Minecraft 的 `stopUsingItem` 不调用物品 `releaseUsing`；它清除使用状态，和 `releaseUsingItem` 的语义不同。本项目未实现相应的 `onStopUsing` 保存逻辑。

即使客户端松键包与变形包的时序使保存分支得到执行，下一次 `WeaponItem.inventoryTick` 遇到“鱿鱼形态且未使用物品”仍会无条件 `clearStoredCharge(stack)`。配置中的保存期限因此无法正常发挥作用，例如斯普拉蓄力狙击枪的 25 tick 和鹦鹉螺的 66 tick。

回归步骤：把支持保存的武器蓄满，保持右键时变成鱿鱼，在保存期限内解除变形并尝试使用已保存蓄力；同时覆盖变形后松键的时序。

修复方向：在清除使用状态前显式保存服务端计算的蓄力，保留按配置递减的存储计时；取消鱿鱼形态下每 tick 无条件清空的逻辑。

## 6. [P2] 单格墨水增量包触发整个区块墨水位置的更新

位置：`src/main/java/net/splatcraft/neoforge/network/payload/WorldInkPayload.java:43`。

处理器无论 `replace` 是全量还是增量，都会先复制该区块全部已有墨水位置，合并本包位置，再逐项调用 `level.sendBlockUpdated`。而 `WorldInkSync.sendUpdate` 对每次单格变化单独发送增量包。

因此，一个已有 N 个墨水位置的区块收到 K 次单格更新，就产生约 K×N 次位置更新和方块查询。连续新增 N 个位置的累计处理量也从线性变成平方级。渲染刷新可能在后续阶段合并，但这里的集合复制、遍历和查询已经发生，涂色密集的战斗会放大客户端主线程开销。

回归验证：在有大量墨水的区块中更新一个位置，记录处理器的 `sendBlockUpdated` 调用次数；增量更新应仅涉及本包变更位置及确有必要的邻居，而不是所有旧墨水位置。实际帧率影响仍需游戏内性能采样。

修复方向：只在 `replace=true` 时取旧、新位置的并集，增量包仅更新其携带的位置。

## 初次审查时的校验记录

- 执行 `gradlew.bat --gradle-user-home F:\neomod\.gradle-user-home compileJava validateSplatcraftResources --console=plain`，结果为 `BUILD SUCCESSFUL`；`compileJava` 为 `UP-TO-DATE`。
- 资源校验通过：128 个物品、37 个方块、8 个方块实体、2 个菜单、8 个实体、4 个进度触发器、35 套武器设置。
- 对照了 Minecraft/NeoForge 的容器 SWAP 路径、区块查询、投掷物移动、停止使用物品及射线遮挡实现。
- 已排除“普通漏斗拒收必然吞料”这一候选问题：当前 `SplatcraftCapabilities` 已注册 `SidedInvWrapper`，标准漏斗走模拟后提交路径。不能直接把原版 Container 回滚问题重复报为现存 bug。
- 编译与资源校验不会覆盖上述运行时行为。未运行客户端、独立服务端或 GameTest；未修改业务源码。
