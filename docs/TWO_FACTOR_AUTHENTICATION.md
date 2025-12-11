# 🔐 Two-Factor Authentication (2FA) - Complete Documentation

## 📋 Tổng quan

Home Harmony app hiện đã tích hợp **Two-Factor Authentication (2FA)** với OTP qua email để tăng cường bảo mật.

**Triển khai:** Full version với UI đẹp, animations, và debug mode
**Thời gian:** ~11-13 giờ
**Ngày hoàn thành:** 2025-12-11

---

## ✨ Features

### Core Features
- ✅ **6-digit OTP code generation**
- ✅ **Email delivery via EmailJS**
- ✅ **Firestore storage** với expiry time (5 phút)
- ✅ **OTP verification** với rate limiting (3 attempts max)
- ✅ **Resend OTP** với cooldown (60 giây)

### UI Features
- ✅ **Custom 6-box OTP input** với auto-focus
- ✅ **Beautiful BottomSheet UI**
- ✅ **Countdown timer** (60 giây)
- ✅ **Shake animation** khi sai OTP
- ✅ **Success animation**
- ✅ **Error messages** rõ ràng
- ✅ **Paste support** (paste "123456" tự động fill)

### Security Features
- ✅ **Rate limiting:** Max 3 attempts
- ✅ **OTP expiry:** 5 phút
- ✅ **One-time use:** OTP invalidated sau khi verify
- ✅ **Resend cooldown:** 60 giây
- ✅ **Debug mode:** Bypass OTP cho testing (có thể bật/tắt)

---

## 🎯 User Flow

```
1. User nhập email + password
         ↓
2. Kiểm tra credentials (Firebase Auth)
         ↓
3. ✅ Login thành công
         ↓
4. Generate OTP 6 số (VD: 472891)
   → Lưu vào Firestore
   → Gửi qua email
         ↓
5. Show OTP BottomSheet
   - 6 ô input đẹp
   - Countdown 60s
   - Email masked: m***y@gmail.com
         ↓
6. User nhập OTP
         ↓
7. Verify OTP:
   ✅ Đúng → Navigate to ShoppingActivity
   ❌ Sai → Shake animation + error
   ⏱️ Hết hạn → "Vui lòng gửi lại"
```

---

## 🔧 Configuration

### Debug Mode (⚠️ QUAN TRỌNG!)

**File:** `util/OTPConfig.kt`

```kotlin
object OTPConfig {
    /**
     * DEBUG MODE - Bypass OTP verification for testing
     * ⚠️ MUST BE FALSE IN PRODUCTION!
     */
    const val DEBUG_BYPASS_OTP = true  // Set to false before release
    const val DEBUG_OTP_CODE = "123456"  // Any 6-digit code works in debug

    // ... other settings
}
```

**Khi `DEBUG_BYPASS_OTP = true`:**
- ✅ OTP vẫn được generate và gửi email
- ✅ Nhưng **BẤT KỲ** mã 6 số nào cũng được chấp nhận!
- ✅ Hoặc dùng mã debug: `123456`
- ✅ UI hiển thị warning: "⚠️ DEBUG MODE: Any 6-digit code works"

**Để test:**
1. Login bình thường
2. OTP BottomSheet hiện ra
3. Nhập **BẤT KỲ** 6 số nào (VD: 111111, 999999)
4. ✅ Success!

**Trước khi release production:**
```kotlin
const val DEBUG_BYPASS_OTP = false  // ⚠️ BẮT BUỘC!
```

---

### EmailJS Setup (Required for Production)

**File:** `util/OTPConfig.kt`

```kotlin
// EmailJS Configuration (Get from https://www.emailjs.com/)
const val EMAILJS_SERVICE_ID = "YOUR_SERVICE_ID"      // TODO: Update
const val EMAILJS_TEMPLATE_ID = "YOUR_TEMPLATE_ID"    // TODO: Update
const val EMAILJS_USER_ID = "YOUR_USER_ID"            // TODO: Update
```

**Setup steps:**

1. **Tạo account EmailJS** (Free tier: 200 emails/month)
   - Go to: https://www.emailjs.com/
   - Sign up (free)

2. **Add email service**
   - Dashboard → Email Services
   - Add service (Gmail, Outlook, etc.)
   - Follow instructions to connect

3. **Create email template**
   - Dashboard → Email Templates
   - Create template với variables:

