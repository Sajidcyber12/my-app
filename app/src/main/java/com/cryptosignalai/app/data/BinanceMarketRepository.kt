package com.cryptosignalai.app.data

import com.cryptosignalai.app.indicator.TechnicalIndicators
import com.cryptosignalai.app.ai.AiSignalEngine
import com.cryptosignalai.app.model.Candle
import com.cryptosignalai.app.model.Signal
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class BinanceMarketRepository(private val riskPercent: Double = 1.8) {
    private val symbols = listOf(
        Triple("SHIBUSDT", "Shiba Inu", "Meme Coin"),
        Triple("PEPEUSDT", "Pepe Coin", "Meme Coin"),
        Triple("DOGEUSDT", "Dogecoin", "Meme Coin"),
        Triple("BONKUSDT", "Bonk", "Meme Coin"),
        Triple("FLOKIUSDT", "Floki Inu", "Meme Coin"),
        Triple("SUIUSDT", "Sui", "New/Trending")
    )

    suspend fun fetchSignals(interval: String = "15m"): List<Signal> {
        val fixed = symbols.mapNotNull { (symbol, name, tag) -> runCatching { fetchSignal(symbol, name, tag, interval) }.getOrNull() }
        val discovered = runCatching { CoinDiscovery.discover().take(8) }.getOrDefault(emptyList())
            .filter { c -> fixed.none { it.symbol == c.symbol.removeSuffix("USDT") } }
            .mapNotNull { c -> runCatching { fetchSignal(c.symbol, c.label, "New/Trending", interval) }.getOrNull() }
        return (fixed + discovered).distinctBy { it.symbol }
    }

    private fun fetchSignal(symbol: String, name: String, tag: String, interval: String): Signal {
        return fetchSignalFromObject(symbol, name, tag, interval)
    }

    private fun fetchSignalFromObject(symbol: String, name: String, tag: String, interval: String): Signal {
        val ticker = org.json.JSONObject(get("https://api.binance.com/api/v3/ticker/24hr?symbol=$symbol"))
        val price = ticker.getString("lastPrice").toDouble()
        val change = ticker.getString("priceChangePercent").toDouble()
        val raw = JSONArray(get("https://api.binance.com/api/v3/klines?symbol=$symbol&interval=$interval&limit=100"))
        val candles = (0 until raw.length()).map { i ->
            val row = raw.getJSONArray(i)
            Candle(
                row.getString(1).toDouble(),
                row.getString(2).toDouble(),
                row.getString(3).toDouble(),
                row.getString(4).toDouble(),
                row.getString(5).toDouble()
            )
        }
        val ind = TechnicalIndicators.analyze(candles)
        val ai = AiSignalEngine.predict(ind, price)
        val score = ai.score
        val side = ai.side
        val entry = price
        val risk = price * (riskPercent / 100.0)
        val stop = if (side == "SELL") price + risk else price - risk
        val tp1 = if (side == "SELL") price - risk * 1.5 else price + risk * 1.5
        val tp2 = if (side == "SELL") price - risk * 2.5 else price + risk * 2.5
        val tp3 = if (side == "SELL") price - risk * 3.5 else price + risk * 3.5
        return Signal(symbol.removeSuffix("USDT"), name, price, change, score, side, entry, stop, tp1, tp2, tp3, ind, tag, candles)
    }

    private fun get(urlString: String): String {
        val c = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", "CryptoSignalAI/0.1")
        }
        try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    fun fmt(v: Double): String = when {
        v >= 1 -> String.format(Locale.US, "$%.4f", v)
        v >= 0.01 -> String.format(Locale.US, "$%.6f", v)
        else -> String.format(Locale.US, "$%.8f", v)
    }
}
