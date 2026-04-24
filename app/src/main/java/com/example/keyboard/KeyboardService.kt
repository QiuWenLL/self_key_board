package com.example.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.keyboard.ui.KeyboardScreen
import com.example.keyboard.ui.theme.KeyboardTheme

class KeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val savedStateRegistryController by lazy { SavedStateRegistryController.create(this) }
    private val store by lazy { ViewModelStore() }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onCreateInputView(): View {
        val view = ComposeView(this)
        
        // 关键修复：除了给 ComposeView 设置外，还需要给 IME 的 Window DecorView 设置 LifecycleOwner
        // 因为 Compose 内部（例如 Popup/Dialog 或者某些测量流程）会往上追溯寻找 Lifecycle
        val decorView = window.window?.decorView
        decorView?.setViewTreeLifecycleOwner(this)
        decorView?.setViewTreeViewModelStoreOwner(this)
        decorView?.setViewTreeSavedStateRegistryOwner(this)
        
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        
        view.setContent {
            KeyboardTheme {
                KeyboardScreen(
                    onKeyPress = { key ->
                        currentInputConnection?.commitText(key, 1)
                    },
                    onDelete = {
                        currentInputConnection?.deleteSurroundingText(1, 0)
                    },
                    onAction = { keyCode ->
                        currentInputConnection?.sendKeyEvent(
                            android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
                        )
                        currentInputConnection?.sendKeyEvent(
                            android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
                        )
                    },
                    onSwitchKeyboard = {
                        // 切换到下一个输入法 (如系统自带的中文输入法)
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                        imm.switchToNextInputMethod(window.window?.attributes?.token, false)
                    }
                )
            }
        }
        return view
    }

    // 禁用默认的全屏模式 (Extract UI)
    // 很多设备横屏或大屏时，Android默认会让输入法占用全屏并弹出一个很大的输入框，我们关闭它。
    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onFinishInput() {
        super.onFinishInput()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
