package com.letstwinkle.sumbrella

import com.letstwinkle.sumbrella.model.SumGame
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SumGameTest {
   @Test fun testLoadGame3() = runTest {
      val json = """
         {
           "id": "3",
           "cells": [
             [1,2,2,4,1],
             [6,1,1,1,1],
             [2,1,1,4,1],
             [4,6,5,1,7],
             [1,9,6,7,5],
           ],
           "sum": 20,
           "plots": 4,
         }
      """
      val game = Json { allowTrailingComma = true } .decodeFromString<SumGame>(json)
      assertEquals("3", game.id)
      assertEquals(20, game.sum)
      assertEquals(4, game.plots)
      assertTrue(arrayOf(
         byteArrayOf(1, 2, 2, 4, 1),
         byteArrayOf(6, 1, 1, 1, 1),
         byteArrayOf(2, 1, 1, 4, 1),
         byteArrayOf(4, 6, 5, 1, 7),
         byteArrayOf(1, 9, 6, 7, 5),
      ).contentDeepEquals(game.cells))
   }

}
