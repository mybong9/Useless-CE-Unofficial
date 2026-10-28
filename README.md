# Useless-CE-Unofficial

This is an **unofficial Forge 1.20.1 port/community edition**, not an official upstream release.

Minecraft **1.20.1**, **Forge**, mod ID `useless_mod`, current source version **1.4.1**.

本仓库为该模组的独立源码仓库。请查看 [构建说明 / Building](BUILDING.md)
了解四个仓库的联合构建方式；发布更新记录见 [CHANGELOG.md](CHANGELOG.md)。

<!-- releases:start -->
## 下载 / Releases

最新可安装 JAR、对应源码标签及校验文件见 [GitHub Releases](https://github.com/mybong9/Useless-CE-Unofficial/releases/latest)。
安装前请查看下方依赖版本表；构建用第三方 API JAR 不属于本模组发布文件。
<!-- releases:end -->

<!-- runtime-dependencies:start -->
## 运行环境与依赖版本

对应发布版本：**1.4.1**。本工程目标为 **Minecraft 1.20.1 / Forge**，使用 **Java 17+**（编译目标为 Java 17）。
以下依赖范围直接依据该版本发布 JAR 的 `META-INF/mods.toml`，不是编译时所用依赖版本。

### 必需前置

| 依赖 | Mod ID | 最低版本 / 加载范围 | 适用端 |
| --- | --- | --- | --- |
| Minecraft Forge | `forge` | 47 或更高 | 客户端、服务端 |
| [Useless Core](https://github.com/mybong9/Useless-Core) | `useless_core` | 1.0.14 或更高| 客户端、服务端 |
| Minecraft | `minecraft` | 1.20.1 或更高 | 客户端、服务端 |
| Applied Energistics 2（AE2） | `ae2` | 15.0.0 或更高 | 客户端、服务端 |

### 可选联动

| 依赖 | Mod ID | 最低版本 / 加载范围 | 适用端 |
| --- | --- | --- | --- |
| Just Enough Items（JEI） | `jei` | 15.12.3 或更高 | 仅客户端 |
| Roughly Enough Items（REI） | `roughlyenoughitems` | 12.0.0 或更高| 仅客户端 |
| Mekanism | `mekanism` | 10.4.0 或更高 | 客户端、服务端 |
| Curios API | `curios` | 5.0.0 或更高 | 客户端、服务端 |
| GeckoLib | `geckolib` | 4.0.0 或更高 | 客户端、服务端 |
| Thermal Series / Foundation | `thermal` | 11.0.0 或更高 | 客户端、服务端 |
| CoFH Core | `cofh_core` | 11.0.0 或更高| 客户端、服务端 |
| EMI | `emi` | 1.0.0 或更高 | 客户端、服务端 |
| AdvancedAE | `advancedae` | 1.0.0 或更高 | 客户端、服务端 |
| ExtendedAE | `expatternprovider` | 1.0.0 或更高 | 客户端、服务端 |
| NuclearCraft | `nuclearcraft` | 未限定最低版本 | 客户端、服务端 |
| Blood Magic Additions（血魔法附加） | `bmaddon` | 未限定最低版本 | 客户端、服务端 |
| NeoECOAEExtension | `neoecoae` | 20.5.1 或更高| 客户端、服务端 |

可选联动不要求安装；安装时须符合上表版本范围，并安装该联动模组自身要求的前置。未声明最低版本的其他动态联动不在此处编造版本下限。

MixinExtras 由 Useless Core 内置提供，通常无需单独安装。

Minecraft 的宽松加载声明不代表本工程已支持其他 Minecraft 主版本。开发构建及本地编译 API 清单见 [BUILDING.md](BUILDING.md)。
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