```
Subject: Your Home Harmony OTP Code

Body:
Hello {{user_name}},

Your OTP code for Home Harmony is:

{{otp_code}}

This code will expire in {{expiry_minutes}} minutes.

If you didn't request this code, please ignore this email.

Best regards,
Home Harmony Team
```

4. **Copy credentials**
   - Service ID: `service_xxxxx`
   - Template ID: `template_xxxxx`
   - User ID: `user_xxxxxxxxxxxxx`

5. **Update OTPConfig.kt**
```kotlin
const val EMAILJS_SERVICE_ID = "service_xxxxx"
const val EMAILJS_TEMPLATE_ID = "template_xxxxx"
const val EMAILJS_USER_ID = "user_xxxxxxxxxxxxx"
```

**⚠️ Nếu chưa setup EmailJS:**
- Debug mode sẽ simulate email send
- OTP vẫn được generate và lưu vào Firestore
- Check Logcat để thấy OTP code

---

### OTP Settings

**File:** `util/OTPConfig.kt`

```kotlin
const val OTP_LENGTH = 6                  // 6 digits
const val OTP_EXPIRY_MINUTES = 5          // Expire after 5 minutes
const val OTP_MAX_ATTEMPTS = 3            // Max 3 wrong attempts
const val RESEND_COOLDOWN_SECONDS = 60    // Wait 60s before resend
```

**Có thể customize:**
- OTP length: 4, 6, hoặc 8 digits
- Expiry time: 5-15 minutes
- Max attempts: 3-5 attempts
- Cooldown: 30-120 seconds

---

## 📁 File Structure

### New Files Created (15 files)

```
app/src/main/java/com/example/furniturecloudy/
├── util/
│   ├── OTPConfig.kt                    ✅ Configuration constants
│   ├── OTPManager.kt                   ✅ Core OTP logic
│   └── EmailService.kt                 ✅ Email sending (EmailJS)
├── data/
│   ├── OTPCode.kt                      ✅ OTP data model
│   └── User.kt                         🔄 Updated (added 2FA fields)
├── model/viewmodel/
│   └── OTPViewModel.kt                 ✅ OTP ViewModel
├── view/
│   └── OTPInputView.kt                 ✅ Custom 6-box input
└── present/fragments/loginRegister/
    ├── OTPBottomSheetFragment.kt       ✅ OTP BottomSheet
    └── LoginFragment.kt                🔄 Updated (OTP flow)

app/src/main/res/
├── layout/
│   ├── view_otp_input.xml              ✅ 6-box layout
│   └── bottom_sheet_otp_verification.xml  ✅ BottomSheet layout
├── drawable/
│   ├── bg_otp_box.xml                  ✅ Box background
│   ├── bg_bottom_sheet.xml             ✅ Sheet background
│   └── bg_bottom_sheet_handle.xml      ✅ Handle bar
└── anim/
    └── shake_animation.xml             ✅ Shake animation

docs/
└── TWO_FACTOR_AUTHENTICATION.md        ✅ This file
```

---

## 🧪 Testing Guide

### Test Case 1: Debug Mode (Quick Test)

**Setup:**
```kotlin
// OTPConfig.kt
const val DEBUG_BYPASS_OTP = true
```

**Steps:**
1. Launch app
2. Login với email/password
3. OTP BottomSheet hiện ra
4. Nhập **bất kỳ** 6 số nào (VD: 111111)
5. ✅ Success → Navigate to Shopping

**Expected:**
- BottomSheet show warning: "⚠️ DEBUG MODE"
- Any 6-digit code được chấp nhận
- Log shows: "⚠️ OTP DEBUG MODE ENABLED"

---

### Test Case 2: Real OTP (with EmailJS)

**Setup:**
```kotlin
// OTPConfig.kt
const val DEBUG_BYPASS_OTP = false
// EmailJS configured
```

**Steps:**
1. Login
2. Check email cho OTP code (VD: 472891)
3. Nhập đúng OTP
4. ✅ Success

**Expected:**
- Email nhận được OTP
- Đúng OTP → Success
- Sai OTP → Shake + error

---

### Test Case 3: Wrong OTP (3 attempts)

**Steps:**
1. Login
2. Nhập sai OTP lần 1 → "Còn 2 lần thử"
3. Nhập sai OTP lần 2 → "Còn 1 lần thử"
4. Nhập sai OTP lần 3 → "Locked"
5. Cần gửi lại OTP mới

