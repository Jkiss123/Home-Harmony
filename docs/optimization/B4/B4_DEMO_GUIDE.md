# B4: PERFORMANCE DEMO - HƯỚNG DẪN SỬ DỤNG

## 🎯 MỤC ĐÍCH

Interactive Performance Demo cho kỹ thuật B4: Map vs List optimization.
Demo trực tiếp trên app với UI cart thật, cho phép hội đồng tự test và cảm nhận sự khác biệt.

---

## 📱 CÁCH MỞ PERFORMANCE DEMO

### **Cách 1: Từ App (DEBUG MODE)**

1. Mở file `UserAccountFragment.kt`
2. Tìm dòng: `val isDebugMode = false` (line ~125)
3. Đổi thành: `val isDebugMode = true`
4. Build và run app
5. Vào **Settings** → **Tài Khoản**
6. Scroll xuống → Bấm **"⚡ Performance Demo - B4 (Map vs List)"**

### **Cách 2: Qua ADB (Không cần thay đổi code)**

```bash
adb shell am start -n com.example.furniturecloudy/.present.PerformanceDemoActivity
```

---

## 🎬 HƯỚNG DẪN DEMO KHI TRÌNH BÀY

### **Bước 1: Giới Thiệu Problem**

**Nói:**
> "Em sẽ demo kỹ thuật tối ưu B4: thay List indexOf() bằng Map lookup.
>
> Khi user tăng/giảm số lượng sản phẩm trong giỏ hàng, app phải tìm product trong danh sách.
>
> **BEFORE**: Dùng List → phải indexOf() từ đầu đến cuối → O(n)
> **AFTER**: Dùng Map → lookup trực tiếp bằng key → O(1)"

**Làm:**
- Mở Performance Demo screen
- Giải thích UI:
  - Top bar: Toggle BEFORE/AFTER
  - Cart size buttons: 100, 1000, 5000 products
  - Stats panel: Track performance real-time

---

### **Bước 2: Demo BEFORE (List - Slow)**

**Làm:**
1. Bấm **"❌ BEFORE (List)"** (màu đỏ)
2. Chọn cart size: **1,000 products**
3. Scroll qua danh sách để show có nhiều items
4. Bấm nút **+** hoặc **-** trên vài products
5. Quan sát timer bên cạnh mỗi item: ~3-6ms

**Nói:**
> "Với BEFORE, mỗi lần click + hoặc - mất khoảng 5ms.
>
> Với 1000 sản phẩm, indexOf() phải duyệt từ đầu đến cuối.
>
> Xem stats panel: Average time ~5ms, có thể cảm nhận được delay nhẹ."

**Point out:**
- Stats Panel hiển thị:
  - Average Time: ~5.000ms
  - Total Operations: (số lần click)
  - Last Op: ~5.000ms

---

### **Bước 3: Demo AFTER (Map - Fast)**

**Làm:**
1. Bấm **"✅ AFTER (Map)"** (màu xanh)
2. Giữ nguyên cart size: **1,000 products**
3. Bấm nút **+** hoặc **-** trên cùng các products như trước
4. Quan sát timer: ~0.007ms - nhanh gấp 700x!

**Nói:**
> "Với AFTER, cùng 1000 sản phẩm nhưng mỗi operation chỉ mất ~0.007ms.
>
> Map lookup O(1) - constant time, không phụ thuộc số lượng.
>
> Improvement: 700-800x nhanh hơn!"

**Point out:**
- Stats Panel hiển thị:
  - Average Time: ~0.007ms
  - Improvement rõ ràng so với BEFORE

---

### **Bước 4: Stress Test với 5,000 Products**

**Làm:**
1. Chọn BEFORE → **5,000 products**
2. Click + một vài lần → ~10-15ms
3. Switch to AFTER → **5,000 products**
4. Click + trên cùng items → vẫn ~0.007ms

**Nói:**
> "Để thấy rõ hơn, test với 5,000 sản phẩm:
>
> **BEFORE**: Lên đến 15ms - lag rõ rệt
> **AFTER**: Vẫn ~0.007ms - không đổi!
>
> Đây là lý do Map scalable hơn List rất nhiều."

---

### **Bước 5: So Sánh Kết Quả**

**Show slide hoặc note:**

| Cart Size | BEFORE (List) | AFTER (Map) | Improvement |
|-----------|--------------|-------------|-------------|
| 100 items | 1ms | 0.026ms | **38x** faster |
| 1,000 items | 5ms | 0.007ms | **714x** faster |
| 5,000 items | 15ms | 0.007ms | **2,143x** faster |

**Nói:**
> "Như đã demo, với 1000 sản phẩm improvement là 714x.
>
> Trong real app, user có thể có 50-100 items trong cart.
>
> Nhưng optimization này đảm bảo app smooth ngay cả khi scale lên."

---

## 💡 CÂU HỎI HỘI ĐỒNG CÓ THỂ HỎI

### **Q1: "Tại sao không test với Firebase thật?"**

**Trả lời:**
> "Em test performance của **thuật toán** (O(n) vs O(1)), không phải Firebase.
>
> In-memory test cho kết quả chính xác hơn vì loại bỏ network latency.
>
> Trong production, em đã áp dụng Map optimization trong CartViewModel.kt thật."

