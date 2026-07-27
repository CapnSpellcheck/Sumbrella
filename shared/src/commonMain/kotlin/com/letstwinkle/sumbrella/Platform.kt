package com.letstwinkle.sumbrella

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform