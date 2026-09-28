# Crypto Signal AI — Final Production-Ready Starter

Android Kotlin + Jetpack Compose crypto signal application.

## Included
- Live Binance Spot public market data
- Dynamic USDT coin discovery from Binance exchangeInfo
- Meme/trending focus with configurable symbol list
- 15m OHLCV candles and real candlestick chart
- RSI, EMA 9/21, MACD and relative-volume analysis
- Transparent on-device AI-style ensemble scoring and confidence
- BUY / SELL / WATCH signals with Entry, Stop Loss and 3 Take Profits
- Watchlist persistence
- Local alert/settings foundation
- Backtest engine over fetched candles
- Backend + Firebase Cloud Messaging deployment template

## Important
The included AI engine is a transparent mathematical ensemble, not a pretrained ML model. A genuine ML model requires a historical labeled dataset, training, validation and ongoing monitoring. The project is structured so a TFLite/ONNX model can replace `AiSignalEngine.predict()` later.

The Android app does not place trades and does not contain exchange trading secrets.

### Firebase
`google-services.json.example` is a template only. To enable remote push notifications, create your own Firebase project, download the real `google-services.json`, add the Google Services Gradle plugin/dependencies, and connect the Android client to the backend. Do not commit credentials.

### Backend
See `backend/README.md`. The backend is intentionally a template because production Firebase credentials belong to the app owner.

### Risk
Crypto and especially meme/new coins are highly volatile. Signals are informational analysis, not guaranteed returns or financial advice. Verify liquidity, market availability and token/contract details independently.

### Build
Open the `CryptoSignalAI` directory in Android Studio and sync Gradle. This package was ZIP-validated in the build environment, but no Gradle/Android SDK toolchain was available here to perform a full APK compilation.
