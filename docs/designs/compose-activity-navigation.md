# Compose Activity 导航设计

## 背景与目标

当前 Desktop UI 直接在 `Main.kt` 中组合计数器页面，并由独立的 `CounterState` 保存状态。后续页面增多后，需要一种接近 Android Activity 的页面组织方式：一个 Activity 实例同时拥有页面状态、交互逻辑和 Compose 布局，并能在页面之间前进和返回。

本设计只覆盖进程内的 Compose Desktop 页面导航，不引入 Android Activity、路由字符串、参数序列化、持久化恢复或平台安装包配置。

## 设计原则

- 每个页面由一个 `ComposeActivity` 实例表示，状态和 UI 归同一个实例所有。
- 返回栈保存 Activity 实例，而不是页面类型或路由，因此返回上一页时恢复原实例及其状态。
- `Main.kt` 只管理 Desktop 窗口生命周期，并把页面渲染交给 `ActivityManager`。
- 页面通过 CompositionLocal 获取导航器，不需要逐层传递 `ActivityManager` 参数。
- 根页面返回行为默认无动作，页面需要显式注册后才能退出或抛出异常。
- Activity 生命周期绑定到实例在返回栈中的存续，而不是 `Content()` 是否处于 Compose 组合中。

## 组件设计

### `ComposeActivity`

`ComposeActivity` 是所有页面的抽象基类，定义页面的 Compose 渲染入口和可选的生命周期回调：

```kotlin
abstract class ComposeActivity {
    @Composable
    abstract fun Content()

    protected open fun onCreate() {}
    protected open fun onNavigateTo() {}
    protected open fun onNavigatedBack() {}
    protected open fun onDestroy() {}
}
```

四个回调均无参数、不是 Composable，并在 Compose Desktop UI 线程同步执行。它们的语义如下：

- `onCreate()`：Activity 首次进入 manager 时调用一次。初始 Activity 在 `ActivityManager` 创建时调用，新 Activity 在 `navigateTo()` 期间调用。
- `onNavigateTo()`：当前 Activity 即将导航到新 Activity 时调用。Activity 仍保留在返回栈中，不会因此销毁。
- `onNavigatedBack()`：上层 Activity 被返回操作弹出后，原 Activity 重新成为当前页面时调用。
- `onDestroy()`：Activity 被 `navigateBack()`、`setCurrentAsRoot()` 或 manager 销毁永久移出返回栈时调用一次。

Activity 的 Compose 状态作为实例属性保存，例如使用 `mutableIntStateOf`。Activity 被压入返回栈后，其实例一直保留到被弹出、被 `setCurrentAsRoot()` 清除或 manager 销毁，因此页面离开当前画面后状态不会丢失。仅仅离开 Compose 组合不会触发 `onDestroy()`；同一实例从返回栈恢复时也不会再次调用 `onCreate()`。

基类通过仅供 manager 使用的内部派发入口维护实例的生命周期状态，保证 `onCreate()` 和 `onDestroy()` 最多各执行一次。一个 Activity 实例只能加入一个 manager 一次；重复压入仍在栈中的实例，或者重新使用已经销毁的实例，都会被拒绝。生命周期回调不得再次调用 manager 的导航方法，重入时抛出 `IllegalStateException`。

该抽象只提供上述项目内生命周期，不模拟 Android Activity 的完整生命周期、Intent 或系统任务栈。

### `ActivityManager`

`ActivityManager` 持有一个 `SnapshotStateList<ComposeActivity>` 作为返回栈。构造函数接收初始 Activity 和 `onExitRequest` 回调，初始 Activity 始终作为第一个当前页面。

公开操作如下：

