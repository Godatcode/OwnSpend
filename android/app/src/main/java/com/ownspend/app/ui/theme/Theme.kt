package com.ownspend.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Modern gradient colors
val PrimaryGreen = Color(0xFF00C853)
val PrimaryGreenDark = Color(0xFF00A843)
val SecondaryBlue = Color(0xFF2196F3)
val AccentOrange = Color(0xFFFF6F00)
val AccentPurple = Color(0xFF9C27B0)

// Income/Expense colors
val IncomeGreen = Color(0xFF4CAF50)
val ExpenseRed = Color(0xFFE53935)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFB2FF59),
    
    secondary = SecondaryBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0D47A1),
    onSecondaryContainer = Color(0xFF90CAF9),
    
    tertiary = AccentPurple,
    onTertiary = Color.White,
    
    background = Color(0xFF0A0E1A),
    onBackground = Color(0xFFE8E8E8),
    
    surface = Color(0xFF1A1F2E),
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF2A3142),
    onSurfaceVariant = Color(0xFFB8B8B8),
    
    error = ExpenseRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2FF59),
    onPrimaryContainer = Color(0xFF1B5E20),
    
    secondary = SecondaryBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBBDEFB),
    onSecondaryContainer = Color(0xFF0D47A1),
    
    tertiary = AccentPurple,
    onTertiary = Color.White,
    
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1A1A1A),
    
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF5A5A5A),
    
    error = ExpenseRed,
    onError = Color.White
)

@Composable
fun OwnSpendTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
