# 🚀 SplitEase Backend & Graphical Admin Dashboard (Vercel Ready)

این پوشه شامل کدهای کامل بک‌اند به همراه **پنل مدیریت گرافیکی حرفه‌ای (Web Dashboard)** برای مدیریت پیام‌های عمومی مدیر، آپدیت‌های اجباری/اختیاری و پیش‌نمایش قابلیت‌های آینده (Teasers) است.

---

## 🎨 پنل مدیریت گرافیکی (Web Dashboard)
پس از استقرار روی Vercel (یا اجرای محلی)، کافیست آدرس اصلی سایت (مثلاً `https://your-domain.vercel.app` یا `http://localhost:3000`) را در مرورگر خود باز کنید!
شما به یک پنل مدیریت کاملاً گرافیکی، زیبا و فارسی با قابلیت‌های زیر دسترسی خواهید داشت:
- 📱 **شبیه‌ساز زنده موبایل (Live Simulator):** مشاهده دقیق شکل ظاهری پاپ‌آپ آپدیت و پیام مدیر قبل از ارسال به کاربران!
- 🔄 **تغییر آنی وضعیت آپدیت:** روشن و خاموش کردن آپدیت اجباری یا اختیاری با یک کلیک.
- ✍️ **نوشتن توضیحات نسخه و قابلیت‌های آینده (Teaser):** وارد کردن متن‌های دلخواه برای ایجاد اشتیاق در کاربران.
- 📢 **ارسال پیام عمومی (Broadcast):** ارسال انواع اطلاعیه‌های عمومی، هشدارهای سرور یا معرفی قابلیت‌ها با رنگ‌بندی‌های اختصاصی.

---

## 🛠️ نحوه استقرار رایگان روی Vercel (در کمتر از ۲ دقیقه)

شما می‌توانید کل این پوشه (`backend`) را بدون هیچ هزینه یا سرور پیچیده‌ای روی **Vercel** بالا بیاورید:

1. **ارسال به گیت‌هاب (روش پیشنهادی):**
   - یک مخزن (Repository) جدید در GitHub بسازید و محتویات این پوشه (`package.json`, `server.js`, `api/`, `public/`, `vercel.json`) را داخل آن آپلود کنید.
   - وارد سایت [Vercel.com](https://vercel.com) شوید و روی **Add New -> Project** کلیک کنید.
   - مخزن گیت‌هاب خود را انتخاب کنید و دکمه **Deploy** را بزنید!

2. **آدرس دریافت شده از Vercel:**
   بعد از استقرار، Vercel یک آدرس به شما می‌دهد (مثلاً: `https://splitease-backend.vercel.app`).
   - اگر این آدرس را در مرورگر باز کنید، **پنل مدیریت گرافیکی** نمایش داده می‌شود.
   - اگر این آدرس را در تب **تنظیمات (Settings)** اپلیکیشن اندروید وارد کنید، برنامه اطلاعات آپدیت و پیام‌ها را به صورت خودکار دریافت می‌کند!

---

## 📡 آدرس‌های API (Endpoints) برای توسعه‌دهندگان

### 1. دریافت تنظیمات آپدیت و پیام مدیر (توسط اپلیکیشن موبایل)
- **Method:** `GET`
- **URL:** `https://your-domain.vercel.app/api/config`
- **نمونه خروجی:**
  ```json
  {
    "updatePolicy": {
      "latestVersionCode": 2,
      "latestVersionName": "2.0.0",
      "isMandatory": false,
      "releaseNotes": "✨ قابلیت‌های جدید اضافه شد...",
      "upcomingFeaturesTeaser": "🚀 قابلیت همگام‌سازی ابری به زودی اضافه می‌شود!",
      "downloadUrl": "https://my-splitease.com/download"
    },
    "activeMessage": {
      "id": "msg_101",
      "title": "📢 پیام مهم مدیر",
      "message": "سرورها با موفقیت ارتقا یافتند.",
      "type": "INFO",
      "isActive": true
    }
  }
  ```

### 2. تغییر تنظیمات آپدیت (توسط پنل ادمین شما یا Postman)
- **Method:** `POST`
- **URL:** `https://your-domain.vercel.app/api/admin/update-policy`
- **Body (JSON):**
  ```json
  {
    "latestVersionCode": 2,
    "latestVersionName": "2.1.0",
    "isMandatory": true,
    "releaseNotes": "توضیحات آپدیت اجباری...",
    "upcomingFeaturesTeaser": "قابلیت‌های در راه..."
  }
  ```

### 3. ارسال پیام عمومی مدیر به کاربران
- **Method:** `POST`
- **URL:** `https://your-domain.vercel.app/api/admin/message`
- **Body (JSON):**
  ```json
  {
    "title": "📢 اطلاعیه جدید",
    "message": "نسخه جدید برنامه منتشر شد!",
    "type": "FEATURE_TEASER",
    "isActive": true
  }
  ```
