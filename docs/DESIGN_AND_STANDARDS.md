# 设计模式分析与开发规范

本文档基于 `DEVELOPMENT.md` 的技术架构，详细分析了项目中应采用的设计模式，并制定了相应的代码开发规范，以确保代码质量、可维护性和团队协作效率。

## 1. 设计模式分析 (Design Patterns)

为了实现一个高内聚、低耦合且具备自适应学习能力的键盘应用，我们将采用以下核心设计模式：

### 1.1 架构模式：MVVM (Model-View-ViewModel)
*   **应用场景**: 整个应用的 UI 与逻辑分离。
*   **实现方式**:
    *   **Model**: `UserDictionary` (Room Entity), `PredictionEngine` (业务逻辑)。
    *   **View**: `ComposeKeyboard` (Jetpack Compose UI), `KeyboardService` (作为 View 的容器)。
    *   **ViewModel**: `KeyboardViewModel`。持有 UI State (`KeyboardState`)，处理按键事件，调用 UseCase，并暴露 `StateFlow` 供 View 观察。
*   **优势**: 完美契合 Jetpack Compose 的声明式 UI 范式，易于测试业务逻辑。

### 1.2 结构型模式：Repository Pattern (仓库模式)
*   **应用场景**: 数据层抽象。
*   **实现方式**: `UserDictionaryRepository`。
*   **作用**: 统一管理数据来源（本地数据库、内存缓存、预置资源）。上层业务逻辑（ViewModel/UseCase）不需要关心数据是来自 Room 还是 Assets，也不需要关心具体的 SQL 语句。

### 1.3 行为型模式：Strategy Pattern (策略模式)
*   **应用场景**: **智能预测引擎 (Prediction Engine)**。
*   **实现方式**: 定义 `PredictionStrategy` 接口。
    *   `FrequencyStrategy`: 基于词频的预测（初期实现）。
    *   `TimeDecayStrategy`: 引入时间衰减权重的预测（后期优化）。
    *   `PinyinStrategy`: 拼音匹配策略。
*   **优势**: 允许我们在运行时或配置中动态切换预测算法，方便后期进行 A/B 测试或针对不同语言环境使用不同算法。

### 1.4 行为型模式：Observer Pattern (观察者模式)
*   **应用场景**: 状态管理与数据流。
*   **实现方式**: Kotlin `Flow` / `StateFlow`。
*   **作用**: ViewModel 将键盘状态（如：当前输入的字符、候选词列表、Shift 状态）暴露为流，Compose UI 订阅这些流并自动重组（Recompose）以更新界面。

### 1.5 创建型模式：Dependency Injection (依赖注入)
*   **应用场景**: 全局组件管理。
*   **实现方式**: Hilt / Dagger。
*   **作用**: 自动管理 `Database`, `Repository`, `ViewModel` 的生命周期和依赖关系，避免手动编写大量的 `Factory` 类和单例代码。

---

## 2. 开发规范 (Development Standards)

### 2.1 命名规范 (Naming Conventions)
遵循 Kotlin 官方编码规约。

*   **类名 (Classes)**: PascalCase (大驼峰)，如 `KeyboardViewModel`, `UserDictionaryRepository`。
*   **函数与变量 (Functions & Variables)**: camelCase (小驼峰)，如 `updateCandidates()`, `userFrequency`。
*   **常量 (Constants)**: UPPER_SNAKE_CASE，如 `MAX_CANDIDATE_COUNT`。
*   **Compose 组件**: PascalCase (大驼峰)，且通常是名词，如 `KeyButton`, `CandidateRow`。
    *   *注意*: 即使是函数，只要带有 `@Composable` 注解并返回 UI 单元，也使用 PascalCase。

### 2.2 代码风格 (Code Style)

#### Kotlin
*   **文件组织**: 按照 `常量 -> 属性 -> 构造函数 -> 公共方法 -> 私有方法` 的顺序排列。
*   **可见性**: 默认使用 `private`，仅在必要时开放 `public` 或 `internal`。
*   **数据类**: 优先使用 `data class` 定义模型。

#### Jetpack Compose
*   **状态提升 (State Hoisting)**: 尽量将状态（State）移动到父组件或 ViewModel 中，子组件通过参数接收数据和回调函数（Events）。
    *   *Good*: `fun KeyButton(text: String, onClick: () -> Unit)`
    *   *Bad*: `fun KeyButton()` (内部自己管理点击逻辑)
*   **单一数据源**: 确保 UI 的唯一真实数据源是 ViewModel 中的 `StateFlow`。

### 2.3 架构规范 (Architecture Guidelines)

*   **分层原则**:
    *   `UI Layer` **严禁**直接访问 `Database`。必须通过 `ViewModel` -> `Repository`。
    *   `InputMethodService` 中只处理系统回调和生命周期，复杂的输入逻辑下沉到 `ViewModel` 或 `InputLogic` 类中。
*   **并发处理**:
    *   数据库 I/O 操作 **必须** 在 `Dispatchers.IO` 线程执行。
    *   UI 更新 **必须** 在 `Dispatchers.Main` 线程执行。
    *   使用 `viewModelScope` 管理协程生命周期，防止内存泄漏。

### 2.4 数据库规范 (Database Standards)

*   **表名**: 使用小写蛇形命名，如 `user_dictionaries`。
*   **索引**: 对查询频繁的字段（如 `word_text`, `frequency`）必须建立索引 (`@Index`)，以保证打字时的毫秒级响应。

### 2.5 Git 提交规范 (Git Workflow)

采用 Conventional Commits 规范：

*   `feat`: 新功能 (feature)
*   `fix`: 修补 bug
*   `docs`: 文档 (documentation)
*   `style`: 格式 (不影响代码运行的变动)
*   `refactor`: 重构 (即不是新增功能，也不是修改 bug 的代码变动)
*   `perf`: 性能优化
*   `chore`: 构建过程或辅助工具的变动

**示例**:
*   `feat: implement candidate view UI`
*   `fix: crash when deleting text in empty field`
*   `docs: update architecture diagram`

### 2.6 隐私与安全规范
*   **禁止联网**: 代码中严禁引入 Retrofit/OkHttp 等网络库（除非后续明确添加云同步功能且经过用户授权）。
*   **日志脱敏**: 在 Logcat 中打印日志时，**严禁**打印用户输入的明文内容，可以使用 `*` 代替或仅打印长度。

