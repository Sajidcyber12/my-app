package com.cryptosignalai.app.indicator

import com.cryptosignalai.app.model.Candle
import com.cryptosignalai.app.model.IndicatorSnapshot
import kotlin.math.abs

object TechnicalIndicators {
    fun analyze(candles: List<Candle>): IndicatorSnapshot {
        require(candles.size >= 30)
        val closes = candles.map { it.close }
        val volumes = candles.map { it.volume }
        val rsi = rsi(closes, 14)
        val ema9 = ema(closes, 9)
        val ema21 = ema(closes, 21)
        val ema12 = ema(closes, 12)
        val ema26 = ema(closes, 26)
        val macd = ema12 - ema26
        val recentVol = volumes.takeLast(5).average()
        val baselineVol = volumes.takeLast(25).average().coerceAtLeast(1e-12)
        return IndicatorSnapshot(rsi, macd, ema9, ema21, recentVol / baselineVol)
    }

    fun score(ind: IndicatorSnapshot, price: Double): Pair<Int, String> {
        var score = 50
        if (ind.rsi in 50.0..70.0) score += 10
        if (ind.rsi < 35.0) score += 4
        if (ind.rsi > 75.0) score -= 10
        if (ind.ema9 > ind.ema21) score += 14 else score -= 12
        val macdScale = (abs(ind.macd) / price.coerceAtLeast(1e-12)).coerceAtMost(0.02)
        if (ind.macd > 0) score += (8 * (macdScale / 0.02)).toInt() else score -= (8 * (macdScale / 0.02)).toInt()
        if (ind.volumeRatio >= 1.5) score += 12 else if (ind.volumeRatio >= 1.15) score += 6
        val finalScore = score.coerceIn(5, 99)
        val side = when {
            finalScore >= 62 -> "BUY"
            finalScore <= 38 -> "SELL"
            else -> "WATCH"
        }
        return finalScore to side
    }

    private fun ema(values: List<Double>, period: Int): Double {
        val k = 2.0 / (period + 1)
        var e = values.take(period).average()
        for (v in values.drop(period)) e = v * k + e * (1 - k)
        return e
    }

    private fun rsi(values: List<Double>, period: Int): Double {
        var gain = 0.0
        var loss = 0.0
        for (i in 1..period) {
            val d = values[i] - values[i - 1]
            if (d >= 0) gain += d else loss -= d
        }
        var avgGain = gain / period
        var avgLoss = loss / period
        for (i in (period + 1) until values.size) {
            val d = values[i] - values[i - 1]
            avgGain = (avgGain * (period - 1) + maxOf(d, 0.0)) / period
            avgLoss = (avgLoss * (period - 1) + maxOf(-d, 0.0)) / period
        }
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }
}
