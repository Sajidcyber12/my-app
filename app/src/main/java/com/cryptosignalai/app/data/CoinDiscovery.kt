package com.cryptosignalai.app.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CoinDiscovery {
    data class Coin(val symbol: String, val label: String)
    fun discover(): List<Coin> {
        val text = get("https://api.binance.com/api/v3/exchangeInfo")
        val symbols = JSONObject(text).getJSONArray("symbols")
        val banned = setOf("USDCUSDT","FDUSDUSDT","TUSDUSDT","USDPUSDT","BUSDUSDT")
        val out = mutableListOf<Coin>()
        for (i in 0 until symbols.length()) {
            val s = symbols.getJSONObject(i)
            val symbol = s.getString("symbol")
            if (s.optString("status") == "TRADING" && s.optString("quoteAsset") == "USDT" && symbol !in banned && !symbol.contains("UP") && !symbol.contains("DOWN")) {
                out += Coin(symbol, symbol.removeSuffix("USDT"))
            }
        }
        return out.take(80)
    }
    private fun get(url:String):String { val c=URL(url).openConnection() as HttpURLConnection; c.connectTimeout=8000; c.readTimeout=8000; try { if(c.responseCode !in 200..299) error("HTTP ${c.responseCode}"); return c.inputStream.bufferedReader().use{it.readText()} } finally { c.disconnect() } }
}
