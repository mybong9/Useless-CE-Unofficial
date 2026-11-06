# Useless-CE-Unofficial

This is an **unofficial Forge 1.20.1 port/community edition**, not an official upstream release.

Minecraft **1.20.1**, **Forge**, mod ID `useless_mod`.

本仓库为该模组的独立源码仓库。请查看 [构建说明 / Building](BUILDING.md)
了解四个仓库的联合构建方式；发布更新记录见 [CHANGELOG.md](CHANGELOG.md)。

<!-- releases:start -->
## 下载 / Releases

最新可安装 JAR、对应源码标签及校验文件见 [GitHub Releases](https://github.com/mybong9/Useless-CE-Unofficial/releases/latest)。
安装前请查看下方依赖版本表；构建用第三方 API JAR 不属于本模组发布文件。
<!-- releases:end -->

<!-- runtime-dependencies:start -->
## 运行环境与依赖版本

对应发布版本：**1.4.10**。Minecraft **1.20.1 / Forge**，Java **17+**。
以下范围依据发布 JAR 的加载声明，其他模组仍需满足各自前置要求。

### 必需前置

| Mod ID | 版本范围 | 适用端 |
| --- | --- | --- |
| `forge` | `[47,)` | 客户端、服务端 |
| `useless_core` | `[1.0.14,)` | 客户端、服务端 |
| `minecraft` | `[1.20.1,)` | 客户端、服务端 |
| `ae2` | `[15.0.0,)` | 客户端、服务端 |

### 可选联动

| Mod ID | 版本范围 | 适用端 |
| --- | --- | --- |
| `jei` | `[15.12.3,)` | 仅客户端 |
| `roughlyenoughitems` | `[12.0.0,)` | 仅客户端 |
| `mekanism` | `[10.4.0,)` | 客户端、服务端 |
| `curios` | `[5.0.0,)` | 客户端、服务端 |
| `geckolib` | `[4.0.0,)` | 客户端、服务端 |
| `thermal` | `[11.0.0,)` | 客户端、服务端 |
| `cofh_core` | `[11.0.0,)` | 客户端、服务端 |
| `emi` | `[1.0.0,)` | 客户端、服务端 |
| `advancedae` | `[1.0.0,)` | 客户端、服务端 |
| `expatternprovider` | `[1.0.0,)` | 客户端、服务端 |
| `nuclearcraft` | `[0,)` | 客户端、服务端 |
| `bmaddon` | `[0,)` | 客户端、服务端 |
| `neoecoae` | `[20.5.1,)` | 客户端、服务端 |
| `gtceu` | `[0,)` | 客户端、服务端 |
| `gtlcore` | `[0,)` | 客户端、服务端 |
| `gtmthings` | `[0,)` | 客户端、服务端 |
| `productivebees` | `[0,)` | 客户端、服务端 |
| `extrahnn` | `[0,)` | 客户端、服务端 |
| `hostilenetworks` | `[0,)` | 客户端、服务端 |
| `woot_revived` | `[0,)` | 客户端、服务端 |

`[x,)` 表示最低 x、无上限；`[0,)` 表示未限定最低版本。未在表中列出的其他动态联动仍可存在。

MixinExtras Forge 0.5.5 或更高由 Core 通过 Jar-in-Jar 内置，通常无需单独安装；Core 本身不要求 AE2。
<!-- runtime-dependencies:end -->

## Source and attribution

The Useless Mod/Stretcher port retains the original authors' credit and license notices.
Upstream credit is recorded in the source, existing license files and mod metadata.
Third-party resource licenses remain attached to their respective assets.

Source code, resources and build scripts are included. Game saves, runtime folders,
crash reports, local configuration, caches, credentials and third-party dependency JARs are excluded.

<!-- project-license:start -->
## 许可证 / License

本非官方版本新增及移植修改部分采用 [MIT License](LICENSE)，版权署名为 **Copyright (c) 2026 mybong9**。
上游原代码的完整 MIT 许可证及原版权声明保留在 [licenses/UPSTREAM-MIT.txt](licenses/UPSTREAM-MIT.txt)。
第三方代码、资源和依赖仍保留各自已有的许可证与版权声明；本项目的署名不替代原作者的版权。
<!-- project-license:end -->

## GTM 与资源蜜蜂联动

- GTM 保持可选：万象炉读取物品和流体加工配方，按原耗时及 GT 配置中的 FE/EU 比例计费，保留电路、模具等不消耗输入。概率产物转换为等效批次。
- 手杖模式面板新增 GregTech 分组：普通、扳手、螺丝刀、软锤、撬棍、硬锤。已有个人布局会补入按钮；GT 机器只识别当前选中的工具模式。
- 万象炉外壳或功能外壳位置可放入原生 GT 数据访问仓、光学数据接收仓、算力接收仓和各类维护仓。舱室不能同时归属另一台原生 GT 多方块。
- 研究按 GT 装配线的 `IDataAccessHatch.isRecipeAvailable` 校验，读取数据访问仓及光学数据网络，不以普通 AE 库存代替研究数据。算力接收仓不等同于研究数据仓。
- 洁净室条件读取维护仓实际提供的洁净类型或包围核心的成型洁净室。维度、生物群系、高度、昼夜、雨和雷暴条件也在运行时检查。条件失效时暂停加工。
- 当前不转换尚未表达的 GT 专用资源、随机输入数量、复杂脚本动作和未知配方元数据；不通过丢弃条件强行生成配方。
- 资源蜜蜂新增装瓶、蜜蜂物品转化、方块／流体转化和纯蜂种 Honey Treat孵化配方，可从万象炉 JEI 页面写出万象样板。保留蜂种 NBT、流体标签替代项和可重复使用的蜜蜂模具。
- 修正蜜蜂转化概率的百分比单位，失败尝试不会额外消耗原蜜蜂；钓鱼蜜蜂按概率折算水量和时间，并保留生物群系要求。现有生产、离心、繁殖等联动保留。
- 孵化接入鸡蛋与 100% 纯蜂种 Honey Treat生成对应刷怪蛋的固定操作；笼中蜜蜂成长及任意基因编辑仍由原生孵化器处理。

## HNN / EHNN 模型模板

- 对照原 1.21 无用之物的配方结构，HNN 的模拟室升级按整级合并：一次消耗升到下一级所需的全部矩阵，输出下一等级起点模型。
- 非满级模拟室只保留整级升级配方，满级保留产物模拟配方。
- HNN 和 EHNN 的模板仅使用每种模型刚好到达等级下限的数据值，不枚举每次击杀或模拟产生的中间模型。模型的迭代计数不影响模板匹配；旧版本区间原料仍可读取。
- EHNN 只接入四个同种模型合并，混合生物组合不参与本联动。按当前 EHNN 配置接入模拟建模机整级升级、V1–V4 终极模拟室和终极战利品制造机，保留最低等级与黑名单规则。
- 四合一模型逐项检查四个模型 ID，避免 NBT 列表的宽松匹配把“一个相同、三个不同”的模型误判为同种组合。

资源蜜蜂群系限制可在通用配置 → 万象炉设置 → 配方转换中通过 `productive_bees_biome_restriction` 切换，默认开启；关闭立即生效，不改变配方成本。GTM 同时兼容旧版和 7.5.3 的电压常量路径。



## Woot Revived 联动

Woot Revived（`woot_revived`）为可选联动，无版本上限；本次对照 Forge 1.20.1 的 20.1.1.9 接口。通用配置 `enable_woot_recipe_conversion` 可关闭转换。

- 冥河铁砧使用铁砧与冥河锤作模具；由怪物碎片制作控制器时，要求完整编程并保留输出的生物 NBT。物品／流体灌注机、染料液化机使用对应机器作为模具。染料配方补齐缺少的基础颜色后按原生配置产出纯染料，不把单种颜色当作完整纯染料。
- 附魔液化机模具下，1 本普通书产出 20 B（20,000 mB）附魔流体。不生成附魔书提取配方。耗能沿用 Woot 的单级附魔处理耗能配置，时间按原配置处理速率计算。
- 基础怪物工厂使用工厂之心与已完成编程的碎片作模具，保留生命燃料、召唤材料及基础时间，战利品表按精确有理数折算确定批次。碎片完整 NBT 必须匹配。
- 不自动接入任意附属动态掉落回调、变体和升级模块产物。未知条件／函数不当作无条件奖励；无法表示的分支跳过并记录。当前按未安装升级的基础掉落处理，不提供经验输出。
- 配方由服务器生成并分批同步，联机客户端可用 JEI 与万象样板。客户端和服务器均需更新到本版本以匹配网络协议。

## 原美术资源授权说明

本项目所使用的原美术资源，已获得原作者及美术作者虾比事李跌、鱼蛋、iava 等的使用许可。

原项目：[SorrowMist/UselessMod](https://github.com/SorrowMist/UselessMod)。
