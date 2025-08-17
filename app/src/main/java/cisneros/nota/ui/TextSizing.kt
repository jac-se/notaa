package cisneros.nota.ui

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

enum class TextSizeLevel { SMALLEST, SMALLER, NORMAL, LARGER, LARGEST }

data class AccessibleSizes(val title: TextUnit, val body: TextUnit, val button: TextUnit)

fun TextSizeLevel.toAccessibleSizes(): AccessibleSizes = when (this) {
    TextSizeLevel.SMALLEST -> AccessibleSizes(16.sp, 14.sp, 14.sp)
    TextSizeLevel.SMALLER -> AccessibleSizes(18.sp, 16.sp, 16.sp)
    TextSizeLevel.NORMAL -> AccessibleSizes(20.sp, 18.sp, 18.sp)
    TextSizeLevel.LARGER -> AccessibleSizes(22.sp, 20.sp, 20.sp)
    TextSizeLevel.LARGEST -> AccessibleSizes(24.sp, 22.sp, 22.sp)
}
