package com.example.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.keyboard.PinyinEngine
import com.example.keyboard.ui.layouts.QwertyLayout
import kotlinx.coroutines.delay

@Composable
fun KeyboardScreen(
    onKeyPress: (String) -> Unit,
    onDelete: () -> Unit,
    onAction: (Int) -> Unit,
    onSwitchKeyboard: () -> Unit
) {
    var isPinyinMode by remember { mutableStateOf(false) }
    var composingText by remember { mutableStateOf("") }
    var candidates by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // 网络请求云端拼音：监听 composingText 改变，自动触发。
    LaunchedEffect(composingText) {
        if (composingText.isNotEmpty()) {
            // 防抖（Debounce）：稍微延迟 150 毫秒。如果用户连续啪啪啪敲好几个字母
            // Compose 会取消上一次正在等 delay 的请求，直接去发最新的字母，节省网络压力。
            delay(150)
            val networkResult = PinyinEngine.fetchCandidates(composingText)
            candidates = networkResult
        } else {
            candidates = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray)
    ) {
        // 如果是拼音模式，并且正在输入，就显示“候选词条 (Candidate View)”
        if (isPinyinMode && composingText.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color.White)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧显示用户拼出的英文字母
                Text(
                    text = composingText,
                    color = Color.Blue,
                    modifier = Modifier.padding(end = 16.dp)
                )
                // 右侧显示对应的候选中文
                candidates.forEach { cand ->
                    Text(
                        text = cand,
                        color = Color.Black,
                        modifier = Modifier
                            .clickable {
                                // 点击候选词后，将中文发送上去，并清空拼写状态
                                if (!cand.startsWith("暂无词库") && !cand.startsWith("词库加载中")) {
                                    onKeyPress(cand)
                                    composingText = ""
                                }
                            }
                            .padding(end = 16.dp)
                    )
                }
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            QwertyLayout(
                isPinyinMode = isPinyinMode,
                onLanguageToggle = {
                    isPinyinMode = !isPinyinMode
                    composingText = ""
                },
                onKeyPress = { key ->
                    if (isPinyinMode && key.length == 1 && key[0].isLetter()) {
                        // 在拼音模式下打字母，存入状态发网络请求
                        composingText += key.lowercase()
                    } else if (isPinyinMode && key == " " && composingText.isNotEmpty()) {
                        // 拼音模式下按空格，默认提交候选列表的第一个候选词
                        val firstCand = candidates.firstOrNull()
                        if (firstCand != null && !firstCand.startsWith("暂无词库") && !firstCand.startsWith("词库加载中")) {
                            onKeyPress(firstCand)
                        } else {
                            onKeyPress(composingText) // 如果没匹配上或者是异常，直接把拼音字母打上去
                        }
                        composingText = ""
                    } else {
                        // 英文模式或者按了符号键，直接发给输入框
                        onKeyPress(key)
                    }
                },
                onDelete = {
                    // 如果正在拼音，则回删正在拼写的字母
                    if (isPinyinMode && composingText.isNotEmpty()) {
                        composingText = composingText.dropLast(1)
                    } else {
                        // 否则回删输入框内的字符
                        onDelete()
                    }
                },
                onAction = onAction,
                onSwitchKeyboard = onSwitchKeyboard
            )
        }
    }
}
