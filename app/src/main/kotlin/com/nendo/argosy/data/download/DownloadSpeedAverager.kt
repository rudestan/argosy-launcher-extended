package com.nendo.argosy.data.download

private const val HISTORY_WEIGHT = 3

/**
 * Rolling average of transfer speed, keyed by download id.
 *
 * [DownloadProgress.bytesPerSecond] is a single sample taken over one UI tick, so an estimate
 * divided straight out of it swings by minutes between frames. Each new sample is weighted
 * against the average so far: a momentary stall or burst barely moves the figure, while a
 * sustained change reaches it within a few samples.
 *
 * Mutable by nature, so it lives in data/ rather than under the packages
 * compose_stability_config.conf promises are stable.
 */
class DownloadSpeedAverager {

    private val averages = mutableMapOf<Long, Long>()

    fun average(id: Long, bytesPerSecond: Long): Long {
        if (bytesPerSecond <= 0) {
            averages.remove(id)
            return 0
        }
        val previous = averages[id]
        val next = if (previous == null || previous <= 0) {
            bytesPerSecond
        } else {
            (previous * HISTORY_WEIGHT + bytesPerSecond) / (HISTORY_WEIGHT + 1)
        }
        averages[id] = next
        return next
    }

    fun retain(ids: Set<Long>) {
        averages.keys.retainAll(ids)
    }
}
