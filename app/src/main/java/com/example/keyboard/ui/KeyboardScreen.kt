package com.example.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun KeyboardScreen(
    onKeyPress: (String) -> Unit,
    onDelete: () -> Unit,
    onAction: (Int) -> Unit,
    onSwitchKeyboard: () -> Unit
) {
    // 前端大杀器：用 State 控制一切状态！
    var isPinyinMode by remember { mutableStateOf(false) }
    var composingText by remember { mutableStateOf("") }

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
                val candidates = PinyinEngine.getCandidates(composingText)
                candidates.forEach { cand ->
                    Text(
                        text = cand,
                        modifier = Modifier
                            .clickable {
                                // 点击候选词后，将中文发送上去，并清空拼写状态
                                if (!cand.startsWith("词库暂无")) {
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
                        // 在拼音模式下打字母，只存入状态，不发到输入框
                        composingText += key.lowercase()
                    } else if (isPinyinMode && key == " " && composingText.isNotEmpty()) {
                        // 拼音模式下按空格，默认提交第一个候选词
                        val firstCand = PinyinEngine.getCandidates(composingText).firstOrNull()
                        if (firstCand != null && !firstCand.startsWith("词库暂无")) {
                            onKeyPress(firstCand)
                        } else {
                            onKeyPress(composingText) // 如果没匹配上内容，直接把字母打上去
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
