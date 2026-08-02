@file:OptIn(ExperimentalTime::class)

package com.letstwinkle

import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toDuration

class TestScheduleSyncingClock(val testScheduler: TestCoroutineScheduler) : Clock {
   private val startInstant = Clock.System.now()

   override fun now(): Instant {
      return startInstant + testScheduler.currentTime.toDuration(DurationUnit.MILLISECONDS)
   }
}
