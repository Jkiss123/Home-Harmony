# 🚀 2FA Quick Start Guide

## TL;DR - Test ngay trong 2 phút!

### Bước 1: Enable Debug Mode
```kotlin
// File: util/OTPConfig.kt
const val DEBUG_BYPASS_OTP = true  // ✅ Set to true
```

### Bước 2: Build & Run
```bash
./gradlew installDebug
# Or click Run in Android Studio
```

### Bước 3: Test Login
1. Mở app
2. Login với email/password
3. OTP BottomSheet hiện ra 🎉
4. Nhập **BẤT KỲ** 6 số nào (VD: 111111)
5. ✅ Success → Vào app!

**🎉 Xong! Đơn giản vậy thôi!**

---

## Debug Mode Features

Khi `DEBUG_BYPASS_OTP = true`:
- ✅ Nhập bất kỳ mã 6 số nào đều OK
- ✅ Hoặc dùng mã debug: `123456`
- ✅ UI hiển thị warning: "⚠️ DEBUG MODE"
- ✅ Logs show real OTP code (nếu muốn test email)

---

## Production Setup (Khi cần thật)

### 1. Register EmailJS (Free - 5 phút)
1. Go to: https://www.emailjs.com/
2. Sign up (free account)
3. Add email service (Gmail/Outlook)
4. Create template:

```
Subject: Your Home Harmony OTP Code

Body:
Hello {{user_name}},

Your OTP code is: {{otp_code}}

Expires in {{expiry_minutes}} minutes.

Best regards,
Home Harmony Team
```

5. Copy credentials:
   - Service ID: `service_xxxxx`
   - Template ID: `template_xxxxx`
   - User ID: `user_xxxxxxxxxxxxx`

### 2. Update Config
```kotlin
// File: util/OTPConfig.kt

const val DEBUG_BYPASS_OTP = false  // ⚠️ Set to false

const val EMAILJS_SERVICE_ID = "service_xxxxx"      // Paste here
const val EMAILJS_TEMPLATE_ID = "template_xxxxx"    // Paste here
const val EMAILJS_USER_ID = "user_xxxxxxxxxxxxx"    // Paste here
```

### 3. Test Real Email
1. Build & run
2. Login
3. Check email → Nhận OTP
4. Nhập OTP → Success! ✅

---

## Configuration Options

### OTP Settings
```kotlin
// File: util/OTPConfig.kt

const val OTP_LENGTH = 6                  // 6 digits
const val OTP_EXPIRY_MINUTES = 5          // 5 minutes
const val OTP_MAX_ATTEMPTS = 3            // 3 attempts
const val RESEND_COOLDOWN_SECONDS = 60    // 60 seconds
```

### Force 2FA for all users
```kotlin
const val TWO_FACTOR_ENABLED_BY_DEFAULT = true  // Mandatory
// Set to false if you want optional 2FA
```

---

## Troubleshooting

### Q: BottomSheet không hiện?
**A:** Check Logcat cho errors. Verify `TWO_FACTOR_ENABLED_BY_DEFAULT = true`

### Q: Email không nhận được?
**A:** Enable DEBUG mode để test không cần email:
```kotlin
const val DEBUG_BYPASS_OTP = true
```

### Q: Làm sao biết OTP thật nếu chưa setup email?
**A:** Check Logcat:
```
D/OTPManager: ⚠️ DEBUG MODE: OTP=472891 OR use 123456
D/OTPBottomSheet: ⚠️ DEBUG MODE - Real OTP: 472891
```

### Q: Build error?
**A:** Sync Gradle:
```bash
./gradlew clean
./gradlew build
```

---

## Files Modified/Created

**Modified (2 files):**
- `data/User.kt` - Added 2FA fields
- `present/fragments/loginRegister/LoginFragment.kt` - OTP flow

**Created (15 files):**
- ✅ `util/OTPConfig.kt`
- ✅ `util/OTPManager.kt`
- ✅ `util/EmailService.kt`
- ✅ `data/OTPCode.kt`
- ✅ `model/viewmodel/OTPViewModel.kt`
- ✅ `view/OTPInputView.kt`
- ✅ `present/fragments/loginRegister/OTPBottomSheetFragment.kt`
- ✅ `res/layout/view_otp_input.xml`
- ✅ `res/layout/bottom_sheet_otp_verification.xml`
- ✅ `res/drawable/bg_otp_box.xml`
- ✅ `res/drawable/bg_bottom_sheet.xml`
- ✅ `res/drawable/bg_bottom_sheet_handle.xml`
- ✅ `res/anim/shake_animation.xml`
- ✅ `docs/TWO_FACTOR_AUTHENTICATION.md`
- ✅ `docs/2FA_QUICK_START.md`

---

## Test Checklist

- [ ] Login với DEBUG mode → Any 6-digit works
- [ ] OTP BottomSheet hiển thị đẹp
- [ ] Countdown timer hoạt động
- [ ] Nhập sai OTP → Shake animation
- [ ] Nhập đúng OTP → Navigate to Shopping
- [ ] Paste OTP (copy 123456 → paste) → Auto fill
- [ ] Resend OTP button (wait 60s)

---

## Next Steps

1. ✅ Test với DEBUG mode
2. ⏭️ Setup EmailJS (optional)
3. ⏭️ Test real email flow
4. ⏭️ Customize UI/colors
5. ⏭️ Setup Firestore security rules

---

## Support

**Full Documentation:** `docs/TWO_FACTOR_AUTHENTICATION.md`

**Security Summary:** `docs/SECURITY_SUMMARY.md`

**Questions?** Check logs in Logcat (filter: "OTP")

---

**Author:** Claude Code
**Date:** 2025-12-11
**Version:** 1.0.0