- `navigateTo(activity)`：先调用当前 Activity 的 `onNavigateTo()`，再调用新 Activity 的 `onCreate()`，然后把新实例压入栈顶并设为当前页面。
- `navigateBack(): Boolean`：当栈中有多个 Activity 时弹出当前实例，依次调用被弹出实例的 `onDestroy()` 和重新显示实例的 `onNavigatedBack()`，随后返回 `true`；当当前页面已经是根页面时执行该页面注册的根返回行为。
- `setCurrentAsRoot()`：删除当前 Activity 之前的所有栈条目，并对被移除条目按从最接近当前页面到原根页面的顺序调用 `onDestroy()`，最后只保留当前实例。该操作不重新创建当前 Activity，也不对它调用导航回调；当前页面已经是根页面时操作为空。
- `Render()`：通过 `CompositionLocalProvider` 提供当前 manager，并渲染栈顶 Activity 的 `Content()`。
- `dispose()`：按从栈顶到栈底的顺序销毁所有剩余 Activity，并使 manager 进入终止状态。该操作可重复调用，后续调用为空操作。

manager 活跃期间返回栈不允许为空。外部代码不能直接修改列表，只能通过上述导航 API 改变栈状态；`dispose()` 是唯一允许清空返回栈的操作。manager 终止后调用导航 API 或 `Render()` 会抛出 `IllegalStateException`。

生命周期回调抛出的异常直接向上传播，不进行状态回滚。回调属于页面程序逻辑，抛出异常表示不可恢复的编程错误。

### `LocalActivityManager`

`LocalActivityManager` 使用 `staticCompositionLocalOf<ActivityManager>` 定义。`ActivityManager.Render()` 是唯一提供者，Activity 及其子组件可通过 `LocalActivityManager.current` 调用 `navigateTo()`、`navigateBack()` 或 `setCurrentAsRoot()`。

在 provider 外读取该值应立即抛出带有明确说明的错误，避免静默使用无效的默认 manager。

### `RootBackBehavior`

`RootBackBehavior` 定义三种根页面返回策略：

- `NoOp`：默认策略，不执行操作，`navigateBack()` 返回 `false`。
- `Exit`：调用 `ActivityManager` 构造时注入的 `onExitRequest`，随后返回 `true`。
- `Throw`：抛出 `IllegalStateException`，异常消息包含当前 Activity 的类型信息，便于定位错误页面。

非根页面调用 `navigateBack()` 时始终优先弹栈，不执行根返回策略。根页面执行 `NoOp`、`Exit` 或 `Throw` 时不直接触发任何 Activity 生命周期回调；若 `Exit` 随后使窗口关闭，则由 manager 的 `dispose()` 统一调用 `onDestroy()`。

### `RootBackHandler`

页面通过下面的 Composable API 注册自身成为根页面时的返回行为：

```kotlin
@Composable
fun RootBackHandler(behavior: RootBackBehavior)
```

该函数从 `LocalActivityManager.current` 获取 manager，并将策略绑定到当前正在渲染的 Activity 实例。注册使用 `DisposableEffect` 管理：页面进入组合时注册，离开组合时自动注销，防止一个页面的行为泄漏到另一个页面。

同一 Activity 在任意时刻只有一个有效的根返回注册。注册操作返回内部令牌，注销时只移除与该令牌匹配的注册，避免旧组合的延迟清理误删新注册。若当前 Activity 没有有效注册，manager 使用 `NoOp`。

注册行为不要求页面当前已经是根页面。页面可以先注册，之后调用 `setCurrentAsRoot()`；只要该页面仍在组合中，成为根页面后策略立即生效。

## 渲染与导航流程

1. `Main.kt` 在 `application` 组合中通过 `remember` 创建 `ActivityManager(CounterActivity(), onExitRequest = ::exitApplication)`。
2. `Window` 的内容只调用 `manager.Render()`。
3. `Render()` 提供 `LocalActivityManager`，读取栈顶 Activity，并调用其 `Content()`。
4. 当前 Activity 调用 `navigateTo(newActivity)` 后，manager 依次派发当前实例的 `onNavigateTo()` 和新实例的 `onCreate()`，再修改返回栈；`Render()` 随快照状态变更重组并显示新实例。
5. 当前 Activity 调用 `navigateBack()` 后，manager 弹出当前实例并调用其 `onDestroy()`；之前保留的实例重新成为栈顶，收到 `onNavigatedBack()`，其状态保持原值。
6. 当前 Activity 调用 `setCurrentAsRoot()` 后，manager 销毁并移除所有更早实例。此后再次返回会执行当前页面注册的 `RootBackBehavior`。
7. `Main.kt` 使用 `DisposableEffect(manager)` 把窗口组合的销毁连接到 `manager.dispose()`，确保退出应用时销毁栈内剩余实例。

