# ⚡ Quick Deploy Commands - Copy & Paste

## 🚀 Deploy trong 5 phút

### 1️⃣ Cài Vercel CLI
```bash
npm install -g vercel
```

### 2️⃣ Login Vercel
```bash
vercel login
# → Nhập email → Check inbox → Click Verify
```

### 3️⃣ Deploy Proxy
```bash
cd /path/to/Home-Harmony/email-proxy
vercel

# Trả lời các câu hỏi:
# Set up and deploy? → Y (Enter)
# Which scope? → Chọn account (Enter)
# Link to existing project? → N (Enter)
# Project name? → home-harmony-email-proxy (Enter)
# Directory? → ./ (Enter)

# ✅ Lưu lại URL: https://home-harmony-email-proxy.vercel.app
```

### 4️⃣ Update Android Code

**File 1:** `app/src/main/java/com/example/furniturecloudy/util/EmailService.kt`

Dòng ~27, thay:
```kotlin
private const val EMAILJS_API_URL = "https://api.emailjs.com/api/v1.0/email/send"
```

Thành (thay YOUR-PROJECT-URL):
```kotlin
private const val EMAILJS_API_URL = "https://YOUR-PROJECT-URL.vercel.app/api/send-otp"
```

**File 2:** `app/src/main/java/com/example/furniturecloudy/util/OTPConfig.kt`

Dòng ~18, thay:
```kotlin
const val DEBUG_BYPASS_OTP = true
```

Thành:
```kotlin
const val DEBUG_BYPASS_OTP = false
```

### 5️⃣ Build & Install
```bash
cd /path/to/Home-Harmony
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 6️⃣ Test
1. Mở app → Login
2. Kiểm tra email → Nhận OTP
3. Nhập OTP → Success! 🎉

---

## 🧪 Test Proxy (Optional)

**Thay YOUR-PROJECT-URL và your-email@gmail.com:**

```bash
curl -X POST https://YOUR-PROJECT-URL.vercel.app/api/send-otp \
  -H "Content-Type: application/json" \
  -d '{
    "service_id": "service_m1pcnmi",
    "template_id": "template_j6qxk8f",
    "user_id": "AO8a042V7FEzfxa-K",
    "template_params": {
      "user_name": "Test",
      "user_email": "your-email@gmail.com",
      "otp_code": "123456",
      "expiry_minutes": 5
    }
  }'
```

**Kết quả mong đợi:**
```json
{"success":true,"message":"Email sent successfully"}
```

Kiểm tra inbox → Nhận email OTP 123456

---

## 🔄 Nếu cần deploy lại

```bash
cd email-proxy
vercel --prod
```

---

## ❌ Troubleshooting Quick Fix

### Lỗi "command not found"
```bash
npm install -g vercel
# Restart terminal
```

### Email không đến
1. Check spam folder
2. Check Logcat:
   ```bash
   adb logcat | grep -i "email\|otp"
   ```

### App crash
```bash
# Clean rebuild
./gradlew clean
./gradlew assembleDebug
```

---

## 📋 Checklist

- [ ] Cài Vercel CLI
- [ ] Login Vercel (check email)
- [ ] Deploy proxy (lưu URL)
- [ ] Update EmailService.kt (URL mới)
- [ ] Tắt DEBUG_BYPASS_OTP
- [ ] Build & install app
- [ ] Test login → Check email → Nhập OTP → Success!

---

**Chi tiết đầy đủ:** Xem file `DEPLOY_EMAIL_PROXY_GUIDE.md`
