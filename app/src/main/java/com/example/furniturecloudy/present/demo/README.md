# Performance Demo Package

## 📦 Package: `com.example.furniturecloudy.present.demo`

This package contains **interactive performance demonstration tools** for optimization techniques.

---

## 📁 Files

### **PerformanceDemoActivity.kt**
Main activity for B4: Map vs List performance demo.

**Features:**
- Toggle between BEFORE (List) and AFTER (Map) implementations
- Load fake cart data (100/1000/5000 products)
- Real-time performance tracking
- Interactive UI with stats panel

**Access:**
- Via app: Settings → Tài Khoản → "⚡ Performance Demo"
- Via ADB: `adb shell am start -n com.example.furniturecloudy/.present.demo.PerformanceDemoActivity`

---

### **CartDemoAdapter.kt**
RecyclerView adapter demonstrating List vs Map performance.

**Features:**
- BEFORE mode: Uses `List.indexOf()` - O(n) lookup
- AFTER mode: Uses `Map[key]` - O(1) lookup
- Real-time timer for each operation
- Measures and displays performance metrics

---

## 🎯 Purpose

Created for **thesis demonstration** to show:
- Algorithm complexity differences (O(n) vs O(1))
- Real-world impact on user experience
- Interactive comparison before optimization committee

---

## 📚 Related Documentation

See `docs/optimization/B4/` for:
- `B4_MAP_VS_LIST.md` - Technical overview
- `B4_DEMO_GUIDE.md` - Presentation guide
- `B4_RESULTS.md` - Benchmark results (if created)

---

## 🚀 Usage

1. Set `isDebugMode = true` in `UserAccountFragment.kt`
2. Build and run app
3. Navigate to Settings → Tài Khoản
4. Tap "⚡ Performance Demo - B4 (Map vs List)"

**Or via ADB:**
```bash
adb shell am start -n com.example.furniturecloudy/.present.demo.PerformanceDemoActivity
```

---

**Note:** This is for development/demonstration only. Not included in production builds.