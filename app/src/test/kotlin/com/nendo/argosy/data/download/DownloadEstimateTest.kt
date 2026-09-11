package com.nendo.argosy.data.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the two halves of the download time estimate: how a speed sample folds into the
 * running average, and when an estimate refuses to be shown at all.
 *
 * The averaging weight is free to be retuned; what must not change is that one slow sample
 * cannot halve the figure, that a sustained change does reach it, and that a transfer with
 * no speed, no declared size, or no bytes left yields no estimate rather than a wrong one.
 */
class DownloadEstimateTest {

    private fun progress(
        downloaded: Long,
        total: Long,
        average: Long,
        state: DownloadState = DownloadState.DOWNLOADING
    ) = DownloadProgress(
        id = 1L,
        gameId = 1L,
        rommId = 1L,
        fileName = "game.zip",
        gameTitle = "Game",
        platformSlug = "snes",
        coverPath = null,
        bytesDownloaded = downloaded,
        totalBytes = total,
        state = state,
        averageBytesPerSecond = average
    )

    @Test
    fun `first sample becomes the average outright`() {
        val averager = DownloadSpeedAverager()
        assertEquals(8_000_000L, averager.average(1L, 8_000_000L))
    }

    @Test
    fun `one sample cannot drag the average to itself`() {
        val averager = DownloadSpeedAverager()
        averager.average(1L, 8_000_000L)
        assertEquals(6_500_000L, averager.average(1L, 2_000_000L))
        assertEquals(5_375_000L, averager.average(1L, 2_000_000L))
    }

    @Test
    fun `a sustained speed change is reached`() {
        val averager = DownloadSpeedAverager()
        averager.average(1L, 8_000_000L)
        repeat(20) { averager.average(1L, 2_000_000L) }
        val settled = averager.average(1L, 2_000_000L)
        assertTrue("settled at $settled", settled in 1_900_000L..2_100_000L)
    }

    @Test
    fun `a stall clears the average instead of holding a stale one`() {
        val averager = DownloadSpeedAverager()
        averager.average(1L, 8_000_000L)
        assertEquals(0L, averager.average(1L, 0L))
        assertEquals(2_000_000L, averager.average(1L, 2_000_000L))
    }

    @Test
    fun `averages are kept per download and pruned when one leaves`() {
        val averager = DownloadSpeedAverager()
        averager.average(1L, 8_000_000L)
        averager.average(2L, 1_000_000L)
        averager.retain(setOf(2L))
        assertEquals(4_000_000L, averager.average(1L, 4_000_000L))
        assertEquals(1_750_000L, averager.average(2L, 4_000_000L))
    }

    @Test
    fun `estimate divides the bytes left by the average`() {
        assertEquals(10L, progress(downloaded = 10L, total = 110L, average = 10L).secondsRemaining)
    }

    @Test
    fun `no estimate without a speed, a size, or bytes left`() {
        assertNull(progress(downloaded = 10L, total = 110L, average = 0L).secondsRemaining)
        assertNull(progress(downloaded = 10L, total = 0L, average = 10L).secondsRemaining)
        assertNull(progress(downloaded = 110L, total = 110L, average = 10L).secondsRemaining)
    }
}
