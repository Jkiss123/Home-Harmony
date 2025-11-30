# Hướng dẫn Bảo mật và Mã hóa - Home Harmony App

## Mục lục
1. [Tổng quan kiến trúc bảo mật](#1-tổng-quan-kiến-trúc-bảo-mật)
2. [Xác thực người dùng (Authentication)](#2-xác-thực-người-dùng-authentication)
3. [Mã hóa dữ liệu địa chỉ (Address Encryption)](#3-mã-hóa-dữ-liệu-địa-chỉ-address-encryption)
4. [Mã hóa PIN Code](#4-mã-hóa-pin-code)
5. [Network Security](#5-network-security)
6. [Giải thích thuật toán AES-256-GCM](#6-giải-thích-thuật-toán-aes-256-gcm)
7. [Android Keystore](#7-android-keystore)
8. [Câu hỏi thường gặp](#8-câu-hỏi-thường-gặp)

---

## 1. Tổng quan kiến trúc bảo mật

### Sơ đồ tổng quan

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         HOME HARMONY APP                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │
│  │  Authentication │  │   Encryption    │  │    Network      │          │
│  │     Layer       │  │     Layer       │  │    Security     │          │
│  ├─────────────────┤  ├─────────────────┤  ├─────────────────┤          │
│  │ • Biometric     │  │ • AES-256-GCM   │  │ • HTTPS Only    │          │
│  │ • Device PIN    │  │ • Android       │  │ • Certificate   │          │
│  │ • App PIN       │  │   Keystore      │  │   Validation    │          │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘          │
│           │                   │                    │                     │
│           └───────────────────┼────────────────────┘                     │
│                               │                                          │
│                               ▼                                          │
│                    ┌─────────────────────┐                               │
│                    │   Firebase Cloud    │                               │
│                    │   (Firestore)       │                               │
│                    └─────────────────────┘                               │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Các lớp bảo mật đã triển khai

| Lớp | Mục đích | Công nghệ |
|-----|----------|-----------|
| **Authentication** | Xác thực người dùng khi mở app | Biometric API, Android Keystore |
| **Data Encryption** | Mã hóa dữ liệu nhạy cảm | AES-256-GCM |
| **Network Security** | Bảo vệ dữ liệu truyền tải | HTTPS, TLS |

---

## 2. Xác thực người dùng (Authentication)

### 2.1 Sơ đồ luồng xác thực

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         MỞ ỨNG DỤNG                                      │
└─────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │  Kiểm tra Auth Enabled │
                    │  (AppAuthManager)      │
                    └───────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    │                       │
                    ▼                       ▼
            ┌───────────┐           ┌───────────────┐
            │ Auth OFF  │           │   Auth ON     │
            └───────────┘           └───────────────┘
                    │                       │
                    ▼                       ▼
            ┌───────────┐       ┌───────────────────────┐
            │ Vào app   │       │ Lấy phương thức đã    │
            │ trực tiếp │       │ chọn từ Settings      │
            └───────────┘       └───────────────────────┘
                                            │
                        ┌───────────────────┼───────────────────┐
                        │                   │                   │
                        ▼                   ▼                   ▼
                ┌───────────────┐   ┌───────────────┐   ┌───────────────┐
                │   BIOMETRIC   │   │ DEVICE_CRED   │   │   APP_PIN     │
                │  Vân tay/     │   │  PIN thiết bị │   │  PIN ứng dụng │
                │  Khuôn mặt    │   │               │   │               │
                └───────────────┘   └───────────────┘   └───────────────┘
                        │                   │                   │
                        └───────────────────┼───────────────────┘
                                            │
                                            ▼
                                    ┌───────────────┐
                                    │   Xác thực    │
                                    │   thành công? │
                                    └───────────────┘
                                            │
                                ┌───────────┴───────────┐
                                │                       │
                                ▼                       ▼
                        ┌───────────────┐       ┌───────────────┐
                        │   Thành công  │       │   Thất bại    │
                        │   → Vào app   │       │   → Thử lại/  │
                        │               │       │     Đăng xuất │
                        └───────────────┘       └───────────────┘
```

### 2.2 Các file liên quan

| File | Chức năng |
|------|-----------|
| `AppAuthManager.kt` | Quản lý settings xác thực (bật/tắt, phương thức) |
| `BiometricHelper.kt` | Xử lý xác thực sinh trắc học |
| `PinCodeManager.kt` | Quản lý PIN ứng dụng (mã hóa, verify) |
| `PinCodeDialog.kt` | UI nhập PIN |
| `IntroductionFragment.kt` | Điều phối luồng xác thực khi mở app |
| `ProfileFragment.kt` | UI Settings để cấu hình xác thực |

### 2.3 Code flow xác thực

```kotlin
// IntroductionFragment.kt - Entry point

private fun handleAuthenticationFlow() {
    // Bước 1: Kiểm tra xác thực có được bật không
    if (!appAuthManager.isAuthEnabled()) {
        navigateToShopping() // Vào app trực tiếp
        return
    }

    // Bước 2: Xác thực theo phương thức đã chọn
    when (appAuthManager.getAuthMethod()) {
        AuthMethod.BIOMETRIC -> authenticateWithBiometric()
        AuthMethod.DEVICE_CREDENTIAL -> authenticateWithDeviceCredential()
        AuthMethod.APP_PIN -> authenticateWithAppPin()
    }
}
```

---

## 3. Mã hóa dữ liệu địa chỉ (Address Encryption)

### 3.1 Tại sao cần mã hóa địa chỉ?

Địa chỉ giao hàng chứa **thông tin cá nhân nhạy cảm (PII - Personally Identifiable Information)**:
- Số điện thoại
- Địa chỉ nhà đầy đủ

Nếu database bị xâm nhập, attacker có thể:
- Thu thập số điện thoại để spam/lừa đảo
- Biết địa chỉ nhà để đột nhập/theo dõi

### 3.2 Sơ đồ luồng mã hóa

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         LƯU ĐỊA CHỈ MỚI                                  │
└─────────────────────────────────────────────────────────────────────────┘

   User nhập dữ liệu                    Lưu vào Firestore
   ┌─────────────────┐                  ┌─────────────────────────────────┐
   │ fullName: "Nguyễn Văn A"           │ fullName: "Nguyễn Văn A"        │
   │ phone: "0912345678"      ────►     │ phone: "ENC:dGVz:YWJj..."       │
   │ addressFull: "123 ABC"             │ addressFull: "ENC:eHl6:bW5v..." │
   │ city: "HCM"                        │ city: "HCM"                     │
   └─────────────────┘                  └─────────────────────────────────┘
          │                                        ▲
          │                                        │
          │         ┌──────────────────┐           │
          └────────►│ encryptAddress() │───────────┘
                    │                  │
                    │  • phone         │
                    │  • addressFull   │
                    │                  │
                    │  Được mã hóa     │
                    │  bằng AES-256    │
                    └──────────────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                         ĐỌC ĐỊA CHỈ                                      │
└─────────────────────────────────────────────────────────────────────────┘

   Dữ liệu trên Firestore               Hiển thị cho User
   ┌─────────────────────────────────┐  ┌─────────────────┐
   │ fullName: "Nguyễn Văn A"        │  │ fullName: "Nguyễn Văn A"
   │ phone: "ENC:dGVz:YWJj..."       │  │ phone: "0912345678"
   │ addressFull: "ENC:eHl6:bW5v..." │  │ addressFull: "123 ABC"
   │ city: "HCM"                     │  │ city: "HCM"
   └─────────────────────────────────┘  └─────────────────┘
          │                                        ▲
          │                                        │
          │        ┌───────────────────┐           │
          └───────►│ decryptAddress()  │───────────┘
                   │                   │
                   │  Giải mã các      │
                   │  field có prefix  │
                   │  "ENC:"           │
                   └───────────────────┘
```

### 3.3 Cấu trúc dữ liệu mã hóa

```
Format: "ENC:<IV_base64>:<Ciphertext_base64>"

Ví dụ:
  Input:  "0912345678"
  Output: "ENC:dGVzdGl2MTIzNDU2:YWJjZGVmZ2hpamtsbW5vcA=="
          │    │                 │
          │    │                 └── Ciphertext (dữ liệu đã mã hóa)
          │    └── IV (Initialization Vector)
          └── Prefix nhận dạng dữ liệu đã mã hóa
```

### 3.4 Code chi tiết

```kotlin
// AddressEncryptionHelper.kt

class AddressEncryptionHelper(context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "HomeHarmonyAddressKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        const val ENCRYPTED_PREFIX = "ENC:"
    }

    /**
     * Mã hóa Address trước khi lưu
     */
    fun encryptAddress(address: Address): Address {
        return address.copy(
            phone = encryptField(address.phone),
            addressFull = encryptField(address.addressFull)
        )
    }

    /**
     * Giải mã Address sau khi đọc
     */
    fun decryptAddress(address: Address): Address {
        return address.copy(
            phone = decryptField(address.phone),
            addressFull = decryptField(address.addressFull)
        )
    }

    /**
     * Mã hóa một field
     */
    private fun encryptField(plainText: String): String {
        // Không mã hóa nếu rỗng hoặc đã mã hóa
        if (plainText.isEmpty()) return plainText
        if (plainText.startsWith(ENCRYPTED_PREFIX)) return plainText

        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv  // IV tự động sinh random
        val cipherText = cipher.doFinal(plainText.toByteArray())

        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)

        return "$ENCRYPTED_PREFIX$ivBase64:$cipherBase64"
    }

    /**
     * Giải mã một field
     */
    private fun decryptField(encryptedText: String): String {
        // FALLBACK: Nếu không có prefix → data cũ, trả về nguyên bản
        if (!encryptedText.startsWith(ENCRYPTED_PREFIX)) return encryptedText

        val data = encryptedText.removePrefix(ENCRYPTED_PREFIX)
        val parts = data.split(":")
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)

        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        return String(cipher.doFinal(cipherText))
    }
}
```

### 3.5 Tích hợp vào ViewModel

```kotlin
// AddressViewmodel.kt - Khi lưu

fun addAddress(address: Address) {
    // ...
    val encryptedAddress = encryptionHelper.encryptAddress(addressWithId)
    docRef.set(encryptedAddress)  // Lưu dữ liệu đã mã hóa
    // ...
}

// BillingViewmodel.kt - Khi đọc

fun getUserAddresses() {
    firestore.collection("address").addSnapshotListener { value, _ ->
        val encryptedAddresses = value?.toObjects(Address::class.java)

        // Giải mã trước khi emit cho UI
        val decryptedAddresses = encryptionHelper.decryptAddresses(encryptedAddresses)
        _address.emit(Resource.Success(decryptedAddresses))
    }
}
```

### 3.6 Backward Compatibility (Tương thích ngược)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    XỬ LÝ DỮ LIỆU CŨ VS MỚI                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Dữ liệu CŨ (chưa mã hóa):                                              │
│  ┌─────────────────────────┐                                            │
│  │ phone: "0912345678"     │ ──► Không có "ENC:" ──► Trả về nguyên bản  │
│  └─────────────────────────┘                                            │
│                                                                          │
│  Dữ liệu MỚI (đã mã hóa):                                               │
│  ┌─────────────────────────┐                                            │
│  │ phone: "ENC:abc:xyz..." │ ──► Có "ENC:" ──► Giải mã ──► "0912345678" │
│  └─────────────────────────┘                                            │
│                                                                          │
│  ✅ App hoạt động bình thường với cả 2 loại dữ liệu                     │
│  ✅ Dữ liệu cũ tự động được mã hóa khi user cập nhật                    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Mã hóa PIN Code

### 4.1 Luồng thiết lập PIN

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         THIẾT LẬP PIN                                    │
└─────────────────────────────────────────────────────────────────────────┘

   User nhập PIN                 Mã hóa                  Lưu trữ
   ┌─────────┐                ┌─────────┐           ┌─────────────────┐
   │  1234   │ ──────────────►│ AES-256 │──────────►│ SharedPreferences│
   └─────────┘                │   GCM   │           │                 │
                              └─────────┘           │ encrypted_pin:  │
                                   │                │ "YWJjZGVm..."   │
                                   │                │                 │
                                   ▼                │ pin_iv:         │
                          ┌───────────────┐         │ "eHl6MTIz..."   │
                          │ Android       │         │                 │
                          │ Keystore      │         └─────────────────┘
                          │ (Secret Key)  │
                          └───────────────┘
```

### 4.2 Luồng xác thực PIN

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         XÁC THỰC PIN                                     │
└─────────────────────────────────────────────────────────────────────────┘

   User nhập            Giải mã PIN đã lưu           So sánh
   ┌─────────┐         ┌─────────────────┐         ┌─────────┐
   │  1234   │         │ encrypted_pin   │         │ 1234 == │
   └─────────┘         │ + iv            │         │ 1234 ?  │
        │              │      │          │         └─────────┘
        │              │      ▼          │              │
        │              │ ┌─────────┐     │              │
        │              │ │ AES-256 │     │      ┌──────┴──────┐
        │              │ │ Decrypt │     │      │             │
        │              │ └─────────┘     │      ▼             ▼
        │              │      │          │  ┌───────┐    ┌───────┐
        │              │      ▼          │  │ Match │    │ Wrong │
        │              │   "1234"        │  │  ✅   │    │  ❌   │
        │              └─────────────────┘  └───────┘    └───────┘
        │                     │
        └─────────────────────┘
              So sánh
```

### 4.3 Bảo vệ Brute Force

```kotlin
// PinCodeManager.kt

companion object {
    const val MAX_ATTEMPTS = 5          // Tối đa 5 lần thử
    const val LOCKOUT_DURATION = 30_000 // Khóa 30 giây
}

fun verifyPin(pin: String): PinVerificationResult {
    // Kiểm tra đang bị khóa không
    if (isLockedOut()) {
        return PinVerificationResult.LockedOut(remainingTime)
    }

    // Verify PIN
    if (pin == decryptedStoredPin) {
        resetFailedAttempts()
        return PinVerificationResult.Success
    } else {
        incrementFailedAttempts()
        if (getFailedAttempts() >= MAX_ATTEMPTS) {
            startLockout()
            return PinVerificationResult.LockedOut(LOCKOUT_DURATION)
        }
        return PinVerificationResult.WrongPin(remainingAttempts)
    }
}
```

```
Timeline Brute Force Protection:

Lần 1 sai ──► "Còn 4 lần thử"
Lần 2 sai ──► "Còn 3 lần thử"
Lần 3 sai ──► "Còn 2 lần thử"
Lần 4 sai ──► "Còn 1 lần thử"
Lần 5 sai ──► "Tài khoản bị khóa 30 giây"
              │
              ▼
         [Đợi 30 giây]
              │
              ▼
         Reset counter, thử lại từ đầu
```

---

## 5. Network Security

### 5.1 Network Security Config

```xml
<!-- res/xml/network_security_config.xml -->

<network-security-config>
    <!-- Chặn tất cả HTTP, chỉ cho phép HTTPS -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>

    <!-- Debug mode: cho phép proxy tools -->
    <debug-overrides>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </debug-overrides>
</network-security-config>
```

### 5.2 Tác dụng

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    NETWORK SECURITY                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ❌ CHẶN:                                                               │
│     • http://api.example.com  (cleartext)                               │
│     • Downgrade attack từ HTTPS xuống HTTP                              │
│     • Thư viện bên thứ 3 "lén" gọi HTTP                                 │
│                                                                          │
│  ✅ CHO PHÉP:                                                           │
│     • https://firebase.googleapis.com (HTTPS)                           │
│     • https://momo.vn (HTTPS)                                           │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Giải thích thuật toán AES-256-GCM

### 6.1 AES là gì?

**AES (Advanced Encryption Standard)** là thuật toán mã hóa đối xứng được NIST (Mỹ) công nhận năm 2001.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         MÃ HÓA ĐỐI XỨNG                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   Plaintext ──────► [  AES + Key  ] ──────► Ciphertext                  │
│                                                                          │
│   Ciphertext ─────► [  AES + Key  ] ──────► Plaintext                   │
│                                                                          │
│   ✅ Cùng một key để mã hóa và giải mã                                  │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Tại sao chọn AES-256?

| Phiên bản | Key Size | Độ an toàn |
|-----------|----------|------------|
| AES-128 | 128 bits | An toàn |
| AES-192 | 192 bits | Rất an toàn |
| **AES-256** | **256 bits** | **Cực kỳ an toàn** |

- 2^256 tổ hợp key có thể
- Brute force cần ~10^77 năm với máy tính hiện tại

### 6.3 GCM Mode là gì?

**GCM (Galois/Counter Mode)** cung cấp:
1. **Encryption** - Mã hóa dữ liệu
2. **Authentication** - Xác thực dữ liệu không bị sửa đổi

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         AES-GCM ENCRYPTION                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   Input:                                                                 │
│   ┌───────────────┐  ┌───────────────┐  ┌───────────────┐               │
│   │   Plaintext   │  │   Secret Key  │  │      IV       │               │
│   │  "0912345678" │  │   (256 bits)  │  │  (12 bytes)   │               │
│   └───────────────┘  └───────────────┘  └───────────────┘               │
│          │                  │                  │                         │
│          └──────────────────┼──────────────────┘                         │
│                             │                                            │
│                             ▼                                            │
│                    ┌─────────────────┐                                   │
│                    │    AES-GCM      │                                   │
│                    │    Encrypt      │                                   │
│                    └─────────────────┘                                   │
│                             │                                            │
│                             ▼                                            │
│   Output:                                                                │
│   ┌───────────────────────────────────────────────────┐                 │
│   │  Ciphertext + Authentication Tag (128 bits)       │                 │
│   │  "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo="          │                 │
│   └───────────────────────────────────────────────────┘                 │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.4 IV (Initialization Vector)

- **Kích thước**: 12 bytes (96 bits) cho GCM
- **Mục đích**: Đảm bảo cùng plaintext + cùng key → khác ciphertext
- **Yêu cầu**: Phải UNIQUE cho mỗi lần mã hóa

```
Ví dụ:
  Plaintext: "0912345678"
  Key: [same]

  Lần 1: IV = "abc123..." → Ciphertext = "xyz789..."
  Lần 2: IV = "def456..." → Ciphertext = "uvw012..."  (khác!)
```

### 6.5 Authentication Tag

- **Kích thước**: 128 bits
- **Mục đích**: Phát hiện nếu ciphertext bị sửa đổi
- **Cơ chế**: Nếu attacker sửa 1 bit → decrypt thất bại

```
┌─────────────────────────────────────────────────────────────────────────┐
│              AUTHENTICATION TAG - CHỐNG GIẢ MẠO                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ✅ Dữ liệu nguyên vẹn:                                                 │
│     Ciphertext: "abc123xyz"                                             │
│     Tag: "tag789"                                                       │
│     → Decrypt thành công                                                │
│                                                                          │
│  ❌ Dữ liệu bị sửa:                                                     │
│     Ciphertext: "abc123xyz" → "abc999xyz" (bị sửa)                      │
│     Tag: "tag789" (không khớp với ciphertext mới)                       │
│     → Decrypt THẤT BẠI, throw exception                                 │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Android Keystore

### 7.1 Android Keystore là gì?

**Android Keystore** là hệ thống lưu trữ key an toàn của Android:
- Key được lưu trong **hardware security module (HSM)** hoặc **TEE (Trusted Execution Environment)**
- Key **không thể export** ra khỏi thiết bị
- Ngay cả khi thiết bị bị root, key vẫn an toàn

### 7.2 Tại sao dùng Android Keystore?

```
┌─────────────────────────────────────────────────────────────────────────┐
│              SO SÁNH CÁC CÁCH LƯU KEY                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ❌ Hardcode trong code:                                                │
│     val key = "my-secret-key-123"                                       │
│     → Decompile APK → thấy key ngay                                     │
│                                                                          │
│  ❌ Lưu trong SharedPreferences:                                        │
│     prefs.putString("key", "my-secret-key")                             │
│     → Root device → đọc được file XML                                   │
│                                                                          │
│  ❌ Lưu trong file:                                                     │
│     File("secret.key").writeText("my-key")                              │
│     → Root device → đọc được file                                       │
│                                                                          │
│  ✅ Android Keystore:                                                   │
│     KeyStore.getInstance("AndroidKeyStore")                             │
│     → Key lưu trong hardware                                            │
│     → Không thể extract                                                 │
│     → Ngay cả root cũng không lấy được                                  │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 7.3 Code tạo key trong Keystore

```kotlin
private fun getOrCreateSecretKey(): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)

    // Nếu đã có key → trả về
    keyStore.getKey(KEY_ALIAS, null)?.let {
        return it as SecretKey
    }

    // Tạo key mới
    val keyGenerator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        "AndroidKeyStore"
    )

    val keySpec = KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)  // AES-256
        .setRandomizedEncryptionRequired(true)  // Tự động sinh IV random
        .build()

    keyGenerator.init(keySpec)
    return keyGenerator.generateKey()
}
```

---

## 8. Câu hỏi thường gặp

### Q1: Tại sao chỉ mã hóa phone và addressFull?

**A:** Các field này chứa **thông tin cá nhân nhạy cảm (PII)**:
- `phone`: Có thể dùng để spam, lừa đảo
- `addressFull`: Có thể dùng để theo dõi, đột nhập

Các field khác như `city`, `district` là thông tin công khai, không cần mã hóa.

### Q2: Nếu user uninstall app thì key mất, data có giải mã được không?

**A:** Không. Key trong Android Keystore bị xóa khi uninstall app.
- Data trên Firestore vẫn còn nhưng ở dạng mã hóa
- Cần thiết kế **key recovery mechanism** nếu cần (ngoài scope hiện tại)

### Q3: Tại sao dùng GCM mode thay vì CBC?

**A:** GCM có nhiều ưu điểm hơn CBC:

| Tính năng | CBC | GCM |
|-----------|-----|-----|
| Encryption | ✅ | ✅ |
| Authentication | ❌ | ✅ |
| Parallel processing | ❌ | ✅ |
| Padding required | ✅ | ❌ |

### Q4: Data cũ (chưa mã hóa) có bị lỗi không?

**A:** Không. Code có **fallback**:
```kotlin
if (!encryptedText.startsWith("ENC:")) {
    return encryptedText  // Trả về nguyên bản nếu chưa mã hóa
}
```

### Q5: Có thể decrypt data từ thiết bị khác không?

**A:** Không. Key trong Android Keystore:
- Chỉ tồn tại trên thiết bị tạo key
- Không thể export
- Không thể backup

→ Mỗi thiết bị có key riêng, data mã hóa trên thiết bị A không thể giải mã trên thiết bị B.

### Q6: Làm sao để data sync giữa nhiều thiết bị?

**A:** Cần implement **server-side encryption** hoặc **key derivation từ user password**. Hiện tại app dùng **device-specific encryption** nên không sync được.

---

## Tài liệu tham khảo

1. [Android Keystore System](https://developer.android.com/training/articles/keystore)
2. [AES-GCM Encryption](https://en.wikipedia.org/wiki/Galois/Counter_Mode)
3. [Network Security Configuration](https://developer.android.com/training/articles/security-config)
4. [Biometric Authentication](https://developer.android.com/training/sign-in/biometric-auth)

---

**Tác giả:** Home Harmony Development Team
**Phiên bản:** 1.0
**Cập nhật:** 2024