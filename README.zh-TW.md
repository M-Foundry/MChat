<div align="center" style="display: flex; justify-content: center; align-items: center; gap: 20px;">

![LOGO](mimo-title.jpg)

</div>

> [!TIP]
> 本譯文由大型語言模型生成，內容可能存在不準確之處。

MChat - 一款為 MiMo 大模型最佳化，專注於原生 Android 體驗的 AI 聊天客戶端

**語言：** [简体中文](README.md) | 繁體中文 | [English](README.en.md) | [日本語](README.ja.md)

![GitHub stars](https://img.shields.io/github/stars/MRoldL001/MChat?style=for-the-badge&label=Stars)
![GitHub Release Downloads](https://img.shields.io/github/downloads/MRoldL001/MChat/total?style=for-the-badge&label=Downloads)
![MiMo](https://img.shields.io/badge/MiMo-console-FF7E00?style=for-the-badge\&logo=xiaomi\&logoColor=white)
![README](https://img.shields.io/badge/MiMo-README-FF7E00?style=for-the-badge\&logo=xiaomi\&logoColor=white)

---

> [!NOTE]
>
> 基於 Kotlin 與 Jetpack Compose 開發的原生 Android AI 聊天客戶端，支援多模型對話、多模態輸入、聯網搜尋、思考模式及 Markdown/LaTeX/程式碼高亮

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square\&logo=kotlin\&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.13.0-02303A?style=flat-square\&logo=gradle\&logoColor=white)
![Compose BOM](https://img.shields.io/badge/Compose%20BOM-2024.12.01-4285F4?style=flat-square\&logo=jetpackcompose\&logoColor=white)
![Target API](https://img.shields.io/badge/Target%20API-36%20%28Android%2016%29-3DDC84?style=flat-square\&logo=android\&logoColor=white)

---

<p align="center">
    <img src="MChat.gif" alt="MChat" width="100">
</p>

## ✨ 特色

- 多模型切換支援
- 對表格、程式碼區塊、分隔線與引用區塊的風格化渲染，以及較完整的 LaTeX 公式渲染（*包含化學公式等額外擴充*）
- 支援帶過程展示的思考模式、可查看來源的聯網搜尋與在對話中新增附件
- 完善的對話歷史管理
- 多種主題可選（包含 Material You 動態取色與自訂顏色）與系統深色模式跟隨，以及更換聊天頁背景圖片
- 自訂系統提示詞與模型參數調整
- 手機端與 Pad 端自動調整佈局
- 流暢的動畫與帶有個人風格的 Material You 風格
- 關於頁裡有個可以捏的 Icon（

## 🚀 快速開始

### 安裝

#### 方式一：從 GitHub Releases 下載（推薦）

1. 前往本專案的 [Releases](https://github.com/MRoldL001/MChat/releases)
2. 下載最新的 `.apk` 檔案
3. 在手機上安裝（可能需要允許未知來源應用）

#### 方式二：從原始碼建置

> [!WARNING]
>
> 對於一般使用者，不建議自行從原始碼建置，因為其較為繁瑣，且建置 `.apk` 時：
>
> 如果建置 `debug` 套件，則安裝包體積會更大，且因為帶有偵錯標籤，無法通過系統安裝器安裝
>
> 如果建置 `releases` 套件，為了安全性開發者所持簽章金鑰不會公開，在自行簽章的情況下每次更新需要用同一組金鑰重新建置安裝包，無法直接更新開發者所發布的 `.apk` 安裝包

1. 使用 `git clone` 將 Repo 克隆到本地
2. 使用 `Android Studio` 或其他任何已安裝 Android 開發外掛程式的 `JetBrains IDE` 開啟專案
3. 同步 `Gradle`
4. 執行或建置 `.apk`，建議自行簽章建置 `releases` 安裝包

### 配置

1. 首次開啟應用程式，點擊右上角設定圖示
2. 設定你的小米 MiMo API Key
3. （可選）設定 API Base URL
4. （可選）設定自訂系統提示詞並調整模型參數
5. 開始聊天！

## 📱 裝置需求

- Android 8 (API 26) 或更高版本
- 動態取色主題僅支援 Android 12 (API 31) 或更高版本
- 預測性返回僅支援 Android 14 (API 34) 或更高版本
- 即時通知僅支援 Android 16 (API 36) 或更高版本

## 📚 多語言支援情況

| 語言                 | 支援情況 |
| -------------------- | -------- |
| 简体中文（簡體中文） | 支援     |
| 繁體中文             | AI 翻譯  |
| English（英語）      | AI 翻譯  |
| 日本語（日語）       | AI 翻訳  |

## 🎨 主題

應用支援多種主題顏色：

| 主題           | 亮色模式     | 暗色模式     |
| -------------- | ------------ | ------------ |
| 墨白素箋(預設) | `#000000`    | `#FFFFFF`    |
| 動態取色       | 系統動態提取 | 系統動態提取 |
| 自訂色彩       | 自訂         | 自訂         |
| 初音之籟       | `#39C5BB`    | `#39C5BB`    |
| 吉原哀歌       | `#D93A49`    | `#D93A49`    |
| 米柚微光       | `#FF7E00`    | `#E86F00`    |
| 寂靜碧語       | `#5EA0AA`    | `#8FCED0`    |
| 森盎青影       | `#006E2A`    | `#5CDB78`    |
| 永恆幽夢       | `#6650A4`    | `#D0BCFF`    |
| 深海沉藍       | `#0B57D0`    | `#A8C7FA`    |

## 📖 使用說明

### 傳送訊息

在底部輸入框輸入內容，點擊傳送按鈕傳送訊息

### 切換模型

點擊頂部目前的模型名稱，在彈出的模型選擇器中選擇其他模型

### 查看思考過程

當 AI 啟用思考模式時，可以點擊展開「思考過程」卡片查看 AI 的推論過程

### 查看搜尋結果

當 AI 啟用聯網搜尋時，可以點擊展開「搜尋結果」卡片查看 AI 的資訊來源

### 新增附件

點擊輸入框左側的按鈕即可為該次對話加入附件（部分模型可能不支援）

### 程式碼區塊

程式碼區塊支援查看特定語言圖示與程式碼高亮

- 全屏顯示：程式碼區塊右上角帶有全屏按鈕，點擊即可展開程式碼區塊至全屏

- 複製程式碼：程式碼區塊右上角帶有複製按鈕，點擊即可複製程式碼內容

### 管理對話

- 新增對話：點擊側邊欄新增按鈕
- 選擇對話：點擊側邊欄中的歷史對話
- 刪除對話：在對話列表中點擊刪除圖示，然後長按確認刪除
- 重新產生：點擊最新一條對話下方的環狀按鈕以重新產生
- 修改後重新產生：點擊最新一條對話下方的筆狀按鈕，然後在輸入框中修改內容後再次點擊傳送以重新產生
- 撤回修改：在點擊最新一條對話下方的筆狀按鈕後，如果想恢復對話內容，可以點擊輸入框左側的箭頭狀按鈕以撤回修改操作

## 🔧 進階功能

### 聯網功能

啟用後，AI 會聯網搜尋即時資訊並給出資訊來源

### 思考模式

啟用後，AI 會展示其思考推論過程

### 技能模式

- **詩人模式**：將你輸入的內容轉化為古體詩
- **學習模式**：採用循序漸進的教學方式，幫助你學習新知識

### 附件上傳

點擊輸入框左側的加號按鈕以拍攝照片或上傳附件，支援圖片、音訊與視訊及**混合上傳**

### 自訂提示詞

在設定中可以新增自訂系統提示詞，自訂 AI 的行為和風格

### 參數設定

在設定中可以更改模型的各項參數以個人化體驗

## 🗒️ 更新日誌

見 [CHANGELOG.md](CHANGELOG.md)

## 免責聲明

### 非官方聲明

本應用（MChat）為第三方開發的開源客戶端，**與小米公司（Xiaomi Corporation）及其關聯公司（下稱小米公司）無任何隸屬、授權或合作關係**。

### 智慧財產權

"小米"、"Xiaomi"、"MiMo" 等商標及圖形標識歸小米公司所有。本應用使用上述標識僅用於描述功能相容性（**即指示性合理使用**）。

### 隱私保護

本應用所有資料均儲存於本地裝置，對話直接透過 API 與小米伺服器通訊，開發者不收集或儲存任何使用者資料。

### 風險承擔

**本應用按「as-is」原則提供，不包含任何明示或暗示的保證**。使用者需自行承擔使用本應用帶來的風險，包含但不限於資料遺失、隱私洩露或裝置損壞。開發者與小米公司不對本應用的合法性、安全性及功能性負責。

### 回饋與支援

若在使用中遇到問題，請前往 [Issues](https://github.com/MRoldL001/MChat/issues) 回饋，若有能力也可嘗試 fork 原始碼自行修改後發起 PR。
