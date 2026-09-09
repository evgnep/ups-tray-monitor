package estimate

// Estimates how long until the UPS battery charge falls to the shutdown
// threshold, based on the recent discharge trend (linear least squares over a
// sliding window). Only meaningful while the UPS runs on battery - call
// reset() as soon as mains power is back.
class DischargeEstimator(
    private val windowMillis: Long = 5 * 60_000L,
    private val minSpanMillis: Long = 45_000L,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private data class Sample(val timeMillis: Long, val percent: Double)

    private val samples = ArrayDeque<Sample>()

    fun reset() = samples.clear()

    fun addSample(percent: Double) {
        val now = clock()
        samples.addLast(Sample(now, percent))
        val cutoff = now - windowMillis
        while (samples.size > 2 && samples.first().timeMillis < cutoff) {
            samples.removeFirst()
        }
    }

    // Estimated seconds until 'thresholdPercent' is reached, or null if there
    // is not enough data yet or the battery is not draining. Returns 0 when
    // the threshold is already reached.
    fun estimateSecondsToThreshold(thresholdPercent: Int): Long? {
        if (samples.size < 2) return null
        val span = samples.last().timeMillis - samples.first().timeMillis
        if (span < minSpanMillis) return null

        // percent = a + slope * timeSeconds; slope is negative while draining
        val n = samples.size
        val t0 = samples.first().timeMillis
        var sx = 0.0
        var sy = 0.0
        var sxx = 0.0
        var sxy = 0.0
        for (s in samples) {
            val x = (s.timeMillis - t0) / 1000.0
            val y = s.percent
            sx += x
            sy += y
            sxx += x * x
            sxy += x * y
        }
        val denom = n * sxx - sx * sx
        if (denom == 0.0) return null
        val slope = (n * sxy - sx * sy) / denom
        if (slope >= -1e-4) return null // not draining (flat or noisy)

        val current = samples.last().percent
        if (current <= thresholdPercent) return 0
        return ((thresholdPercent - current) / slope).toLong().coerceAtLeast(0)
    }
}