**Show code:**
```kotlin
// CartViewModel.kt - Line 34
private val cartProductsMap = mutableMapOf<String, DocumentSnapshot>()
```

---

### **Q2: "Memory usage tăng bao nhiêu?"**

**Trả lời:**
> "Với 100 products:
> - BEFORE: 20KB (chỉ List)
> - AFTER: 35KB (List + Map) → +75%
>
> Trade-off: +15KB memory để đổi lấy 714x speed → Rất đáng!"

---

### **Q3: "Có test trên thiết bị thật không?"**

**Trả lời:**
> "Có ạ, đây chính là app running trên thiết bị.
>
> Thầy/Cô có thể tự test ngay bây giờ - bấm + hoặc - và xem timer."

*Đưa điện thoại cho hội đồng tự test*

---

## 🎓 TALKING POINTS CHO SLIDE TRÌNH BÀY

### **Slide 1: Problem Statement**
```
🔴 BEFORE: List với indexOf() - O(n)

fun ChangeQuantity(product: CartProducts) {
    val index = products.indexOf(product)  // ❌ O(n)
    // Must iterate through entire list
}

→ Với 1000 items: ~5ms mỗi lần click
→ User cảm nhận được lag
```

### **Slide 2: Solution**
```
🟢 AFTER: Map với key lookup - O(1)

private val cartProductsMap = mutableMapOf<String, DocumentSnapshot>()

fun ChangeQuantity(product: CartProducts) {
    val doc = cartProductsMap[product.id]  // ✅ O(1)
    // Direct hash lookup
}

→ Với 1000 items: ~0.007ms
→ 714x nhanh hơn!
```

### **Slide 3: Demo Results**
```
📊 PERFORMANCE COMPARISON

Test Environment: Android (Real device)
Test Method: Interactive UI demo

| Cart Size | BEFORE | AFTER  | Improvement |
|-----------|--------|--------|-------------|
| 100       | 1ms    | 0.03ms | 38x         |
| 1,000     | 5ms    | 0.01ms | 714x        |
| 5,000     | 15ms   | 0.01ms | 2,143x      |

✅ Constant time regardless of cart size
```

### **Slide 4: Trade-offs**
```
⚖️ TRADE-OFFS ANALYSIS

Memory:
- +75% (+15KB for 100 items)
- Acceptable trade-off for massive speed gain

Code Complexity:
- Slightly more complex
- Must maintain Map alongside List
- Clear documentation helps

Verdict: ✅ Worth it!
```

---

## 📸 SCREENSHOTS CHO LUẬN VĂN

### **Cần chụp:**

1. **Screen 1**: Top bar với BEFORE button selected (màu đỏ)
2. **Screen 2**: Cart list với timer hiển thị ~5ms
3. **Screen 3**: Stats panel showing "Average: 5.000ms"
4. **Screen 4**: Top bar với AFTER button selected (màu xanh)
5. **Screen 5**: Cart list với timer hiển thị ~0.007ms
6. **Screen 6**: Stats panel showing "Average: 0.007ms"

### **Cách chụp:**
```bash
# Chụp screen trên emulator/device
adb exec-out screencap -p > screenshot.png
```

Hoặc dùng Android Studio: **Logcat → Camera icon**

---

## ⚙️ BUILD & RUN

### **Build Debug APK:**
```bash
./gradlew assembleDebug
```

### **Install on device:**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### **Open via ADB:**
```bash
adb shell am start -n com.example.furniturecloudy/.present.PerformanceDemoActivity
```

---

## 🔧 TROUBLESHOOTING

### **Lỗi: Button không hiển thị**
→ Đổi `isDebugMode = true` trong `UserAccountFragment.kt`

### **Lỗi: Activity not found**
→ Check `AndroidManifest.xml` có activity `PerformanceDemoActivity`

### **Lỗi: Build failed**
→ Run `./gradlew clean assembleDebug`

---

## ✅ CHECKLIST KHI DEMO

- [ ] App đã build và install trên device
- [ ] isDebugMode = true (nếu demo qua app)
- [ ] Đã practice demo flow 2-3 lần
- [ ] Đã chụp screenshots cho luận văn
- [ ] Slides đã có code comparison
- [ ] Đã chuẩn bị trả lời Q&A

---

## 🎯 KẾT LUẬN

Performance Demo này cho phép:
✅ **Interactive demonstration** - Hội đồng tự test
✅ **Real-time metrics** - Số liệu trực quan
✅ **Visual comparison** - Thấy rõ BEFORE vs AFTER
✅ **Professional** - Giống production benchmark tool
✅ **Impressive** - Demo sống động hơn slides

**Good luck với presentation! 🚀**

---

**Files liên quan:**
- `PerformanceDemoActivity.kt` - Main logic
- `CartDemoAdapter.kt` - BEFORE/AFTER implementation
- `activity_performance_demo.xml` - UI layout
- `item_cart_demo.xml` - Cart item layout
- `CartViewModel.kt` - Production code đã optimize
- `B4_RESULTS.md` - Benchmark results