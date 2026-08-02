package com.letstwinkle.sumbrella.engine

import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.model.SumGameEffort

interface IGameEngineFactory {
   fun createGameEngine(game: SumGame, effort: SumGameEffort): IGameEngine
}

class GameEngineFactory : IGameEngineFactory {
   override fun createGameEngine(game: SumGame, effort: SumGameEffort): SumGameEngine {
      return SumGameEngine(game, effort)
   }

}
