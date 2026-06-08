package com.horas_al_mando.ham_android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val HamColorScheme = lightColorScheme(
    primary            = Primary,
    onPrimary          = OnPrimary,
    secondary          = Secondary,
    onSecondary        = OnPrimary,
    background         = Background,
    onBackground       = OnBackground,
    surface            = Surface,
    onSurface          = OnBackground,
    outline            = Outline,
    error              = Destructive,
    onError            = OnPrimary,
)

@Composable
fun HamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HamColorScheme,
        typography  = HamTypography,
        shapes      = HamShapes,
        content     = content,
    )
}
