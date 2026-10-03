# 替换素材与新增维度

公开源码不包含汉仪字体和录音音效；示例 JSON 保留替换路径，请自行提供有授权的 TTF/OGG。从干净源码运行 `python tools/example_packs.py` 会生成使用 Minecraft 默认字体、无自定义音效的示例包。下文“已带字体/音效”指作者本机开发素材，并非公开仓库中的文件。

适用于 Minecraft 1.21.1、更好的加载界面 1.2.x。示例目录和 ZIP 内容相同，修改目录后运行 `python tools/example_packs.py` 会重新生成示例；该命令也会恢复示例内容，保留自己的修改前请先复制目录。

## 安装

- `loading-resources.zip` 放入客户端 `resourcepacks`，在游戏里启用。包含可直接编辑的 PNG、OGG、TTF 和 JSON。
- `loading-data.zip` 放入世界 `datapacks`，执行 `/reload`。单人世界直接支持；多人服务器需要安装本模组，玩家也需要客户端模组。
- 客户端资源重载用 F3+T。数据包只同步定义和文字，PNG、OGG、TTF 仍由客户端资源包提供；联机可通过服务器资源包分发。
- 不安装服务器模组时，资源包里的全部配置仍独立有效。

## JSON 定义

客户端读取 `assets/<命名空间>/loading_screen/<文件名>.json`，服务器读取 `data/<命名空间>/loading_screen/<文件名>.json`。可放多个 JSON，先按文件路径字典顺序、路径相同时按命名空间排序合并，后合并的定义覆盖前者；同一资源 ID 按 Minecraft 包优先级覆盖。服务端定义最后合并，优先于客户端配置。未写字段保留原值，`dimensions` 逐项合并，`tips` 整个数组替换。坏文件记录日志并跳过，原版默认值作为起点。

```json
{
  "dimensions": {
    "yourmod:moon": {
      "texture": "yourpack:textures/gui/moon.png",
      "width": 256,
      "height": 256,
      "base": 255
    }
  },
  "tips": [
    {"title": "月球", "body": "欢迎来到新维度。"},
    {"title": "yourpack.tip.title", "body": "yourpack.tip.body", "translate": true}
  ],
  "font": "yourpack:ui",
  "sound": "yourpack:arrival"
}
```

`yourmod:moon` 是真实维度 ID，不要求模组提前登记。PNG 放在 `assets/yourpack/textures/gui/moon.png`。未知维度仍使用问号；可用顶层 `fallback` 替换。`elements` 使用同样图标格式，替换整张底部进度图；进度仍从左到右填充。

图标 `width`、`height` 为 PNG 实际尺寸；可选 `u`、`v`、`region_width`、`region_height` 从图集截取区域，默认整张。`base` 是原图灰度基准（1～255）：纯白图标用 255，内置维度图标用 100、元素用 80。透明背景和灰度图适合昼夜自动染色。默认昼夜配色和布局继续生效。

小贴士支持 1～256 条，轮流显示；标题最多 256 字符、正文最多 2048 字符，显示区域最多两行，建议短句。`translate: true` 使用语言文件键名，省略时按原文显示。

## TTF 和音效

`font: "yourpack:ui"` 对应 `assets/yourpack/font/ui.json`：

```json
{"providers":[
  {"type":"ttf","file":"yourpack:ui.ttf","shift":[0,0],"size":9,"oversample":4},
  {"type":"reference","id":"minecraft:default"}
]}
```

将字体放到 `assets/yourpack/font/ui.ttf`。示例已带当前汉仪文黑字体，可替换文件或调整 provider。也可以直接覆盖 `assets/teleport_loading/font/wenhei.ttf` 和 `font/tips.json`，不用改定义。

`sound: "yourpack:arrival"` 对应 `assets/yourpack/sounds.json` 中 `arrival` 事件，再指向 `assets/yourpack/sounds/arrival.ogg`。需 OGG Vorbis；MP3 先转换。示例已包含事件配置和现用音效。每次加载只播放一次，音量和淡出规则保持不变。

直接覆盖内置素材也兼容：`assets/teleport_loading/textures/gui/elements.png`（2172×724）、`dimensions.png`（1254×1254，2×2 图集）、`sounds/loading.ogg`、`sounds.json`、`font/wenhei.ttf`、`font/tips.json`、`lang/zh_cn.json`。改 PNG 尺寸时，应同时提供图标 JSON 中的新尺寸。

示例 `example:moon` 不创建维度，只演示映射。将键改成已有维度 ID 即可。
