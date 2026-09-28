package com.cryptosignalai.app.ai

import com.cryptosignalai.app.model.Candle
import com.cryptosignalai.app.model.IndicatorSnapshot
import kotlin.math.abs

/** Transparent on-device ensemble. Replace weights with a trained TFLite/ONNX model in production. */
object AiSignalEngine {
    data class Result(val score: Int, val side: String, val confidence: Int)

    fun predict(ind: IndicatorSnapshot, price: Double): Result {
        val rsiFeature = when {
            ind.rsi in 50.0..68.0 -> 1.0
            ind.rsi < 32.0 -> 0.35
            ind.rsi > 78.0 -> -1.0
            else -> 0.0
        }
        val trend = if (ind.ema9 > ind.ema21) 1.0 else -1.0
        val macd = (ind.macd / price.coerceAtLeast(1e-12)).coerceIn(-0.02, 0.02) / 0.02
        val volume = ((ind.volumeRatio - 1.0) / 1.5).coerceIn(-1.0, 1.0)
        val raw = 0.30 * rsiFeature + 0.35 * trend + 0.20 * macd + 0.15 * volume
        val score = (50 + raw * 45).toInt().coerceIn(5, 99)
        val side = when { score >= 62 -> "BUY"; score <= 38 -> "SELL"; else -> "WATCH" }
        val confidence = (50 + abs(raw) * 50).toInt().coerceIn(50, 99)
        return Result(score, side, confidence)
    }

    fun backtest(candles: List<Candle>): BacktestResult {
        if (candles.size < 50) return BacktestResult(0, 0, 0.0, 0.0)
        var wins = 0; var trades = 0; var pnl = 0.0
        for (i in 30 until candles.lastIndex) {
            val window = candles.subList(0, i + 1)
            val closes = window.map { it.close }
            val volumes = window.map { it.volume }
            val rsi = rsi(closes, 14)
            val ema9 = ema(closes, 9); val ema21 = ema(closes, 21)
            val macd = ema(closes, 12) - ema(closes, 26)
            val vr = volumes.takeLast(5).average() / volumes.takeLast(25).average().coerceAtLeast(1e-12)
            val result = predict(IndicatorSnapshot(rsi, macd, ema9, ema21, vr), closes.last())
            if (result.side == "WATCH") continue
            trades++
            val entry = closes.last(); val future = candles[i + 1].close
            val ret = if (result.side == "BUY") (future - entry) / entry else (entry - future) / entry
            pnl += ret * 100.0
            if (ret > 0) wins++
        }
        return BacktestResult(trades, wins, if (trades == 0) 0.0 else wins * 100.0 / trades, pnl)
    }

    data class BacktestResult(val trades: Int, val wins: Int, val winRate: Double, val pnlPercent: Double)

    private fun ema(values: List<Double>, period: Int): Double { val k=2.0/(period+1); var e=values.take(period).average(); for(v in values.drop(period)) e=v*k+e*(1-k); return e }
    private fun rsi(values: List<Double>, period: Int): Double { var g=0.0; var l=0.0; for(i in 1..period){ val d=values[i]-values[i-1]; if(d>=0)g+=d else l-=d }; var ag=g/period; var al=l/period; for(i in period+1 until values.size){ val d=values[i]-values[i-1]; ag=(ag*(period-1)+maxOf(d,0.0))/period; al=(al*(period-1)+maxOf(-d,0.0))/period }; if(al==0.0)return 100.0; val rs=ag/al; return 100.0-100.0/(1+rs) }
}
