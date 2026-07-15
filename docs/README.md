# docs 目录
存放项目文档和资源。

## docs/screenshots/
放置 README.md 中引用的产品截图，共 5 张，文件名必须完全一致：

| 序号 | 页面功能 | 文件名 |
|:---:|:---|:---|
| 1 | AI 问答聊天主界面 | `01-ai-chat.png` |
| 2 | 修图建议对比预览 | `02-retouch-compare.png` |
| 3 | 首页（渐变紫色 Banner + 核心功能） | `03-homepage.png` |
| 4 | 照片打分结果页（6维雷达图 + 92 分） | `04-photo-score.png` |
| 5 | 器材选购推荐方案页 | `05-gear-recommend.png` |

## 使用 rename-screenshots.ps1 脚本（推荐）

为了避免手动修改文件名列错顺序，`docs/` 目录下提供了 **PowerShell 批量重命名脚本**，可以自动把你粘贴到 `screenshots/` 下的 5 张截图按正确的顺序改名为上表所需的名字。

### 工作原理

脚本**默认按「文件创建时间 CreationTime 升序」排序**，也就是说你在聊天中按 1→2→3→4→5 的顺序依次保存到本地并粘贴进 `screenshots/` 目录，文件的创建时间正好对应正确的页面顺序，这正是默认排序依据，无需手动调顺序。

### 使用步骤（在项目根目录 `c:\Users\ZhuQian\Documents\trae_projects\sheyingtong 下执行）：

```powershell
# 0. 把聊天中 5 张截图按聊天顺序依次粘贴到 docs\screenshots\ 目录

# 1. 先预览映射关系（安全，不会真重命名）
powershell -ExecutionPolicy Bypass -File docs\rename-screenshots.ps1 -PreviewOnly

# 2. 确认映射无误后，正式执行重命名（会交互式询问确认）
powershell -ExecutionPolicy Bypass -File docs\rename-screenshots.ps1

# 3. 验证文件
dir docs\screenshots\*.png
```

### 常用可选参数

| 参数 | 作用 | 示例 |
|:---|:---|:---|
| `-PreviewOnly` | 只打印映射表预览，不执行重命名（安全模式） | `...ps1 -PreviewOnly` |
| `-NoConfirm` | 跳过交互式 y/N 确认，直接执行 | `...ps1 -NoConfirm` |
| `-Force` | 遇到同名目标文件冲突时直接覆盖（配合 `-NoConfirm`） | `...ps1 -NoConfirm -Force` |
| `-SortBy Name` | 按文件名排序（不按创建时间） | `...ps1 -SortBy Name` |
| `-SortBy LastWriteTime` | 按修改时间排序 | `...ps1 -SortBy LastWriteTime` |
| `-Descending` | 排序倒序 | `...ps1 -Descending` |
| `-PreserveExtension` | 保留原图扩展名（默认强制重命名为 `.png`） | `...ps1 -PreserveExtension` |

### 冲突处理

如果目标文件名已存在且不是当前文件本身，脚本会逐个询问：
- 输入 `o` → **Overwrite** 覆盖旧文件
- 输入 `s` → **Skip** 跳过重命名
- 输入 `r` → **Rename** 在目标名后追加 `-1`、`-2`… 后缀避免冲突

### 退出码含义

| 退出码 | 含义 |
|:---:|:---|
| 0 | 全部成功（或 `-PreviewOnly` 预览模式正常结束） |
| 1 | `docs/screenshots/` 目录不存在 |
| 2 | 目录下没有找到任何图片文件 |
| 3 | 有 ≥1 个文件重命名失败 |
| 4 | 全部跳过，0 个成功 |
