const express = require('express');
const cors = require('cors');
const path = require('path');
const app = express();

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// In-Memory / Sample Database for Update Policy and Admin Messages
let appConfigState = {
  updatePolicy: {
    latestVersionCode: 1,
    latestVersionName: "1.0.0",
    isMandatory: false,
    releaseNotes: "✨ قابلیت‌های جدید:\n• اضافه شدن سیستم سفر خانواده‌ها با تعداد نفرات\n• جستجو و فیلتر پیشرفته در تاریخچه هزینه‌ها\n• خروجی اکسل و PDF فارسی",
    upcomingFeaturesTeaser: "🚀 قابلیت‌های در راه (نسخه بعدی): قابلیت همگام‌سازی ابری و ایجاد حساب کاربری اعضا به زودی اضافه می‌شود!",
    downloadUrl: "https://my-splitease.com/download"
  },
  activeMessage: {
    id: "msg_welcome",
    title: "📢 پیام مدیر سیستم (SplitEase)",
    message: "به برنامه محاسبه دنگ و هزینه‌های SplitEase خوش آمدید! تمامی اطلاعات شما در حال حاضر روی حافظه امن دستگاه شما ذخیره می‌شود.",
    type: "INFO", // "INFO", "WARNING", "FEATURE_TEASER"
    isActive: true,
    timestamp: Date.now()
  }
};

let activityLogs = [
  { id: 1, action: "بروزرسانی پیام عمومی سیستم (خوش‌آمدگویی)", type: "MESSAGE", timestamp: Date.now() - 1800000, ip: "185.192.112.45", userId: "usr_admin", userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64)", latency: "45ms", location: "تهران (IR)" },
  { id: 2, action: "انتشار تنظیمات آپدیت نسخه 1.0.0 (اختیاری)", type: "UPDATE", timestamp: Date.now() - 3600000, ip: "185.192.112.45", userId: "usr_admin", userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)", latency: "62ms", location: "تهران (IR)" },
  { id: 3, action: "بررسی وضعیت سرورهای Vercel و اتصال دیتابیس", type: "SYSTEM", timestamp: Date.now() - 5400000, ip: "76.76.21.21", userId: "system_cron", userAgent: "Vercel-Monitor/2.1", latency: "12ms", location: "فرانکفورت (DE)" },
  { id: 4, action: "خطا در همگام‌سازی ابری یکی از کاربران (پاسخ 500 از سرور)", type: "ERROR", timestamp: Date.now() - 7200000, ip: "91.240.118.89", userId: "usr_8492", userAgent: "SplitEase-Android/1.0 (Android 14)", latency: "1420ms", location: "شیراز (IR)" },
  { id: 5, action: "هشدار: تاخیر بالا در پاسخگویی سرور دیتابیس (بیش از 800ms)", type: "WARNING", timestamp: Date.now() - 10800000, ip: "10.0.0.42", userId: "db_monitor", userAgent: "Postgres-HealthCheck/1.0", latency: "845ms", location: "دیتاسنتر داخلی" },
  { id: 6, action: "موفقیت: بکاپ‌گیری خودکار اطلاعات کل کاربران انجام شد", type: "SUCCESS", timestamp: Date.now() - 14400000, ip: "10.0.0.101", userId: "backup_daemon", userAgent: "AWS-S3-Sync/3.4", latency: "310ms", location: "دیتاسنتر داخلی" },
  { id: 7, action: "ارسال اعلان عمومی برای نگهداری دوره‌ای سرورها", type: "MESSAGE", timestamp: Date.now() - 18000000, ip: "185.192.112.45", userId: "usr_admin", userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64)", latency: "38ms", location: "تهران (IR)" },
  { id: 8, action: "خطای تلاش ناموفق برای ورود به پنل ادمین با رمز عبور اشتباه", type: "ERROR", timestamp: Date.now() - 21600000, ip: "45.146.165.20", userId: "anonymous", userAgent: "python-requests/2.31.0", latency: "18ms", location: "آمستردام (NL)" }
];

let userEngagement7Days = [
  { day: "شنبه", activeUsers: 142 },
  { day: "یکشنبه", activeUsers: 198 },
  { day: "دوشنبه", activeUsers: 245 },
  { day: "سه‌شنبه", activeUsers: 310 },
  { day: "چهارشنبه", activeUsers: 289 },
  { day: "پنج‌شنبه", activeUsers: 412 },
  { day: "جمعه", activeUsers: 480 }
];

let userEngagement30Days = [
  { day: "هفته ۱", activeUsers: 1250 },
  { day: "هفته ۲", activeUsers: 1680 },
  { day: "هفته ۳", activeUsers: 2100 },
  { day: "هفته ۴", activeUsers: 2450 }
];

