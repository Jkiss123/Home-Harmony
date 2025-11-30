# MoMo Payment Integration Guide

## Tổng quan
Dự án đã được tích hợp MoMo Payment SDK để hỗ trợ thanh toán qua ví MoMo.

## ⚠️ Quan trọng - Giải quyết Build Error

Do JitPack không build được AAR file từ GitHub repository, chúng ta đã **thêm MoMo SDK như một local module** thay vì dependency từ JitPack.

## Các file đã thêm/sửa đổi

### 1. MoMo SDK Module (Local)
- **momo_partner_sdk/**: Module chứa source code của MoMo SDK
  - `build.gradle.kts`: Gradle config cho module
  - `src/`: Source code Java của SDK
  - `consumer-rules.pro`: ProGuard rules

### 2. Dependencies & Configuration
- **settings.gradle.kts**:
  - ~~Đã thêm JitPack repository~~ (không cần nữa)
  - Đã thêm `include(":momo_partner_sdk")`
- **app/build.gradle.kts**: Đã thêm MoMo SDK local module `implementation(project(":momo_partner_sdk"))`
- **AndroidManifest.xml**: Đã thêm INTERNET permission

### 2. Payment Helper Classes
- **MoMoPaymentHelper.kt**: Class chính xử lý thanh toán MoMo
- **MoMoConfig.kt**: Configuration file chứa merchant credentials

### 3. UI Integration
- **BillingFragment.kt**: Đã tích hợp MoMo payment vào flow thanh toán

## Hướng dẫn sử dụng

### Bước 1: Đăng ký Merchant Account
1. Truy cập https://business.momo.vn
2. Đăng ký tài khoản doanh nghiệp
3. Sau khi được duyệt, bạn sẽ nhận được:
   - Merchant Code
   - Merchant Name
   - Access Key
   - Secret Key

### Bước 2: Cập nhật Merchant Credentials
Mở file `MoMoConfig.kt` và cập nhật các thông tin:

```kotlin
object MoMoConfig {
    // Development credentials
    const val MERCHANT_NAME = "Tên cửa hàng của bạn"
    const val MERCHANT_CODE = "Mã merchant của bạn" // Từ business.momo.vn
    const val MERCHANT_NAME_LABEL = "Label ngắn gọn"

    // Production credentials (cho khi release)
    const val PROD_MERCHANT_NAME = "Tên cửa hàng"
    const val PROD_MERCHANT_CODE = "Mã merchant production"

    // Đổi thành false khi release
    const val IS_DEVELOPMENT = true
}
```

### Bước 3: Sync và Build Project

1. Mở Android Studio
2. Click "Sync Project with Gradle Files" (hoặc File > Sync Project with Gradle Files)
3. Đợi sync hoàn tất
4. Build project: Build > Make Project (hoặc Ctrl/Cmd + F9)
5. Nếu có lỗi, xem phần Troubleshooting bên dưới

### Bước 4: Test Payment

#### Test trong Development Mode:
1. Cài đặt app MoMo trên thiết bị test
2. Đăng nhập tài khoản MoMo test (hoặc tài khoản thật)
3. Trong app, chọn sản phẩm và đến màn hình thanh toán
4. Chọn phương thức thanh toán "MoMo"
5. Nhấn "Đặt hàng"
6. App sẽ chuyển sang MoMo app để xác nhận thanh toán
7. Sau khi thanh toán, app sẽ quay lại và hiển thị kết quả

#### Credentials Test (từ MoMo SDK):
```kotlin
const val MERCHANT_CODE = "MOMOC2IC20220510" // Test merchant code
```

## Luồng hoạt động

### 1. Khởi tạo SDK
```kotlin
// Trong onCreate() của BillingFragment
MoMoPaymentHelper.initialize(isDevelopment = true)
moMoPaymentHelper = MoMoConfig.createPaymentHelper()
```

### 2. Request Payment
```kotlin
// Khi user chọn thanh toán MoMo
moMoPaymentHelper.requestPayment(
    activity = requireActivity(),
    amount = amountInVND,  // Số tiền tính bằng VND
    orderId = orderId,
    description = "Mô tả đơn hàng"
)
```

### 3. Handle Payment Result
```kotlin
// Trong onActivityResult()
val result = moMoPaymentHelper.handlePaymentResult(requestCode, resultCode, data)
if (result?.isSuccessful() == true) {
    // Thanh toán thành công
    // Lưu order với paymentStatus = "PAID"
} else {
    // Thanh toán thất bại
    // Hiển thị lỗi và cho phép retry
}
```

## Payment Status

Order có 2 trường liên quan đến payment:
- **paymentMethod**: "COD", "MoMo", "VNPay", "ZaloPay"
- **paymentStatus**: "PENDING", "PAID", "FAILED", "REFUNDED"
- **paymentTransactionId**: Transaction ID từ MoMo (nếu có)

## Xử lý lỗi

### 1. MoMo app chưa cài đặt
- App sẽ hiển thị dialog hỏi user có muốn chuyển sang COD không

### 2. Thanh toán thất bại
- App hiển thị dialog với 3 options:
  - Hủy
  - Thử lại MoMo
  - Chuyển sang COD

### 3. User hủy thanh toán
- Pending order sẽ được clear
- User có thể chọn lại phương thức thanh toán

## Chuyển đổi tiền tệ

Hiện tại app sử dụng USD, nhưng MoMo yêu cầu VND:
```kotlin
// Trong BillingFragment
val amountInVND = (totalPrice * 25000).toLong()
```

**Lưu ý**: Bạn nên cập nhật tỷ giá động hoặc chuyển toàn bộ app sang VND.

## Security Notes

⚠️ **QUAN TRỌNG**:
1. KHÔNG commit merchant credentials thật vào Git
2. Sử dụng environment variables hoặc BuildConfig cho production
3. Validate payment result ở server-side
4. Implement webhook để nhận thông báo từ MoMo server

## Server-side Integration (Khuyến nghị)

Để tăng bảo mật, nên implement server-side:

1. Client request payment token từ server
2. Server tạo payment request với signature
3. Server gọi MoMo API để tạo payment
4. Server trả payment URL về client
5. Client mở MoMo app
6. MoMo gọi webhook của server khi payment complete
7. Server verify và update order status

## Troubleshooting

### 1. Build Error: "Could not resolve all files for configuration"
**Đã fix**: Sử dụng local module thay vì JitPack dependency.

Nếu vẫn gặp lỗi:
- Đảm bảo `momo_partner_sdk/` folder tồn tại trong project root
- Kiểm tra `settings.gradle.kts` có `include(":momo_partner_sdk")`
- Sync Gradle lại
- Invalidate Caches and Restart (File > Invalidate Caches...)

### 1a. Plugin Error: "Plugin with id 'com.github.dcendents.android-maven' not found"
**Đã fix**: Đã xóa file `build.gradle` cũ và chỉ giữ lại `build.gradle.kts`

Nếu vẫn gặp:
- Kiểm tra trong `momo_partner_sdk/` chỉ có `build.gradle.kts`, không có `build.gradle`
- Xóa `.gradle` cache: `rm -rf .gradle/` ở project root
- Sync lại

### 2. Sync Error: "Module not found"
- Clean project: Build > Clean Project
- Rebuild project: Build > Rebuild Project
- Restart Android Studio

### 3. MoMo app không mở
- Kiểm tra xem MoMo app đã cài đặt chưa
- Kiểm tra merchant code có đúng không

### 4. Payment failed ngay lập tức
- Kiểm tra INTERNET permission trong manifest
- Kiểm tra merchant code và credentials
- Xem logcat để debug: `adb logcat | grep MoMo`

### 5. App crash khi payment
- Kiểm tra đã thêm module đúng chưa
- Sync Gradle lại
- Clean và rebuild project

### 6. Compilation Error: "Cannot find symbol class AppMoMoLib"
- Đảm bảo module `momo_partner_sdk` đã được include
- Check `app/build.gradle.kts` có `implementation(project(":momo_partner_sdk"))`
- Sync và rebuild

## References
- MoMo SDK GitHub: https://github.com/momo-wallet/mobile-sdk
- MoMo Business Portal: https://business.momo.vn
- MoMo Developer Docs: https://developers.momo.vn

## Support
Nếu có vấn đề với integration, hãy:
1. Kiểm tra logs trong logcat
2. Xem MoMoPaymentResult.errorCode và message
3. Liên hệ MoMo support qua business.momo.vn
