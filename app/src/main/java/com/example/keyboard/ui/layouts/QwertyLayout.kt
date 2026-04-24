package com.example.keyboard.ui.layouts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.keyboard.ui.components.KeyButton

@Composable
fun QwertyLayout(
    isPinyinMode: Boolean,
    onLanguageToggle: () -> Unit,
    onKeyPress: (String) -> Unit,
    onDelete: () -> Unit,
    onAction: (Int) -> Unit,
    onSwitchKeyboard: () -> Unit
) {
    var isShifted by remember { mutableStateOf(false) }

    val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")

    val transform = { key: String -> if (isShifted) key.uppercase() else key }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            row1.forEach { key ->
                KeyButton(
                    text = transform(key),
                    modifier = Modifier.weight(1f).height(50.dp),
                    onClick = { onKeyPress(transform(key)) }
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            row2.forEach { key ->
                KeyButton(
                    text = transform(key),
                    modifier = Modifier.weight(1f).height(50.dp),
                    onClick = { onKeyPress(transform(key)) }
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            KeyButton(
                text = "SHIFT",
                modifier = Modifier.weight(1.5f).height(50.dp),
                onClick = { isShifted = !isShifted }
            )
            row3.forEach { key ->
                KeyButton(
                    text = transform(key),
                    modifier = Modifier.weight(1f).height(50.dp),
                    onClick = { onKeyPress(transform(key)) }
                )
            }
            KeyButton(
                text = "DEL",
                modifier = Modifier.weight(1.5f).height(50.dp),
                onClick = onDelete
            )
        }
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            KeyButton(
                text = if (isPinyinMode) "中" else "EN",
                modifier = Modifier.weight(1.5f).height(50.dp),
                onClick = onLanguageToggle
            )
            KeyButton(
                text = ",",
                modifier = Modifier.weight(1f).height(50.dp),
                onClick = { onKeyPress(",") }
            )
            KeyButton(
                text = "SPACE",
                modifier = Modifier.weight(4f).height(50.dp),
                onClick = { onKeyPress(" ") }
            )
            KeyButton(
                text = ".",
                modifier = Modifier.weight(1f).height(50.dp),
                onClick = { onKeyPress(".") }
            )
            KeyButton(
                text = "ENTER",
                modifier = Modifier.weight(1.5f).height(50.dp),
                onClick = { onAction(66) } // KEYCODE_ENTER
            )
        }
    }
}
