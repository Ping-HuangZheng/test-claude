# 待機時鐘（Standby Clock）

Android 床頭／待機時鐘 App。Kotlin + Jetpack Compose，不需要網路。

## 功能

| 功能 | 說明 |
| --- | --- |
| 三種錶面 | 數位、指針、翻頁（翻頁動畫）。左右滑動切換，兩指縮放（0.5～2 倍，各錶面分開記） |
| 控制列 | 點一下畫面出現，5 秒沒操作自動收起：切換錶面、夜間模式、鬧鐘、設定 |
| 狀態列 | 左上日期（＋下一次鬧鐘），右上電量與充電狀態 |
| 設定 | 夜間模式、時間格式（跟隨系統／12／24 小時）、顯示秒數、名言輪播、亮度還原 |
| 夜間模式 | 黑底暗紅字，切到夜間亮度（預設 5%） |
| 防烙印 | 所有模式下每分鐘把畫面微移幾 dp（最多 ±10dp），螢幕保持常亮 |
| 調亮度 | 在畫面左右兩側（各 15% 寬）上下滑動；只改本 App 視窗亮度，不動系統設定。白天、夜間各記一組 |
| 充電待機 | 系統螢幕保護程式（DreamService），沿用 App 的錶面、夜間模式、亮度 |
| 名言輪播 | 內建 60 句（聖經和合本、論語、老子各 20 句），每 10 分鐘換一句 |
| 鬧鐘 | 一組鬧鐘：時間、開關、重複星期；系統預設鬧鈴聲＋震動；貪睡 5 分鐘；10 分鐘沒關自動停止；重開機後自動重排 |

## 專案結構

```
clockmath/   純 Kotlin 模組（不依賴 Android）：時間格式、指針角度、電量、防烙印、亮度、手勢判斷、名言、鬧鐘時間
app/         Android App
  data/      設定（SharedPreferences）、時間更新、電池狀態
  ui/        時鐘畫面、錶面、控制列、設定頁、鬧鐘頁
  alarm/     鬧鐘排程、響鈴通知、響鈴畫面、開機重排
  ClockDreamService.kt   充電待機螢幕保護程式
```

## 建置

需要 JDK 17 以上、Android SDK（compileSdk 37）。

```bash
./gradlew assembleDebug
```

APK 會在 `app/build/outputs/apk/debug/app-debug.apk`。安裝到手機：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

用 Android Studio 開啟時，需要支援 AGP 9.5 的版本。

## 測試

```bash
# ClockMath 的 JVM 單元測試（快，不需要 Android）
./gradlew test

# Robolectric 測試：畫面截圖預覽（輸出到 build/preview/）＋鬧鐘流程
# 第一次會下載約 200MB 的模擬 Android 環境
./gradlew :app:testDebugUnitTest -Probo
```

## 手機上的設定

**充電時自動顯示時鐘**：設定 → 顯示 → 螢幕保護程式 → 選「待機時鐘」，啟動時機選「充電時」。
App 的設定頁有按鈕可以直接跳過去。有些廠牌的系統把這個選項藏起來或拿掉了，那種手機只能手動開 App。

**鬧鐘**：第一次開鬧鐘時，Android 13 以上會詢問通知權限，請允許。鬧鐘頁會列出還缺哪些權限，並有按鈕可前往設定。
建議在系統的電池設定裡，把本 App 設為「不受限制」，避免部分廠牌的省電機制干擾。

## 權限

只有鬧鐘需要權限，時鐘本身不需要任何權限。

| 權限 | 用途 |
| --- | --- |
| `USE_EXACT_ALARM`（Android 13+）／`SCHEDULE_EXACT_ALARM`（12 以下） | 鬧鐘準時響 |
| `POST_NOTIFICATIONS` | 響鈴通知（Android 13+ 會詢問） |
| `USE_FULL_SCREEN_INTENT` | 螢幕關著時直接跳出響鈴畫面 |
| `RECEIVE_BOOT_COMPLETED` | 重開機後重新排鬧鐘 |
| `VIBRATE` | 響鈴時震動 |

## 名言來源

全部取自沒有著作權問題的文本：聖經（1919 年《和合本》，公有領域）、《論語》、《老子》。
