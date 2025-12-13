# Smart Custom Keyboard - 开发文档

本文档旨在为开发者提供项目的技术架构、模块设计及开发路线图。本项目的核心目标是构建一个**具备自适应学习能力**的 Android 键盘。

## 1. 技术架构

项目采用现代 Android 开发技术栈，遵循 MVVM (Model-View-ViewModel) 架构模式，确保代码的可维护性和扩展性。

### 1.1 核心技术
*   **语言**: Kotlin
*   **UI 框架**: Jetpack Compose (用于构建键盘布局和候选词视图)
*   **系统组件**: `InputMethodService` (Android 输入法核心服务)
*   **数据存储**: Room Database (SQLite) - 用于存储用户词库和频率数据
*   **依赖注入**: Hilt (推荐) 或 Koin - 用于管理组件依赖
*   **异步处理**: Kotlin Coroutines & Flow

### 1.2 架构分层
*   **UI Layer (View)**:
    *   `KeyboardService`: 继承自 `InputMethodService`，负责生命周期管理和与系统的 `InputConnection` 交互。
    *   `ComposeKeyboard`: 使用 Compose 构建的键盘界面（按键、布局）。
    *   `CandidateBar`: 候选词显示栏。
*   **Domain Layer (ViewModel/UseCase)**:
    *   `KeyboardViewModel`: 管理键盘状态（大小写、符号模式）、处理按键事件逻辑。
    *   `PredictionUseCase`: 封装预测算法，根据输入流返回候选词列表。
*   **Data Layer (Repository)**:
    *   `UserDictionaryRepository`: 管理用户词库的增删改查。
    *   `LearningRepository`: 负责记录用户输入习惯，更新词频权重。

## 2. 核心模块设计

### 2.1 输入法服务 (IMService)
这是 Android 输入法的入口。
*   **职责**:
    *   初始化 Compose 环境 (`ComposeView`)。
    *   管理 `InputConnection` (提交文本、删除字符、移动光标)。
    *   监听系统事件 (显示/隐藏键盘、输入框类型变化)。
*   **关键点**: 在 `InputMethodService` 中正确集成 Compose 需要处理好 `LifecycleOwner` 和 `SavedStateRegistryOwner`。

### 2.2 智能预测引擎 (Prediction Engine) - **核心差异化功能**
这是本项目实现"越用越好用"的关键。

*   **数据模型**:
    *   **N-gram 模型**: 记录词与词之间的关联概率（例如输入"我"之后，"想"出现的概率）。
    *   **动态权重**: 每个词汇有一个 `weight` 字段。用户每次选中该词，`weight` 增加。
    *   **时间衰减**: (可选) 长期不用的词汇权重随时间降低，确保推荐符合当前习惯。
*   **存储**:
    *   使用 Room 数据库存储 `Word` 实体 (`id`, `text`, `frequency`, `lastUsedTimestamp`)。
*   **学习机制**:
    1.  用户输入拼音/字符 -> 查询数据库 -> 按 `frequency` 降序排列候选词。
    2.  用户点击候选词 -> 提交文本 -> **异步更新**该词在数据库中的 `frequency` (+1)。
    3.  如果用户输入了新词 -> 将新词插入数据库，初始权重设为 1。

### 2.3 键盘 UI (Compose)
*   **布局**: 使用 `Column`, `Row`, `Box` 组合实现 QWERTY 布局。
*   **按键**: 自定义 `Key` Composable，支持点击 (`onClick`) 和长按 (`onLongClick`)。
*   **主题**: 支持动态主题，适应系统深色/浅色模式。

## 3. 开发路线图 (Roadmap)

### Phase 1: 骨架搭建 (Skeleton)
- [ ] 创建 `KeyboardService` 并配置 `AndroidManifest.xml` (BIND_INPUT_METHOD)。
- [ ] 在 Service 中加载一个空的 Compose View。
- [ ] 能够被系统识别并切换为默认输入法。

### Phase 2: 基础输入 (Basic Input)
- [ ] 实现 QWERTY 英文布局 UI。
- [ ] 实现 `InputConnection` 逻辑：点击按键上屏字母，删除键回退。
- [ ] 实现 Shift (大小写切换) 和 符号键盘切换。

### Phase 3: 核心逻辑与候选词 (Core Logic)
- [ ] 实现候选词栏 (Candidate Bar) UI。
- [ ] 建立基础的词库数据结构 (暂时可用内存列表代替)。
- [ ] 实现输入逻辑：输入字符 -> 显示在候选栏 -> 点击上屏。

### Phase 4: 智能学习与持久化 (Intelligence)
- [ ] 集成 Room 数据库。
- [ ] 实现 `UserDictionary` 表结构。
- [ ] **实现学习算法**: 记录用户选择，更新词频。
- [ ] 实现模糊匹配或拼音匹配逻辑 (如果是中文输入)。

### Phase 5: 优化与发布 (Polish)
- [ ] 性能优化：确保键盘弹出速度和打字低延迟。
- [ ] 隐私合规检查：确保无网络请求，数据仅本地存储。
- [ ] UI 美化与动画。

## 4. 隐私与安全
*   **本地优先**: 所有的词库数据、用户习惯数据必须存储在应用私有目录 (`/data/data/...`)。
*   **无网络权限**: 除非必要（如下载离线词库），否则不申请 `INTERNET` 权限，从根源保证隐私安全。

## 5. 参考资料
*   [Create an Input Method (Android Docs)](https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method)
*   [Jetpack Compose Interoperability](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose)
