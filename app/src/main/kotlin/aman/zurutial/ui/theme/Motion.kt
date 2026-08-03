package aman.zurutial.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

/**
 * Central motion tokens so every screen shares the same spring feel instead of
 * re-declaring animation specs inline. Mirrors Material 3 Expressive's emphasis
 * on springy, physical motion over linear easing.
 */
object Motion {
    fun <T> expressiveSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> snappySpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Shared-axis X (horizontal) — used for forward/back flows like Home -> Create Room.
    fun <S> sharedAxisX(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        (slideInHorizontally(animationSpec = tween(320)) { it / 4 } + fadeIn(tween(280)))
            .togetherWith(
                slideOutHorizontally(animationSpec = tween(320)) { -it / 6 } + fadeOut(tween(200))
            )
    }

    // Shared-axis Y (vertical) — used for entering a room / full-screen takeovers.
    fun <S> sharedAxisY(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        (slideInVertically(animationSpec = tween(360)) { it / 5 } + fadeIn(tween(300)))
            .togetherWith(
                slideOutVertically(animationSpec = tween(280)) { -it / 8 } + fadeOut(tween(200))
            )
    }
}
