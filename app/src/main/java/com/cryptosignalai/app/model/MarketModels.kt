package com.cryptosignalai.app.model

data class Candle(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class IndicatorSnapshot(
    val rsi: Double,
    val macd: Double,
    val ema9: Double,
    val ema21: Double,
    val volumeRatio: Double
)

data class Signal(
    val symbol: String,
    val name: String,
    val price: Double,
    val change24h: Double,
    val score: Int,
    val side: String,
    val entry: Double,
    val stopLoss: Double,
    val tp1: Double,
    val tp2: Double,
    val tp3: Double,
    val indicators: IndicatorSnapshot,
    val tag: String,
    val candles: List<Candle> = emptyList()
)
