package com.cryptosignalai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptosignalai.app.data.BinanceMarketRepository
import com.cryptosignalai.app.model.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import androidx.compose.ui.platform.LocalContext

private val Bg = Color(0xFF020B14)
private val Card = Color(0xFF071827)
private val Green = Color(0xFF00F0A8)
private val Red = Color(0xFFFF3D62)
private val Blue = Color(0xFF48B7FF)
private val Muted = Color(0xFF91A9BD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CryptoApp() }
    }
}

@Composable
fun CryptoApp() {
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Signal?>(null) }
    var signals by remember { mutableStateOf<List<Signal>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var favorites by remember { mutableStateOf(loadFavorites(context)) }

    suspend fun refresh() {
        loading = true
        error = null
        runCatching {
            withContext(Dispatchers.IO) { BinanceMarketRepository().fetchSignals() }
        }.onSuccess { fresh ->
            if (fresh.isNotEmpty()) signals = fresh else error = "No market data received."
        }.onFailure { error = "Live feed unavailable. Showing last data if available." }
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Card, primary = Green)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            if (selected != null) SignalDetails(selected!!, onBack = { selected = null })
            else Scaffold(containerColor = Bg, bottomBar = { BottomBar(tab) { tab = it } }) { p ->
                Column(Modifier.padding(p).fillMaxSize()) {
                    if (loading && signals.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Green)
                    error?.let { Text(it, color = Color(0xFFFFC857), modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp), fontSize = 12.sp) }
                    when (tab) {
                        0 -> Home(signals, loading, onRefresh = { scope.launch { refresh() } }, favorites = favorites, onFavorite = { symbol ->
                            favorites = toggleFavorite(context, favorites, symbol)
                        }, onSelect = { selected = it })
                        1 -> Scanner(signals, onSelect = { selected = it })
                        2 -> Watchlist(signals.filter { favorites.contains(it.symbol) }, onSelect = { selected = it })
                        3 -> Notifications()
                        else -> Profile()
                    }
                }
            }
        }
    }
}

@Composable
fun Header(title: String, subtitle: String, refresh: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🤖", fontSize = 30.sp); Spacer(Modifier.width(10.dp))
        Column { Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 12.sp) }
        Spacer(Modifier.weight(1f))
        if (refresh != null) IconButton(onClick = refresh) { Icon(Icons.Default.Refresh, "Refresh", tint = Green) }
        Icon(Icons.Default.Notifications, null, tint = Color.White)
    }
}

