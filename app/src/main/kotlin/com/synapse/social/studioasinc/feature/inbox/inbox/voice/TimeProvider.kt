package com.synapse.social.studioasinc.feature.inbox.inbox.voice

interface TimeProvider {
    fun currentTimeMillis(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

class TestTimeProvider(var currentTime: Long = 1000000000L) : TimeProvider {
    override fun currentTimeMillis(): Long = currentTime

    fun advanceTimeBy(ms: Long) {
        currentTime += ms
    }
}
