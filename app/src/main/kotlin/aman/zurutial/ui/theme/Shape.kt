package aman.zurutial.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material 3 Expressive leans into large, confident rounded corners.
val ZurutialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

// Extra tokens beyond the default Material scale, for the biggest hero surfaces
// and the smallest pill-shaped chips.
object ExtraShapes {
    val heroCard = RoundedCornerShape(32.dp)
    val bottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val pill = RoundedCornerShape(percent = 50)
    val thumbnail = RoundedCornerShape(28.dp)
}
