# 📧 Email Proxy for Home Harmony

## ❓ Tại sao cần Proxy?

**Vấn đề:** EmailJS chỉ cho phép gọi API từ **Browser** (JavaScript), không cho phép gọi trực tiếp từ **Native Mobile App** (Android/iOS) vì lý do bảo mật.

**Giải pháp:** Proxy server này hoạt động như một **trung gian**:
```
Android App → Proxy Server → EmailJS API → Email được gửi
```

---

## 🏗️ Cấu trúc Project

```
email-proxy/
├── api/
│   └── send-otp.js      ← Serverless function (Node.js)
├── vercel.json          ← Vercel config
├── package.json         ← Dependencies
└── README.md            ← File này
```

---

## 🔍 Code Proxy hoạt động như thế nào?

### File: `api/send-otp.js`

**Input từ Android app:**
```json
POST /api/send-otp
{
  "service_id": "service_m1pcnmi",
  "template_id": "template_j6qxk8f",
  "user_id": "AO8a042V7FEzfxa-K",
  "template_params": {
    "user_name": "John Doe",
    "user_email": "user@example.com",
    "otp_code": "472891",
    "expiry_minutes": 5
  }
}
```

**Proxy xử lý:**
1. Validate request (check required fields)
2. Forward request đến EmailJS API
3. Nhận response từ EmailJS
4. Trả về Android app

**Output về Android app:**
```json
{
  "success": true,
  "message": "Email sent successfully"
}
```

**Nếu lỗi:**
```json
{
  "error": "Failed to send email",
  "details": "..."
}
```

---

## 🚀 Deploy lên Vercel (5 phút)

### Bước 1: Cài Vercel CLI

```bash
npm install -g vercel
```

### Bước 2: Login

```bash
vercel login
# Nhập email → Check inbox → Click verify
```

### Bước 3: Deploy

```bash
cd email-proxy
vercel

# Trả lời câu hỏi:
# Set up and deploy? → Y
# Which scope? → Chọn account
# Link to existing project? → N
# Project name? → home-harmony-email-proxy
# Directory? → ./
```

### Bước 4: Lưu URL

Sau khi deploy, bạn sẽ nhận được URL:
```
✅ Production: https://home-harmony-email-proxy.vercel.app
```

**API Endpoint:**
```
https://home-harmony-email-proxy.vercel.app/api/send-otp
```

---

## 🔧 Update Android Code

### File 1: `EmailService.kt`

**Dòng ~27:**
```kotlin
// Thay:
private const val EMAILJS_API_URL = "https://api.emailjs.com/api/v1.0/email/send"

// Bằng (thay YOUR-URL):
private const val EMAILJS_API_URL = "https://home-harmony-email-proxy.vercel.app/api/send-otp"
```

### File 2: `OTPConfig.kt`

**Dòng ~18:**
```kotlin
// Tắt DEBUG mode:
const val DEBUG_BYPASS_OTP = false
```

---

## 🧪 Test Proxy

### Test bằng curl:

```bash
curl -X POST https://YOUR-URL.vercel.app/api/send-otp \
  -H "Content-Type: application/json" \
  -d '{
    "service_id": "service_m1pcnmi",
    "template_id": "template_j6qxk8f",
    "user_id": "AO8a042V7FEzfxa-K",
    "template_params": {
      "user_name": "Test User",
      "user_email": "your-email@gmail.com",
      "otp_code": "123456",
      "expiry_minutes": 5
    }
  }'
```

**Thay:**
- `YOUR-URL` → URL Vercel của bạn
- `your-email@gmail.com` → Email thật của bạn

**Kết quả mong đợi:**
```json
{"success":true,"message":"Email sent successfully"}
```

**Kiểm tra inbox** → Nhận email OTP 123456

---

## 🔒 Bảo mật

### ✅ An toàn:
- Proxy **không lưu** bất kỳ data nào
- HTTPS được Vercel tự động enable
- CORS enabled cho mobile app
- EmailJS credentials được gửi từ app (không hard-code trong proxy)

### ⚠️ Lưu ý:
- **KHÔNG commit** credentials vào Git
- Credentials được lưu trong Android app (`OTPConfig.kt`)
- ProGuard sẽ obfuscate credentials trong release build

---

## 📊 Vercel Free Tier

| Feature | Limit |
|---------|-------|
| Requests | Unlimited |
| Bandwidth | 100 GB/month |
| Deployments | Unlimited |
| Functions | 100 GB-Hours |
| HTTPS | ✅ Auto |
| Custom Domain | ✅ Supported |
| Cost | **FREE** |

**Đủ cho:** ~1,000,000 emails/tháng (EmailJS limit: 200/month free)

---

## 🔄 Redeploy (nếu có thay đổi)

Nếu bạn modify code proxy:

```bash
cd email-proxy
vercel --prod
```

Vercel sẽ deploy version mới, URL giữ nguyên.

---

## 🐛 Troubleshooting

### ❌ Error: "Method not allowed"

**Nguyên nhân:** GET request thay vì POST

**Giải pháp:** Dùng POST request (xem phần Test)

---

### ❌ Error: "Missing required fields"

**Nguyên nhân:** Request thiếu `service_id`, `template_id`, `user_id` hoặc `template_params`

**Giải pháp:** Check request body có đầy đủ fields

---

### ❌ Error: "Failed to send email"

**Nguyên nhân:** EmailJS API trả về lỗi

**Kiểm tra:**
1. EmailJS credentials đúng chưa? (`OTPConfig.kt`)
2. Template ID đúng chưa?
3. EmailJS account còn quota chưa? (free: 200/month)

**Debug:**
- Xem Vercel logs: Dashboard → Project → Deployments → View Logs
- Xem Android Logcat: `adb logcat | grep -i "email\|otp"`

---

## 📝 Code Details

### CORS Headers

```javascript
res.setHeader('Access-Control-Allow-Origin', '*');
res.setHeader('Access-Control-Allow-Methods', 'POST');
res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
```

**Giải thích:** Cho phép Android app gọi API từ bất kỳ domain nào.

### Request Validation

```javascript
if (!service_id || !template_id || !user_id || !template_params) {
  return res.status(400).json({ error: 'Missing required fields' });
}
```

**Giải thích:** Validate input trước khi forward đến EmailJS.

### Forward to EmailJS

```javascript
const response = await fetch('https://api.emailjs.com/api/v1.0/email/send', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ service_id, template_id, user_id, template_params })
});
```

**Giải thích:** Gọi EmailJS API với data từ Android app.

---

## 📞 Hỗ trợ

**Nếu gặp vấn đề:**

1. Check **Troubleshooting** section
2. Xem Vercel logs
3. Xem Android Logcat
4. Test proxy bằng curl

**Tài liệu:**
- Vercel: https://vercel.com/docs
- EmailJS: https://www.emailjs.com/docs

---

## 📚 Tài liệu liên quan

- `../DEPLOY_EMAIL_PROXY_GUIDE.md` - Hướng dẫn deploy chi tiết
- `../QUICK_DEPLOY_COMMANDS.md` - Commands copy/paste nhanh
- `../docs/TWO_FACTOR_AUTHENTICATION.md` - 2FA documentation

---

**Author:** Claude Code
**Date:** 2025-12-11
**Project:** Home Harmony - Furniture E-Commerce App