---

### Test Case 4: OTP Expiry

**Steps:**
1. Login
2. Đợi 5 phút
3. Nhập OTP → "Mã đã hết hạn"
4. Click "Gửi lại"
5. Nhập OTP mới

---

### Test Case 5: Resend OTP

**Steps:**
1. Login
2. Click "Gửi lại" ngay → Disabled (countdown 60s)
3. Đợi 60 giây
4. Click "Gửi lại" → Enabled
5. Nhận OTP mới

---

### Test Case 6: Paste OTP

**Steps:**
1. Login
2. Copy OTP từ email: "472891"
3. Paste vào box đầu tiên
4. ✅ Tự động fill 6 boxes
5. Auto-submit

---

## 🐛 Troubleshooting

### Issue 1: Email không nhận được

**Nguyên nhân:**
- EmailJS chưa config
- Service ID/Template ID sai
- Email service chưa verify

**Giải pháp:**
1. Check OTPConfig.kt có đúng credentials không
2. Login EmailJS dashboard kiểm tra quota
3. Test template trực tiếp trên EmailJS
4. Enable DEBUG_BYPASS_OTP để test không cần email

---

### Issue 2: BottomSheet không hiện

**Nguyên nhân:**
- Fragment manager issue
- Navigation issue

**Giải pháp:**
1. Check Logcat cho errors
2. Verify LoginFragment code updated đúng
3. Check `TWO_FACTOR_ENABLED_BY_DEFAULT = true`

---

### Issue 3: OTP input không hoạt động

**Nguyên nhân:**
- View binding issue
- EditText focus issue

**Giải pháp:**
1. Clean & rebuild project
2. Check OTPInputView initialization
3. Test keyboard xuất hiện không

---

### Issue 4: Debug mode không work

**Nguyên nhân:**
- Config sai
- Firestore rules block

**Giải pháp:**
1. Verify `DEBUG_BYPASS_OTP = true`
2. Check Logcat: "⚠️ OTP DEBUG MODE ENABLED"
3. Try code "123456"
4. Check Firestore permissions

---

## 📊 Firestore Structure

**Collection:** `otp_codes`

**Document ID:** `userId`

**Fields:**
```json
{
  "userId": "abc123",
  "otp": "472891",
  "email": "user@example.com",
  "createdAt": Timestamp(2025-12-11 10:30:00),
  "expiresAt": Timestamp(2025-12-11 10:35:00),
  "verified": false,
  "attempts": 0,
  "maxAttempts": 3,
  "lastAttemptAt": null
}
```

**Security Rules:**
```javascript
// Allow users to read/write their own OTP codes
match /otp_codes/{userId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
```

---

## 🎨 UI Screenshots (Text Mockup)

### OTP BottomSheet

```
┌─────────────────────────────────┐
│         ──  (handle bar)        │
│                                 │
│   🔐 Xác thực hai bước          │
│                                 │
│   Mã OTP đã được gửi đến        │
│   m***y@gmail.com               │
│                                 │
│   ┌───┐ ┌───┐ ┌───┐            │
│   │ 4 │ │ 7 │ │ 2 │            │
│   └───┘ └───┘ └───┘            │
│   ┌───┐ ┌───┐ ┌───┐            │
│   │ 8 │ │ 9 │ │ 1 │            │
│   └───┘ └───┘ └───┘            │
│                                 │
│   ⏱️ Gửi lại sau 00:45          │
│                                 │
│   ┌─────────────────────────┐  │
│   │      Xác nhận           │  │
│   └─────────────────────────┘  │
│                                 │
│   Không nhận được mã?           │
│   Gửi lại                       │
│                                 │
│   ⚠️ DEBUG MODE: Any code works│
└─────────────────────────────────┘
```

### Error State

```
┌─────────────────────────────────┐
│   🔐 Xác thực hai bước          │
│                                 │
│   ┌───┐ ┌───┐ ┌───┐            │
│   │ 1 │ │ 2 │ │ 3 │            │← Shake animation
│   └───┘ └───┘ └───┘            │
│   ┌───┐ ┌───┐ ┌───┐            │
│   │ 4 │ │ 5 │ │ 6 │            │
│   └───┘ └───┘ └───┘            │
│                                 │
│   ❌ Mã OTP không đúng.         │
│      Còn 2 lần thử.             │← Error message
│                                 │
└─────────────────────────────────┘
```

