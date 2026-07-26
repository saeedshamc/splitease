const express = require('express');
const cors = require('cors');
const app = express();

app.use(cors());
app.use(express.json());

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

// GET /api/config - App fetches this on startup
app.get('/api/config', (req, res) => {
  res.status(200).json(appConfigState);
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

  res.status(200).json({ success: true, updatedPolicy: appConfigState.updatePolicy });
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

  res.status(200).json({ success: true, updatedMessage: appConfigState.activeMessage });
});

const PORT = process.env.PORT || 3000;
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`✅ SplitEase Backend running on http://localhost:${PORT}`);
  });
}

module.exports = app;