@Composable
fun Pill(text: String, active: Boolean = false, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        color = if (active) Green else Card,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            color = if (active) Color.Black else Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun Home(signals: List<Signal>, loading: Boolean, onRefresh: () -> Unit, favorites: Set<String>, onFavorite: (String) -> Unit, onSelect: (Signal) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Crypto Signal AI", "Live Binance Feed  •  Meme Coins  •  New Gems", refresh = onRefresh)
        Surface(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color(0xFF052319)) {
            Column(Modifier.padding(18.dp)) {
                Text("AI SCANS LIVE MARKETS", color = Green, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Technical-indicator signals for meme & trending coins", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                Text("⚠ Signals are analysis, not guaranteed returns.", color = Color(0xFFFFC857), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            listOf("₿ BTC  Live", "Ξ ETH  Live", "◎ SOL  Live", "🐸 MEME  Scan").forEach { Surface(Modifier.padding(end = 10.dp), shape = RoundedCornerShape(16.dp), color = Card) { Text(it, Modifier.padding(16.dp), color = Color.White) } }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Top Live Signals", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(if (loading) "Updating…" else "Auto refresh", color = Green, fontSize = 12.sp)
        }
        if (signals.isEmpty() && !loading) Text("No signals available right now.", color = Muted, modifier = Modifier.padding(18.dp))
        filteredSignals.sortedByDescending { it.score }.take(6).forEach { SignalCard(it, isFavorite = favorites.contains(it.symbol), onFavorite = { onFavorite(it.symbol) }, onClick = { onSelect(it) }) }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun SignalCard(c: Signal, isFavorite: Boolean = false, onFavorite: () -> Unit = {}, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.symbol, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(c.name, color = Muted, fontSize = 12.sp)
                Text(c.tag, color = Blue, fontSize = 11.sp)
                Text("Entry ${price(c.entry)}  •  SL ${price(c.stopLoss)}", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onFavorite) { Icon(if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, "Favorite", tint = if (isFavorite) Green else Muted) }
                Text(c.side, color = when (c.side) { "BUY" -> Green; "SELL" -> Red; else -> Color(0xFFFFC857) }, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("AI ${c.score}%", color = if (c.score >= 70) Green else Color(0xFFFFC857))
                Text(String.format("%+.2f%%", c.change24h), color = if (c.change24h >= 0) Green else Red, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun Scanner(signals: List<Signal>, onSelect: (Signal) -> Unit) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filteredSignals = when (selectedFilter) { "Meme Coins" -> signals.filter { it.tag == "Meme Coin" }; "New/Trending" -> signals.filter { it.tag == "New/Trending" }; "High Volume" -> signals.filter { it.indicators.volumeRatio >= 2.0 }; else -> signals }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("AI Coin Scanner", "Live Candles  •  RSI  •  MACD  •  EMA  •  Volume")
        Surface(Modifier.padding(16.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color(0xFF062318)) { Column(Modifier.padding(18.dp)) { Text("AI SCANS 24/7", color = Green, fontSize = 25.sp, fontWeight = FontWeight.Bold); Text("Meme coins, momentum and volume spikes", color = Color.White) } }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) { listOf("All", "Meme Coins", "New/Trending", "High Volume").forEachIndexed { i, t -> Box(Modifier.padding(end = 8.dp)) { Pill(t, selectedFilter == t) { selectedFilter = t } } } }
        filteredSignals.sortedByDescending { it.score }.forEachIndexed { i, c ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", Modifier.width(28.dp), color = Muted)
                Column(Modifier.weight(1f)) { Text(c.name, fontWeight = FontWeight.Bold); Text("${c.symbol}  •  ${String.format("%+.2f%%", c.change24h)}", color = if (c.change24h >= 0) Green else Red, fontSize = 12.sp); Text("RSI ${c.indicators.rsi.format1()}  •  Vol ${c.indicators.volumeRatio.format1()}x", color = Muted, fontSize = 11.sp) }
                Text("${c.score}", color = Green, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onSelect(c) }) { Text(c.side, color = if (c.side == "BUY") Green else Red) }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun Watchlist(signals: List<Signal>, onSelect: (Signal) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Watchlist", "Favorite Coins  •  Live Updates  •  Instant Signals")
        Surface(Modifier.padding(16.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Card) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text("🐸", fontSize = 36.sp); Column(Modifier.weight(1f)) { Text("Track Your Favorite Coins", fontWeight = FontWeight.Bold); Text("Live prices and signal changes", color = Muted, fontSize = 12.sp) }; Button(onClick = {}) { Text("+ Add") } } }
        signals.forEach { SignalCard(it, isFavorite = true, onClick = { onSelect(it) }) }
    }
}

@Composable
fun Notifications() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Notifications", "Signal Alerts  •  Price Alerts  •  Updates")
        listOf("🟢 New BUY Signal", "🔥 Meme Coin Momentum Alert", "🔔 Price Target Reached", "🔴 SELL Signal", "🚀 New Listing", "⚙ App Update").forEach { Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), shape = RoundedCornerShape(16.dp), color = Card) { Text(it, Modifier.padding(18.dp), fontWeight = FontWeight.SemiBold) } }
        Text("Push Notifications", Modifier.padding(18.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        listOf("Buy / Sell Signals", "New Meme Coins", "New Listings", "Price Alerts").forEach { Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { Text(it, Modifier.weight(1f)); Switch(true, {}) } }
    }
}

@Composable
fun Profile() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Profile & Settings", "Customize your app experience")
        Text("👤  Crypto Signal User", Modifier.padding(20.dp), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        listOf("🔔 Notifications", "🛡 Risk Management", "⚡ Auto Take Profit", "🛑 Stop Loss Protection", "🌙 Dark Theme", "🌐 Language", "💵 Currency", "🔐 Security & 2FA", "❓ Help & Support").forEach { Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(14.dp), color = Card) { Text(it, Modifier.padding(17.dp)) } }
    }
}

@Composable
fun SignalDetails(c: Signal, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }; Text("Signal Details", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        Surface(Modifier.padding(16.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Card) {
            Column(Modifier.padding(18.dp)) {
                Row { Column(Modifier.weight(1f)) { Text(c.symbol, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text(c.name, color = Muted); Text(price(c.price), fontSize = 21.sp, modifier = Modifier.padding(top = 8.dp)) }; Text("${c.side} SIGNAL", color = if (c.side == "BUY") Green else Red, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                Text("AI Score ${c.score}%", color = Green, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
                Spacer(Modifier.height(12.dp))
                CandleChart(c.candles.takeLast(55))
                Spacer(Modifier.height(14.dp)); Text("Technical Indicators", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("RSI (14): ${c.indicators.rsi.format1()}   •   EMA 9/21: ${if (c.indicators.ema9 > c.indicators.ema21) "Bullish" else "Bearish"}", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                Text("MACD: ${c.indicators.macd.format8()}   •   Volume: ${c.indicators.volumeRatio.format1()}x", color = Muted, modifier = Modifier.padding(top = 5.dp))
                Spacer(Modifier.height(14.dp)); Text("Signal Summary", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                listOf("Entry" to c.entry, "Stop Loss" to c.stopLoss, "Take Profit 1" to c.tp1, "Take Profit 2" to c.tp2, "Take Profit 3" to c.tp3).forEach { Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) { Text(it.first, Modifier.weight(1f), color = Muted); Text(price(it.second), fontWeight = FontWeight.Bold) } }
                Text("⚠ Meme/new coins can be extremely volatile. Verify liquidity, contract details and manage risk.", color = Color(0xFFFFC857), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
fun CandleChart(candles: List<com.cryptosignalai.app.model.Candle>) {
    Surface(Modifier.fillMaxWidth().height(220.dp), shape = RoundedCornerShape(14.dp), color = Color(0xFF04131F)) {
        if (candles.size < 2) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Candles unavailable", color = Muted) }
        } else {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                val min = candles.minOf { it.low }
                val max = candles.maxOf { it.high }
                val range = (max - min).coerceAtLeast(1e-12)
                val step = size.width / candles.size
                candles.forEachIndexed { i, c ->
                    val x = step * i + step / 2f
                    fun y(v: Double) = (size.height * (1f - ((v - min) / range).toFloat()))
                    val bullish = c.close >= c.open
                    val bodyTop = y(maxOf(c.open, c.close))
                    val bodyBottom = y(minOf(c.open, c.close))
                    val wickTop = y(c.high)
                    val wickBottom = y(c.low)
                    val stroke = if (bullish) Green else Red
                    drawLine(stroke, androidx.compose.ui.geometry.Offset(x, wickTop), androidx.compose.ui.geometry.Offset(x, wickBottom), strokeWidth = 2f)
                    drawRect(stroke, topLeft = androidx.compose.ui.geometry.Offset(x - step * .28f, bodyTop), size = androidx.compose.ui.geometry.Size(step * .56f, (bodyBottom - bodyTop).coerceAtLeast(2f)))
                }
            }
        }
    }
}

private const val PREFS = "crypto_signal_ai"
private const val KEY_FAVORITES = "favorites"
private fun loadFavorites(context: Context): Set<String> = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()
private fun toggleFavorite(context: Context, current: Set<String>, symbol: String): Set<String> {
    val next = current.toMutableSet().apply { if (!add(symbol)) remove(symbol) }.toSet()
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(KEY_FAVORITES, next).apply()
    return next
}

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = Color(0xFF03101B)) {
        listOf(Icons.Default.Home to "Home", Icons.Default.Bolt to "AI Scanner", Icons.Default.Star to "Watchlist", Icons.Default.Notifications to "Alerts", Icons.Default.Person to "Profile").forEachIndexed { i, (icon, label) -> NavigationBarItem(selected == i, onClick = { onSelect(i) }, icon = { Icon(icon, label) }, label = { Text(label, fontSize = 10.sp) }) }
    }
}

private fun price(v: Double): String = when { v >= 1 -> String.format("$%.4f", v); v >= 0.01 -> String.format("$%.6f", v); else -> String.format("$%.8f", v) }
private fun Double.format1() = String.format("%.1f", this)
private fun Double.format8() = String.format("%.8f", this)