---

## 🔒 Security Considerations

### Rate Limiting
- Max 3 wrong attempts
- After 3 fails → OTP locked
- Must resend new OTP

### Expiry
- OTP expires after 5 minutes
- Cannot reuse expired OTP
- Auto-cleanup in Firestore

### One-Time Use
- OTP marked as `verified: true` after success
- Cannot reuse same OTP twice
- Invalidated after successful login

### Resend Protection
- 60-second cooldown between resends
- Prevents OTP spam
- Timer shows remaining time

---

## 📈 Analytics & Monitoring

### Logs to Monitor

**Success:**
```
✅ OTP created for user abc123: 472891 (expires in 5 min)
✅ Email sent successfully to user@example.com
✅ OTP verified successfully for user abc123
```

**Errors:**
```
❌ Failed to send email: [reason]
❌ Wrong OTP for user abc123. Attempts: 2/3
❌ OTP expired for user abc123
❌ OTP locked for user abc123 (too many attempts)
```

**Debug:**
```
⚠️ OTP DEBUG MODE ENABLED - Any 6-digit code will be accepted!
⚠️ DEBUG MODE: OTP=472891 OR use 123456
⚠️ DEBUG MODE: Accepting OTP=111111 (bypass enabled)
```

---

## 🎯 Next Steps (Optional Enhancements)

### Future Features
1. **SMS OTP** - Alternative to email
2. **TOTP (Google Authenticator)** - Time-based OTP
3. **Backup codes** - Emergency access
4. **Remember device** - Skip OTP on trusted devices
5. **Biometric + OTP** - Combined authentication
6. **Settings UI** - Enable/disable 2FA per user

### Performance Optimizations
1. Cache OTP in memory (reduce Firestore reads)
2. Batch Firestore writes
3. Optimize email templates
4. Add retry mechanism

---

## 🎓 For Presentation (Học tập)

### Demo Script

**1. Login Flow (2 phút)**
```
"Tôi đã triển khai Two-Factor Authentication cho app.
Khi user login, sau khi verify email/password thành công,
hệ thống sẽ generate một mã OTP 6 số và gửi qua email."
```

**2. Show OTP BottomSheet (3 phút)**
```
"Đây là giao diện nhập OTP với 6 ô input đẹp mắt.
Có countdown timer 60 giây, nút gửi lại, và xử lý lỗi đầy đủ.
Khi nhập sai, có shake animation. Khi đúng, có success animation."
```

**3. Security Features (2 phút)**
```
"Về bảo mật:
- OTP chỉ có hiệu lực 5 phút
- Tối đa 3 lần nhập sai
- Mỗi OTP chỉ dùng được 1 lần
- Có resend cooldown 60 giây để chống spam"
```

**4. Debug Mode (1 phút)**
```
"Để testing dễ dàng, tôi có implement debug mode.
Khi bật, bất kỳ mã 6 số nào cũng được chấp nhận.
Điều này giúp test nhanh mà không cần check email."
```

**5. Architecture (2 phút)**
```
"Kiến trúc gồm:
- OTPManager: Core logic generate và verify OTP
- EmailService: Gửi email qua EmailJS (no backend needed)
- OTPViewModel: State management
- OTPBottomSheetFragment: Beautiful UI với animations
- Firestore: Store OTP codes với expiry"
```

---

## 📚 References

- [EmailJS Documentation](https://www.emailjs.com/docs/)
- [Firebase Firestore Best Practices](https://firebase.google.com/docs/firestore/best-practices)
- [Android BottomSheet Design](https://material.io/components/sheets-bottom)
- [OWASP 2FA Guidelines](https://owasp.org/www-community/controls/Blocking_Brute_Force_Attacks)

---

## ✅ Checklist Before Production

- [ ] Set `DEBUG_BYPASS_OTP = false`
- [ ] Configure EmailJS credentials
- [ ] Test email delivery
- [ ] Setup Firestore security rules
- [ ] Test all error cases
- [ ] Monitor logs in production
- [ ] Setup crash reporting (Firebase Crashlytics)
- [ ] Document for team

---

**Tác giả:** Claude Code
**Ngày:** 2025-12-11
**Project:** Home Harmony - Furniture E-Commerce App
**Version:** 1.0.0