let userEngagement90Days = [
  { day: "فروردین", activeUsers: 5400 },
  { day: "اردیبهشت", activeUsers: 6850 },
  { day: "خرداد", activeUsers: 8920 }
];

// GET /api/config - App fetches this on startup
app.get('/api/config', (req, res) => {
  res.status(200).json(appConfigState);
});

// GET /api/admin/stats - Admin dashboard statistics and audit logs
app.get('/api/admin/stats', (req, res) => {
  res.status(200).json({
    activityLogs: activityLogs.slice(0, 25),
    userEngagement: userEngagement7Days,
    userEngagement30Days,
    userEngagement90Days
  });
});

// DELETE /api/admin/logs - Admin purges audit history
app.delete('/api/admin/logs', (req, res) => {
  activityLogs = [
    { id: Date.now(), action: "تاریخچه فعالیت‌ها و لاگ‌های سرور توسط مدیر پاکسازی شد", type: "WARNING", timestamp: Date.now() }
  ];
  res.status(200).json({ success: true, activityLogs });
});

// POST /api/admin/logs/delete-bulk - Admin removes specific selected logs
app.post('/api/admin/logs/delete-bulk', (req, res) => {
  const { ids } = req.body;
  if (Array.isArray(ids) && ids.length > 0) {
    activityLogs = activityLogs.filter(log => !ids.includes(log.id));
  }
  res.status(200).json({ success: true, activityLogs });
});

// POST /api/admin/update-policy - Admin panel updates the app version requirements
app.post('/api/admin/update-policy', (req, res) => {
  const { latestVersionCode, latestVersionName, isMandatory, releaseNotes, upcomingFeaturesTeaser, downloadUrl } = req.body;
  if (latestVersionCode !== undefined) appConfigState.updatePolicy.latestVersionCode = latestVersionCode;
  if (latestVersionName !== undefined) appConfigState.updatePolicy.latestVersionName = latestVersionName;
  if (isMandatory !== undefined) appConfigState.updatePolicy.isMandatory = isMandatory;
  if (releaseNotes !== undefined) appConfigState.updatePolicy.releaseNotes = releaseNotes;
  if (upcomingFeaturesTeaser !== undefined) appConfigState.updatePolicy.upcomingFeaturesTeaser = upcomingFeaturesTeaser;
  if (downloadUrl !== undefined) appConfigState.updatePolicy.downloadUrl = downloadUrl;

  activityLogs.unshift({
    id: Date.now(),
    action: `تنظیم آپدیت جدید برای نسخه ${latestVersionName || appConfigState.updatePolicy.latestVersionName} (${isMandatory ? 'اجباری' : 'اختیاری'})`,
    type: "UPDATE",
    timestamp: Date.now(),
    ip: req.ip || "185.192.112.45",
    userId: "usr_admin",
    userAgent: req.get('User-Agent') || "Admin Dashboard Web",
    latency: Math.floor(Math.random() * 50 + 20) + "ms",
    location: "تهران (IR)"
  });
  if (activityLogs.length > 20) activityLogs.pop();

  res.status(200).json({ success: true, updatedPolicy: appConfigState.updatePolicy, activityLogs });
});

// POST /api/admin/message - Admin sends a broadcast announcement
app.post('/api/admin/message', (req, res) => {
  const { title, message, type, isActive } = req.body;
  appConfigState.activeMessage = {
    id: "msg_" + Date.now(),
    title: title || "پیام مدیر",
    message: message || "",
    type: type || "INFO",
    isActive: isActive !== undefined ? isActive : true,
    timestamp: Date.now()
  };

  activityLogs.unshift({
    id: Date.now(),
    action: `ارسال پیام عمومی: ${title || "پیام مدیر"} (${isActive !== false ? 'فعال' : 'غیرفعال'})`,
    type: "MESSAGE",
    timestamp: Date.now(),
    ip: req.ip || "185.192.112.45",
    userId: "usr_admin",
    userAgent: req.get('User-Agent') || "Admin Dashboard Web",
    latency: Math.floor(Math.random() * 50 + 20) + "ms",
    location: "تهران (IR)"
  });
  if (activityLogs.length > 20) activityLogs.pop();

  res.status(200).json({ success: true, updatedMessage: appConfigState.activeMessage, activityLogs });
});

// Admin Dashboard Web Routes
app.get(['/', '/admin', '/dashboard'], (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

const PORT = process.env.PORT || 3000;
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`✅ SplitEase Backend running on http://localhost:${PORT}`);
  });
}

module.exports = app;
