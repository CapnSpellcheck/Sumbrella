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
          arrayOf(byteArrayOf(1, 2, 3, 4), byteArrayOf(3, 2, 1, 4), byteArrayOf(2, 3, 1, 4)),
          7,
          4
       )
       val viewModel = viewModel<SumGameViewModel> { SumGameViewModel(game) }
       SumGame(viewModel)
    }
}


inline fun SymmetricInOutEasing() = CubicBezierEasing(0.42f, 0f, 0.58f, 1.0f)
