@file:OptIn(ExperimentalTime::class)

package com.letstwinkle.sumbrella

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.letstwinkle.sumbrella.screens.game.SumGame
import com.letstwinkle.sumbrella.screens.game.SumGameViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.ExperimentalTime

@Composable
@Preview
fun App() {
    MaterialTheme {

       val game = com.letstwinkle.sumbrella.model.SumGame(
          "",
          arrayOf(byteArrayOf(1, 1, 1, 1), byteArrayOf(2, 2, 2, 2), byteArrayOf(3, 3, 3, 3), byteArrayOf(4, 4, 4, 4)),
          10,
          4
       )
       val viewModel = viewModel<SumGameViewModel> { SumGameViewModel(game) }
       SumGame(viewModel)
    }
}


inline fun SymmetricInOutEasing() = CubicBezierEasing(0.42f, 0f, 0.58f, 1.0f)
