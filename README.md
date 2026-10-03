# 更好的加载界面

**Better Loading Screen** — Genshin-inspired teleport and dimension loading screens.

Source: https://github.com/tamikip/better-loading-screen

## 开源与构建 / Open source and build

源码、配置、文档及本项目制作的图片采用 [Apache-2.0](LICENSE)，Copyright 2026 TamikiP。分发时保留许可证及相关声明，修改过的文件需注明变更；协议允许商业使用，也不要求修改后的代码必须开源。

汉仪字体和用户提供的音效不属于此授权，未纳入公开仓库；具体来源和范围见 [THIRD_PARTY.md](THIRD_PARTY.md)。公开源码构建默认使用 Minecraft 字体，不播放缺失的自定义音效。下文关于内嵌字体和音效的记录描述本机开发版本；可用自己有权使用的素材通过资源包恢复或替换。

The original source, configuration, documentation, and project-created artwork are licensed under Apache-2.0. Retain the license and applicable notices, and identify modified files when redistributing. Commercial use is permitted; publishing modifications is not required. Third-party font/audio files are excluded; see [THIRD_PARTY.md](THIRD_PARTY.md).

Install JDK 21 and Gradle 8.14.3, then run `gradle build` from the repository root. Output JARs and a source JAR are written to `build/libs/`. The build runs the loading state, style, definition, and command parsing checks. A clean checkout uses Minecraft's default font and omits the unavailable custom audio. Optional local integration tools in `tools/` require the author's Minecraft instance layout; use Gradle for a portable build.

Minecraft 1.21.1 · NeoForge 21.1.228+ · Java 21 · 客户端必需，服务端可选

检测超过 24 格的位置跳变，以及原版接收地形/切换维度界面。目的地尚未准备好时，显示维度徽记、小贴士和七元素进度；底层世界继续渲染。图片均重新生成，未使用参考截图。

七枚图标从左到右逐渐填充，第一帧从 12% 开始，等待时伪进度上限为 84%。目标附近默认 5×5 个区块的数据已到达、玩家所在区块段已编译、当前渲染队列完成，并持续稳定 350 毫秒后，完成图标填充，以 800 毫秒渐变退场。淡出开始后单向完成，后续渲染队列波动不会恢复遮罩；同一次加载不会重复启动或重置进度。已准备好的同维度传送不强制显示界面；切维度界面始终覆盖。Sodium 使用自身接管的原版就绪接口，Iris 下覆盖层绘制在世界、HUD 和屏幕之后。

1.1.0 按参考比例调整留白：维度徽记在中央偏上，小贴士及两侧细线位于底部，七元素缩小为底部进度。主世界为树与浮岛，下界为堡垒与火焰，末地为龙与水晶，其他维度为问号。以目标维度 `dayTime` 自动配色：13000～22999 tick 为夜间（深灰底、浅金色），其他时段为昼间（纯白底、蓝灰色）。使用游戏时间，不使用电脑时间。三组通用占位小贴士按加载轮换，中英文文案位于语言资源文件，可通过资源包替换。

加载界面文字独立使用汉仪文黑-85W（HYWenHei-85W）无衬线字体，不更改其他界面的字体。原生字号、整数坐标，无额外矩阵缩放；图标启用线性采样并在每次绘制前恢复透明混合，保证未完成图标与已完成部分正确区分。内嵌字体来自 https://github.com/Zamburg1/Genshin-Font-Host ，将仓库的 WOFF2 字体容器转换为 TTF，内部字体名称经校验为 HYWenHei-85W。

配置自动生成于实例 `config/teleport_loading-client.toml`：`enabled`、`teleportDistance`、`chunkRadius`、`minimumDisplayMs`、`stableRenderMs`、`fadeOutMs`、`timeoutMs`。默认至少显示 600 毫秒，30 秒超时后退出，避免区块或网络异常时一直遮住画面。重启游戏生效。

安装 `build/libs/teleport-loading-neoforge-1.21.1-1.2.1.jar` 到实例 `mods`，重启。服务器不需要安装；如需数据包同步则服务端也安装。本模组只覆盖显示，传送、安全落点和世界生成仍由原来的游戏或传送模组负责。

1.2.1 在客户端处理换维度包时提前锁定目的地图标，原版设置世界之前强制绘制的过渡帧也使用目的地素材。屏蔽原版传送门穿越及进入音效，保留传送门环境声和资源包指定的加载音效。自身坐标 `/tp`、`/teleport`（含 `minecraft:` 前缀、`@s`、自身名字、`~`、`^`、`execute in ... run`）在发命令时预检查目标；尚未就绪则立即启动界面，等待服务器响应期间不提前淡出，5 秒未收到位置或换维度包则取消预等待并结束。未知命令和其他传送来源在收到位置包时触发，并先绘制加载帧再处理后续区块数据。目标已就绪时不触发额外加载界面；本功能不减少服务器世界生成所需时间。

1.2.0 支持资源包和数据包 JSON 配置：小贴士不限于内置三条，任意维度 ID 可映射独立 PNG 或图集区域，底部元素图、问号、音效事件和 TTF 字体均可替换。资源包 F3+T 重载；数据包 `/reload` 后同步给已安装模组的玩家，服务器未安装模组时客户端资源包仍生效。服务端定义优先，退出服务器清除同步配置。示例资源包、数据包和字段说明见 [examples/README.md](examples/README.md)。二进制素材通过资源包提供，数据包引用其资源 ID。

1.0.1 加入用户提供的约 3 秒加载音效，转换为 OGG Vorbis。每次加载仅播放一次，不循环；界面提前结束时跟随淡出，退出世界或禁用模组时停止。`loadingSound` 控制开关，`loadingSoundVolume` 控制音量（0～1，默认 0.8），同时遵循游戏主音量。

标准构建：`gradle build`。本机离线开发库构建：`python tools/build.py`，可用 `LOADING_DEV_JAR` 指定同版本开发 jar。`python tools/client_check.py` 在独立测试存档验证远距离传送、主世界/下界/末地/未知维度、昼夜配色、音频、小贴士，以及进度和淡出的连续帧状态，使用当前实例的 Sodium/Iris，输出截图和验证报告。测试专用最短显示时长为 2000 毫秒，方便截取布局，正式默认仍为 600 毫秒。正式 jar 不包含测试模组。