窗口标题、尺寸和系统关闭按钮仍由 `Main.kt` 管理。系统关闭按钮继续直接使用 `::exitApplication`；`RootBackBehavior.Exit` 复用注入到 manager 的同一个退出回调。

## 计数器占位页面迁移

新增 `CounterActivity : ComposeActivity`，替代当前的 `CounterState` 和 `Main.kt` 中的私有 `CounterApp()`：

- `CounterActivity` 持有 `count` 快照状态，初始值为 `0`。
- `increment()` 负责把计数增加 `1`。
- `Content()` 保留现有 Material3 页面：显示当前计数，并提供点击后递增的按钮。
- Material 主题由页面或后续统一的应用主题提供；本次实现不扩展主题系统。

这样占位页面同时承担状态与布局，验证 Activity 实例在导航返回后的状态保留语义。

## 错误与边界处理

- manager 构造后返回栈至少包含初始 Activity，活跃期间不提供清空栈的 API；只有终止 manager 的 `dispose()` 可以清空返回栈。
- 根页面未注册行为时，返回是可观察的无动作，结果为 `false`。
- `Throw` 只用于明确要求根返回视为编程错误的页面，不作为默认策略。
- `setCurrentAsRoot()` 只修改返回栈，不自动修改或注册根返回行为。
- 导航调用假定发生在 Compose Desktop UI 线程；本设计不提供跨线程同步。
- 同一个 Activity 实例不能重复加入返回栈，也不能在销毁后重新加入 manager。
- 生命周期回调期间不能发起嵌套导航；manager 检测到重入时抛出 `IllegalStateException`。
- manager 只在 `dispose()` 后允许返回栈为空；终止后的 manager 不可再次使用。

## 测试设计

导航单元测试覆盖：

- 初始 Activity 是当前页面，返回栈大小为 `1`。
- `navigateTo()` 压入并选中新 Activity。
- `navigateBack()` 弹出当前页面，并恢复之前保留的同一 Activity 实例。
- `setCurrentAsRoot()` 清除之前的条目，同时保留当前实例及其状态。
- 初始 Activity 和新压入的 Activity 分别只收到一次 `onCreate()`。
- `navigateTo()` 按 `onNavigateTo()`、`onCreate()` 的顺序派发回调，保留在栈内的旧页面不收到 `onDestroy()`。
- `navigateBack()` 按被弹出页面的 `onDestroy()`、恢复页面的 `onNavigatedBack()` 顺序派发回调，恢复页面不再次收到 `onCreate()`。
- `setCurrentAsRoot()` 对被清除实例按从最接近当前页面到原根页面的顺序派发 `onDestroy()`，不对保留的当前实例派发回调。
- `dispose()` 按栈顶到栈底销毁所有剩余实例，每个实例只销毁一次；重复调用为空操作。
- 重复压入同一实例、复用已销毁实例、生命周期回调重入导航以及终止后继续使用 manager 都会被拒绝。
- 生命周期回调抛出的异常会直接向调用方传播。
- 根页面未注册行为时使用 `NoOp`，返回 `false`。
- 根页面注册 `Exit` 后调用退出回调，并返回 `true`。
- 根页面注册 `Throw` 后抛出包含当前 Activity 信息的 `IllegalStateException`。
- 三种根返回行为自身都不派发 Activity 生命周期回调。

Compose 组合测试覆盖：

- `RootBackHandler` 在页面离开组合时移除注册，后续页面不会继承该策略。
- Activity 再次进入组合时能重新注册自己的策略。

计数器测试继续覆盖单次递增和连续递增，测试对象改为 `CounterActivity`。构建验证至少运行 `:ui:test`，并确认 Desktop UI 源集能够编译。

## 非目标

本次不实现以下能力：

- Android 或 Kotlin Native 平台支持。
- URL、字符串路由、深层链接或导航参数序列化。
- Activity 状态落盘、进程重启恢复或多窗口状态共享。
- 动画转场、Android 完整生命周期、依赖注入框架或安装包生成。
