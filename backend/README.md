# 🚀 SplitEase Backend & Admin API (Vercel Ready)

این پوشه شامل کدهای کامل بک‌اند برای مدیریت پیام‌های عمومی مدیر، آپدیت‌های اجباری/اختیاری و پیش‌نمایش قابلیت‌های آینده (Teasers) است.

---

## 🛠️ نحوه استقرار رایگان روی Vercel (در کمتر از ۲ دقیقه)

شما می‌توانید کل این پوشه (`backend`) را بدون هیچ هزینه یا سرور پیچیده‌ای روی **Vercel** بالا بیاورید:

1. **نصب Vercel CLI (در صورت تمایل):**
   ```bash
   npm install -g vercel
   ```
2. **ارسال به گیت‌هاب (روش پیشنهادی):**
   - یک مخزن (Repository) جدید در GitHub بسازید و محتویات این پوشه (`package.json`, `server.js`, `api/`) را داخل آن آپلود کنید.
   - وارد سایت [Vercel.com](https://vercel.com) شوید و روی **Add New -> Project** کلیک کنید.
   - مخزن گیت‌هاب خود را انتخاب کنید و دکمه **Deploy** را بزنید!

3. **آدرس دریافت شده از Vercel:**
   بعد از استقرار، Vercel یک آدرس به شما می‌دهد (مثلاً: `https://splitease-backend.vercel.app`).
   آدرس شما آماده است! کافیست این آدرس را در تب **تنظیمات (Settings)** اپلیکیشن اندروید وارد کنید تا برنامه به سرور متصل شود.

---

## 📡 آدرس‌های API (Endpoints)

### 1. دریافت تنظیمات آپدیت و پیام مدیر (توسط اپلیکیشن)
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
