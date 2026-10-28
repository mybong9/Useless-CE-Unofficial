# 万象炉兼容与样板编码

## NuclearCraft: Neoteric

兼容模组 ID `nuclearcraft`，核对的原生接口为 [NuclearCraft 的 1.20 分支](https://github.com/igentuman/NuclearCraft-Neoteric/tree/1.20)。以下机器的原生配方及使用相同配方类型的魔改配方进入万象目录；将对应机器物品放入模具舱。

| 机器 ID | 机器 ID | 机器 ID |
| --- | --- | --- |
| `fluid_infuser`（流体灌注） | `assembler`（组装） | `nuclear_furnace` |
| `manufactory` | `alloy_smelter` | `centrifuge` |
| `chemical_reactor` | `crystallizer` | `fuel_reprocessor` |
| `decay_hastener` | `electrolyzer` | `extractor` |
| `fluid_enricher` | `ingot_former` | `isotope_separator` |
| `melter` | `pressurizer` | `rock_crusher` |
| `supercooler` | | |

灌注保留物品原料与流体原料，支持标签、数量和原生严格 NBT 条件；产物采用 NuclearCraft 选择的实际物品和流体。时间与能耗按未安装升级时的机器配置及配方倍率换算。禁用的机器不会转换。

反应堆、需要反应堆中子通量的辐照、世界交互、粒子等额外机制不作为普通加工配方转换。一个输入的替代材料若要求不同数量，目前跳过并记录原因，避免按错误数量加工。

配置项：`enable_nuclearcraft_recipe_conversion`，默认开启，修改后重启。

接口依据：[AbstractRecipe](https://github.com/igentuman/NuclearCraft-Neoteric/blob/1.20/src/main/java/igentuman/nc/recipes/AbstractRecipe.java)、[ProcessorPrefab](https://github.com/igentuman/NuclearCraft-Neoteric/blob/1.20/src/main/java/igentuman/nc/content/processors/ProcessorPrefab.java)。

## BloodMagicAdditions

兼容 [BloodMagicAdditions 的 1.20.1 分支](https://github.com/edgemq/BloodMagicAdditions/tree/1.20.1)，模组 ID `bmaddon`，同时需要它自身的 Blood Magic 前置。

万象目录新增血之装配器版本的血之祭坛、炼金桌配方。模具舱需要 `bmaddon:blood_altar_assembler`；二级及以上配方另需对应等级或更高等级的祭坛等级卡。机器和等级卡都作为不消耗的模具。

生命源质按原生配方 LP × 附属配置倍率向上取整，并作为真实流体输入。加工时间、AE/t 使用附属的基础配置，AE 能量按 AE2 的换算规则转为 FE；采用未安装升级时的数值。既有 Blood Magic 转换入口与此入口分别显示。

在 JEI 的万象配方分类中选取该版本，向 AE2 兼容样板终端放入普通 AE 空白样板后点击转移，即可生成完整万象样板。附属自己的血之样板仍属于其原生机器。

配置项：`enable_bmaddon_recipe_conversion`，默认开启，修改后重启。

接口依据：[BloodAltarPatternDetails](https://github.com/edgemq/BloodMagicAdditions/blob/1.20.1/src/main/java/com/edgemq/bmaddon/ae2/BloodAltarPatternDetails.java)、[BMAddonCommonConfig](https://github.com/edgemq/BloodMagicAdditions/blob/1.20.1/src/main/java/com/edgemq/bmaddon/config/BMAddonCommonConfig.java)。

## 气动激光与钻头装配样板

PneumaticCraft 原生 `StackedIngredient` 同时检查物品与堆叠数量；AE 匹配物品类型时使用数量为 1 的代表物品，旧适配器因此拒绝需要多个原料的装配配方。现在将数量独立保存在万象配方中，将已知的堆叠原料转成保持标签的普通物品条件；其他 NBT 自定义条件保持原样。激光、钻头及钻头激光联动装配使用同一修复，机器模具和对应程序模具仍需放入模具舱。

在 JEI 万象配方分类中重新选择对应装配配方，将普通 AE 空白样板放入终端后转移。此前编码失败或已失效的样板需要重新编码。该修复覆盖原生装配类型及使用相同原料结构的魔改配方，包括以无限锭作为原料的配方；尚未获得用户整合包的具体配方 JSON，未对该整合包做实际游戏复现。

实现依据：[AssemblyRecipeImpl](https://github.com/TeamPneumatic/pnc-repressurized/blob/1.20.1/src/main/java/me/desht/pneumaticcraft/common/recipes/machine/AssemblyRecipeImpl.java)、[StackedIngredient](https://github.com/TeamPneumatic/pnc-repressurized/blob/1.20.1/src/api/java/me/desht/pneumaticcraft/api/crafting/ingredient/StackedIngredient.java)，并核对本工程依赖的 6.0.23 JAR。

## 大型配方

Re-Avaritia 原生有序、无序及不消耗催化剂工作台配方改为逐槽检查材料替代项，不再因完整排列组合超过 512 而直接丢弃配方。不同奇点的 NBT、合成返还物及原料数量仍需一致且完整；未知自定义配方类继续保留原有严格检查。

当输入或输出数量超过 64，或原料、产物种类超过终端的可见槽位时，服务端根据所选万象配方直接生成完整样板，消耗一张普通 AE 空白样板。先取走终端输出槽中的旧样板；更新前无效的样板需要从当前万象配方重新编码。

## 验证范围

生产代码的纯逻辑回归检查覆盖气动装配的 1／2／64／128／4096 数量原料与标签正规化、81 格 × 3 种替代材料、返还物 NBT 与数量差异、输入拷贝隔离，以及 NuclearCraft 的取整与大数边界。命令为 `gradlew.bat :useless_mod:omniversalCompatChecks --offline`。原始构建输出存于 `logs/build/`。

编译和这些自动检查不等于 Minecraft 内实际启动、JEI 转移或 AE 网络加工验证；这两项可选兼容尚需在安装相应模组的游戏中检查。
