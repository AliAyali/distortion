package com.aliayali.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliayali.game.settings.GameSettings
import com.aliayali.game.settings.GameSettingsStore

@Composable
fun AudioSettingsDialog(
    store: GameSettingsStore,
    onDismiss: () -> Unit,
    onSettingsChanged: (GameSettings) -> Unit = {},
) {
    var settings by remember(store) { mutableStateOf(store.load()) }

    fun update(next: GameSettings) {
        settings = next
        store.save(next)
        onSettingsChanged(next)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = Color(0xFF111116),
        titleContentColor = Color(0xFFE8E2E0),
        textContentColor = Color(0xFFC8C2C4),
        title = {
            Text(
                text = "تنظیمات تجربه",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
                color = Color(0xFFE7C6C8),
            )
        },
        text = {
            androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    SettingSwitch(
                        title = "موسیقی و صدای پس‌زمینه",
                        subtitle = "صدای پیوسته و فضای وهم‌آلود خانه",
                        checked = settings.musicEnabled,
                        onCheckedChange = { update(settings.copy(musicEnabled = it)) },
                    )
                    SliderSetting(
                        title = "بلندی موسیقی",
                        value = settings.musicVolume,
                        onValueChange = { update(settings.copy(musicVolume = it)) },
                    )
                    SettingSwitch(
                        title = "افکت‌های صوتی",
                        subtitle = "قدم‌ها، در، آینه و ناهنجاری‌ها",
                        checked = settings.effectsEnabled,
                        onCheckedChange = { update(settings.copy(effectsEnabled = it)) },
                    )
                    SliderSetting(
                        title = "بلندی افکت‌ها",
                        value = settings.effectsVolume,
                        onValueChange = { update(settings.copy(effectsVolume = it)) },
                    )
                    SliderSetting(
                        title = "حساسیت چرخش دوربین",
                        value = (settings.lookSensitivity - 0.14f) / (0.62f - 0.14f),
                        valueLabel = when {
                            settings.lookSensitivity < 0.27f -> "کم"
                            settings.lookSensitivity < 0.43f -> "متوسط"
                            else -> "زیاد"
                        },
                        onValueChange = { fraction ->
                            update(settings.copy(lookSensitivity = 0.14f + fraction * (0.62f - 0.14f)))
                        },
                    )
                    SettingSwitch(
                        title = "کاهش فلش‌های تصویری",
                        subtitle = "برای تجربه‌ی آرام‌تر و حساسیت کمتر به نورهای سریع",
                        checked = settings.reduceFlashes,
                        onCheckedChange = { update(settings.copy(reduceFlashes = it)) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ذخیره و بستن", color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color(0xFFE0D9D8), fontSize = 13.sp, textAlign = TextAlign.Right)
            Text(subtitle, color = Color(0xFF8D878D), fontSize = 10.sp, textAlign = TextAlign.Right, lineHeight = 15.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    valueLabel: String = "${(value.coerceIn(0f, 1f) * 100).toInt()}٪",
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, color = Color(0xFFE0D9D8), fontSize = 12.sp)
            Text(valueLabel, color = Color(0xFFC79A9F), fontSize = 11.sp)
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            steps = 19,
        )
    }
}
