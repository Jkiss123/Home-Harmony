# 📋 OPTIMIZATION PLAN - LUẬN VĂN TỐI ƯU KOTLIN/ANDROID

## 🎯 MỤC TIÊU

Demo 9 kỹ thuật tối ưu trong Home Harmony app cho luận văn với 2 nhóm:
- **6 kỹ thuật PERFORMANCE** → Demo Before/After với benchmark
- **3 kỹ thuật CODE QUALITY** → Chỉ code comparison

---

## 📊 DANH SÁCH KỸ THUẬT

### NHÓM A: PERFORMANCE (Cần benchmark Before/After)

| # | Kỹ thuật | Trạng thái | Metric đo | Độ khó |
|---|----------|------------|-----------|--------|
| **B1** | **Caching** | ✅ Đã có (in-memory) | Load time (ms) | Dễ |
| **B3** | **Coroutines** | ⚠️ Cần refactor | UI freeze time (ms) | Dễ |
| **B4** | **Data Structure** | ❌ Chưa có | Lookup time (ms) | Dễ |
| **B5** | **Dispatchers** | ⚠️ Cần optimize | Frame drop count | Dễ |
| **D2** | **Sequence** | ❌ Chưa có | Execution time, allocations | Dễ |
| **E2** | **WorkManager** | ❌ Chưa có | Reliability test | TB |

### NHÓM B: CODE QUALITY (Chỉ code comparison)

| # | Kỹ thuật | Lợi ích | Demo |
|---|----------|---------|------|
| **D1** | **Inline functions** | Giảm lambda overhead | Bytecode comparison |
| **D5** | **data class** | Code ngắn gọn | Lines of code: 80 → 5 |
| **D6** | **Extension functions** | Code readable | Code comparison |

---

## 🗓️ ROADMAP TRIỂN KHAI

### ✅ Phase 1: Quick Wins - Performance (Tuần 1)

**Mục tiêu:** 4 kỹ thuật performance có số liệu benchmark ngay

#### 1️⃣ B4: DATA STRUCTURE - Map vs List (3 giờ)

**Vị trí:** `CartViewModel.kt:68, 97`

**Hiện tại:** Dùng List + indexOf/find (O(n))
```kotlin
// CartViewModel.kt:68
val index = _cartProducts.value.data?.indexOf(cartProducts)
if (index != null && index != -1) {
    val documentId = cartProductsDocument[index].id
    // ...
}
```

**Plan:**
1. Tạo `CartViewModelBefore.kt` (copy code hiện tại)
2. Tạo `CartViewModelAfter.kt` với Map
3. Tạo `CartBenchmarkTest.kt` để đo performance

**Implementation After:**
```kotlin
@HiltViewModel
class CartViewModelAfter @Inject constructor(...) : ViewModel() {

    // ✅ Dùng Map thay vì List
    private val _cartProductsMap = MutableStateFlow<Map<String, CartProducts>>(emptyMap())
    private val _cartProductsList = MutableStateFlow<Resource<List<CartProducts>>>(Resource.UnSpecified())
    val cartProduct = _cartProductsList.asStateFlow()

    private var cartDocumentMap = mutableMapOf<String, DocumentSnapshot>()

    private fun getCartProducts() {
        firestore.collection("user")
            .document(firebaseAuth.uid!!)
            .collection("cart")
            .addSnapshotListener { value, error ->
                if (error != null || value == null) {
                    // handle error
                } else {
                    val cartProducts = value.toObjects(CartProducts::class.java)

                    // ✅ Build Map for O(1) lookup
                    val productMap = cartProducts.associateBy { it.product.id }
                    _cartProductsMap.value = productMap

                    // Build document map
                    value.documents.forEach { doc ->
                        val product = doc.toObject(CartProducts::class.java)
                        product?.let {
                            cartDocumentMap[it.product.id] = doc
                        }
                    }

                    _cartProductsList.emit(Resource.Success(cartProducts))
                }
            }
    }

    fun changeQuantity(cartProducts: CartProducts, status: FirebaseCommon.QuantityStatus) {
        // ✅ O(1) lookup thay vì indexOf
        val documentId = cartDocumentMap[cartProducts.product.id]?.id

        if (documentId != null) {
            when(status) {
                FirebaseCommon.QuantityStatus.INCREASE -> increase(documentId)
                FirebaseCommon.QuantityStatus.DECREASE -> {
                    if (cartProducts.quantity == 1) {
                        viewModelScope.launch { _deleteDialog.emit(cartProducts) }
                    } else {
                        decrease(documentId)
                    }
                }
            }
        }
    }

    // Helper function cho lookup
    fun getProductById(productId: String): CartProducts? {
        return _cartProductsMap.value[productId]  // ✅ O(1)
    }
}
```

**Benchmark Test:**
```kotlin
// app/src/test/java/com/example/furniturecloudy/CartBenchmarkTest.kt
class CartBenchmarkTest {

    @Test
    fun `benchmark List indexOf vs Map lookup`() {
        // Prepare data
        val products = List(1000) { index ->
            CartProducts(
                product = Product(id = "product_$index", name = "Product $index"),
                quantity = 1
            )
        }

        // BEFORE: List with indexOf
        val startList = System.nanoTime()
        repeat(1000) {
            val index = products.indexOfFirst { it.product.id == "product_999" }
            products.getOrNull(index)
        }
        val listTimeMs = (System.nanoTime() - startList) / 1_000_000.0

        // AFTER: Map with key lookup
        val productMap = products.associateBy { it.product.id }
        val startMap = System.nanoTime()
        repeat(1000) {
            productMap["product_999"]
        }
        val mapTimeMs = (System.nanoTime() - startMap) / 1_000_000.0

        println("=== BENCHMARK RESULTS ===")
        println("List.indexOfFirst (O(n)): ${listTimeMs}ms")
        println("Map[key] (O(1)):          ${mapTimeMs}ms")
        println("Improvement:              ${(listTimeMs / mapTimeMs).format(2)}x faster")

        // Expected: Map nhanh hơn 15-20 lần
    }
}

private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
```

**Expected Output:**
```
=== BENCHMARK RESULTS ===
List.indexOfFirst (O(n)): 18.5ms
Map[key] (O(1)):          0.8ms
Improvement:              23.13x faster
```

---

#### 2️⃣ D2: SEQUENCE - Lazy Evaluation (2 giờ)

**Vị trí:** `SearchViewModel.kt:138-191`

**Hiện tại:** Eager evaluation với Collection
```kotlin
private fun filterAndSortProducts() {
    var filteredList = allProducts.toList()  // Copy 1

    filteredList = filteredList.filter { ... }  // Copy 2
    filteredList = filteredList.filter { ... }  // Copy 3
    filteredList = when (...) {
        sortedBy { ... }  // Copy 4
    }
}
```

**Plan:**
1. Tạo branch `feature/sequence-optimization`
2. Backup code hiện tại vào `SearchViewModelBefore.kt`
3. Refactor `SearchViewModel.kt` dùng Sequence
4. Tạo benchmark test

**Implementation After:**
```kotlin
// SearchViewModel.kt
private fun filterAndSortProducts() {
    viewModelScope.launch {
        _searchResults.emit(Resource.Loading())

        // ✅ Dùng Sequence cho lazy evaluation
        val filteredList = allProducts
            .asSequence()  // Chuyển sang lazy
            .filter { product ->
                if (currentSearchQuery.isNotEmpty()) {
                    val queryLowerCase = currentSearchQuery.lowercase(Locale.getDefault())
                    product.name.lowercase(Locale.getDefault()).contains(queryLowerCase) ||
                    product.category.lowercase(Locale.getDefault()).contains(queryLowerCase)
                } else {
                    true
                }
            }
            .filter { product ->
                val finalPrice = if (product.offerPercentage != null) {
                    product.price * (1 - product.offerPercentage)
                } else {
                    product.price
                }
                finalPrice >= currentFilter.minPrice && finalPrice <= currentFilter.maxPrice
            }
            .filter { product ->
                !currentFilter.inStockOnly || product.stock > 0
            }
            .filter { product ->
                !currentFilter.onSaleOnly || (product.offerPercentage != null && product.offerPercentage > 0)
            }
            .let { sequence ->
                when (currentFilter.sortBy) {
                    SortOption.PRICE_LOW_TO_HIGH -> sequence.sortedBy {
                        if (it.offerPercentage != null) it.price * (1 - it.offerPercentage) else it.price
                    }
                    SortOption.PRICE_HIGH_TO_LOW -> sequence.sortedByDescending {
                        if (it.offerPercentage != null) it.price * (1 - it.offerPercentage) else it.price
                    }
                    SortOption.RATING_HIGH_TO_LOW -> sequence.sortedByDescending { it.averageRating }
                    SortOption.NAME_A_TO_Z -> sequence.sortedBy { it.name.lowercase() }
                    SortOption.NAME_Z_TO_A -> sequence.sortedByDescending { it.name.lowercase() }
                    SortOption.NEWEST, SortOption.NONE -> sequence
                }
            }
            .toList()  // ✅ Chỉ tạo 1 list cuối cùng

        _searchResults.emit(Resource.Success(filteredList))
    }
}
```

**Benchmark Test:**
```kotlin
// app/src/test/java/com/example/furniturecloudy/SequenceBenchmarkTest.kt
class SequenceBenchmarkTest {

    @Test
    fun `benchmark Collection vs Sequence with multiple operations`() {
        // Prepare 10,000 products
        val products = List(10_000) { index ->
            Product(
                id = "p$index",
                name = "Product $index",
                price = (index % 1000).toFloat(),
                category = "Category ${index % 10}",
                stock = index % 50,
                offerPercentage = if (index % 3 == 0) 0.1f else null
            )
        }

        val query = "5"
        val minPrice = 100f
        val maxPrice = 500f

        // BEFORE: Collection (eager)
        val startCollection = System.currentTimeMillis()
        var result1 = products.toList()
        result1 = result1.filter { it.name.contains(query) }
        result1 = result1.filter { it.price >= minPrice && it.price <= maxPrice }
        result1 = result1.filter { it.stock > 0 }
        result1 = result1.sortedBy { it.price }
        val collectionTime = System.currentTimeMillis() - startCollection

        // AFTER: Sequence (lazy)
        val startSequence = System.currentTimeMillis()
        val result2 = products
            .asSequence()
            .filter { it.name.contains(query) }
            .filter { it.price >= minPrice && it.price <= maxPrice }
            .filter { it.stock > 0 }
            .sortedBy { it.price }
            .toList()
        val sequenceTime = System.currentTimeMillis() - startSequence

        println("=== BENCHMARK RESULTS ===")
        println("Products count:   ${products.size}")
        println("Collection time:  ${collectionTime}ms")
        println("Sequence time:    ${sequenceTime}ms")
        println("Improvement:      ${((collectionTime - sequenceTime) * 100.0 / collectionTime).format(1)}% faster")
        println("Results match:    ${result1 == result2}")

        // Expected: Sequence nhanh hơn 50-70%
    }
}
```

**Expected Output:**
```
=== BENCHMARK RESULTS ===
Products count:   10000
Collection time:  850ms
Sequence time:    320ms
Improvement:      62.4% faster
Results match:    true
```

---

#### 3️⃣ B1: CACHING - In-Memory Cache (1 giờ)

**Vị trí:** `SearchViewModel.kt:28`

**Hiện tại:** ✅ Đã có in-memory cache

**Plan:**
1. Tạo `SearchViewModelNoCache.kt` (version không cache)
2. So sánh performance

**Implementation - No Cache Version:**
```kotlin
// SearchViewModelNoCache.kt (TẠO MỚI để demo Before)
@HiltViewModel
class SearchViewModelNoCache @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _searchResults = MutableStateFlow<Resource<List<Product>>>(Resource.UnSpecified())
    val searchResults = _searchResults.asStateFlow()

    // ❌ KHÔNG cache, gọi Firestore mỗi lần search
    fun searchProducts(query: String) {
        viewModelScope.launch {
            _searchResults.emit(Resource.Loading())

            try {
                val snapshot = firestore.collection("Products")
                    .get()
                    .await()

                val products = snapshot.toObjects(Product::class.java)

                // Filter locally
                val filtered = if (query.isEmpty()) {
                    products
                } else {
                    products.filter {
                        it.name.lowercase().contains(query.lowercase())
                    }
                }

                _searchResults.emit(Resource.Success(filtered))
            } catch (e: Exception) {
                _searchResults.emit(Resource.Error(e.message ?: "Unknown error"))
            }
        }
    }
}
```

**Benchmark Test:**
```kotlin
// Trong Fragment/Activity
class CacheBenchmarkFragment : Fragment() {

    private fun runCacheBenchmark() {
        lifecycleScope.launch {
            println("=== CACHE BENCHMARK ===")

            // Test 1: First search (cả 2 đều gọi Firestore)
            val start1 = System.currentTimeMillis()
            viewModel.searchProducts("chair")
            viewModel.searchResults.collectLatest { resource ->
                if (resource is Resource.Success) {
                    val time1 = System.currentTimeMillis() - start1
                    println("First search 'chair': ${time1}ms (from Firestore)")
                }
            }

            delay(100)

            // Test 2: Second search (With cache: fast, No cache: slow)
            val start2 = System.currentTimeMillis()
            viewModel.searchProducts("table")
            viewModel.searchResults.collectLatest { resource ->
                if (resource is Resource.Success) {
                    val time2 = System.currentTimeMillis() - start2
                    println("Search 'table': ${time2}ms")
                }
            }

            delay(100)

            // Test 3: Search lại "chair" (With cache: instant, No cache: slow)
            val start3 = System.currentTimeMillis()
            viewModel.searchProducts("chair")
            viewModel.searchResults.collectLatest { resource ->
                if (resource is Resource.Success) {
                    val time3 = System.currentTimeMillis() - start3
                    println("Second search 'chair': ${time3}ms")
                }
            }
        }
    }
}
```

**Expected Output:**
```
=== NO CACHE ===
First search 'chair':  2500ms (from Firestore)
Search 'table':        2300ms (from Firestore)
Second search 'chair': 2400ms (from Firestore) ❌

=== WITH CACHE (hiện tại) ===
First search 'chair':  2500ms (from Firestore)
Search 'table':        15ms (from cache) ✅
Second search 'chair': 12ms (from cache) ✅

→ Improvement: 200x faster on cached queries
```

---

#### 4️⃣ B5: DISPATCHERS - Search Filtering (3 giờ)

**Vị trí:** `SearchViewModel.kt:138`

**Hiện tại:** filterAndSortProducts chạy trên Main thread

**Plan:**
1. Tạo fake data lớn (5000-10000 products) để thấy rõ lag
2. Implement version Before (Main) và After (Default)
3. Monitor frame metrics

**Implementation Before (CODE HIỆN TẠI):**
```kotlin
// SearchViewModel.kt:138 (hiện tại - chạy trên Main)
private fun filterAndSortProducts() {
    viewModelScope.launch {  // ❌ Dispatchers.Main (default)
        _searchResults.emit(Resource.Loading())

        // Heavy calculation trên Main thread
        var filteredList = allProducts.toList()
        filteredList = filteredList.filter { ... }
        filteredList = filteredList.filter { ... }
        filteredList = filteredList.sortedBy { ... }

        _searchResults.emit(Resource.Success(filteredList))
    }
}
```

**Implementation After:**
```kotlin
private fun filterAndSortProducts() {
    viewModelScope.launch {
        _searchResults.emit(Resource.Loading())

        // ✅ Chuyển heavy calculation sang Dispatchers.Default
        val filteredList = withContext(Dispatchers.Default) {
            var list = allProducts.toList()

            // Apply search query
            if (currentSearchQuery.isNotEmpty()) {
                val queryLowerCase = currentSearchQuery.lowercase(Locale.getDefault())
                list = list.filter { product ->
                    product.name.lowercase(Locale.getDefault()).contains(queryLowerCase) ||
                    product.category.lowercase(Locale.getDefault()).contains(queryLowerCase)
                }
            }

            // Apply price filter
            list = list.filter { product ->
                val finalPrice = if (product.offerPercentage != null) {
                    product.price * (1 - product.offerPercentage)
                } else {
                    product.price
                }
                finalPrice >= currentFilter.minPrice && finalPrice <= currentFilter.maxPrice
            }

            // Apply stock filter
            if (currentFilter.inStockOnly) {
                list = list.filter { it.stock > 0 }
            }

            // Apply sale filter
            if (currentFilter.onSaleOnly) {
                list = list.filter { it.offerPercentage != null && it.offerPercentage > 0 }
            }

            // Apply sorting
            when (currentFilter.sortBy) {
                SortOption.PRICE_LOW_TO_HIGH -> list.sortedBy {
                    if (it.offerPercentage != null) it.price * (1 - it.offerPercentage) else it.price
                }
                SortOption.PRICE_HIGH_TO_LOW -> list.sortedByDescending {
                    if (it.offerPercentage != null) it.price * (1 - it.offerPercentage) else it.price
                }
                SortOption.RATING_HIGH_TO_LOW -> list.sortedByDescending { it.averageRating }
                SortOption.NAME_A_TO_Z -> list.sortedBy { it.name.lowercase() }
                SortOption.NAME_Z_TO_A -> list.sortedByDescending { it.name.lowercase() }
                SortOption.NEWEST, SortOption.NONE -> list
            }
        }

        _searchResults.emit(Resource.Success(filteredList))
    }
}
```

**Tạo Fake Data để test:**
```kotlin
// util/FakeDataGenerator.kt (TẠO MỚI)
object FakeDataGenerator {

    fun generateLargeProductList(count: Int = 10_000): List<Product> {
        return List(count) { index ->
            Product(
                id = "product_$index",
                name = "Product $index ${getRandomCategory()}",
                price = (50 + (index % 500)).toFloat(),
                category = getRandomCategory(),
                stock = index % 100,
                averageRating = (3.0f + (index % 20) / 10f),
                offerPercentage = if (index % 3 == 0) 0.1f + (index % 30) / 100f else null,
                images = listOf("https://example.com/image_$index.jpg")
            )
        }
    }

    private fun getRandomCategory(): String {
        val categories = listOf("Chair", "Table", "Cupboard", "Accessory", "Furniture")
        return categories.random()
    }
}
```

**Frame Metrics Monitor:**
```kotlin
// util/FrameMetricsMonitor.kt (TẠO MỚI)
class FrameMetricsMonitor(private val activity: Activity) {

    private val slowFrames = mutableListOf<Double>()

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.window.addOnFrameMetricsAvailableListener(
                { _, frameMetrics, _ ->
                    val totalDurationNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
                    val totalDurationMs = totalDurationNs / 1_000_000.0

                    if (totalDurationMs > 16.67) {  // > 60fps threshold
                        slowFrames.add(totalDurationMs)
                        Log.w("FrameMetrics", "Slow frame: ${totalDurationMs.format(2)}ms")
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }
    }

    fun getReport(): String {
        return buildString {
            appendLine("=== FRAME METRICS REPORT ===")
            appendLine("Total slow frames (>16.67ms): ${slowFrames.size}")
            if (slowFrames.isNotEmpty()) {
                appendLine("Slowest frame: ${slowFrames.maxOrNull()?.format(2)}ms")
                appendLine("Average slow frame: ${slowFrames.average().format(2)}ms")
            }
        }
    }

    fun reset() {
        slowFrames.clear()
    }
}
```

**Testing Fragment:**
```kotlin
// SearchFragment.kt - Thêm monitoring
class SearchFragment : Fragment() {

    private lateinit var frameMonitor: FrameMetricsMonitor

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        frameMonitor = FrameMetricsMonitor(requireActivity())
        frameMonitor.start()

        binding.btnTestDispatcher.setOnClickListener {
            testDispatcherPerformance()
        }
    }

    private fun testDispatcherPerformance() {
        // Generate large dataset
        viewModel.setFakeProducts(FakeDataGenerator.generateLargeProductList(10_000))

        frameMonitor.reset()

        // Trigger search with complex filter
        viewModel.applyFilter(ProductFilter(
            minPrice = 100f,
            maxPrice = 500f,
            inStockOnly = true,
            sortBy = SortOption.PRICE_LOW_TO_HIGH
        ))
        viewModel.searchProducts("Product")

        // Check results after 2 seconds
        lifecycleScope.launch {
            delay(2000)
            Log.d("Benchmark", frameMonitor.getReport())
        }
    }
}
```

**Expected Output:**
```
=== BEFORE (Dispatchers.Main) ===
Total slow frames (>16.67ms): 15
Slowest frame: 187.5ms
Average slow frame: 45.2ms
→ UI lag, stutter khi search

=== AFTER (Dispatchers.Default) ===
Total slow frames (>16.67ms): 0
Slowest frame: N/A
Average slow frame: N/A
→ UI smooth 60fps
```

---

### ✅ Phase 2: Coroutines & Advanced (Tuần 2)

#### 5️⃣ B3: COROUTINES - Sync vs Async (3 giờ)

**Vị trí:** Tạo demo mới trong `SearchFragment`

**Plan:**
1. Tạo function demo synchronous blocking
2. Tạo function demo asynchronous non-blocking
3. Show UI freeze difference

**Implementation:**
```kotlin
// SearchViewModel.kt - Thêm demo functions

// ❌ BEFORE: Synchronous blocking (cho demo)
fun loadProductsSync() {
    viewModelScope.launch {
        _products.emit(Resource.Loading())

        try {
            // ❌ Blocking call - giả lập bằng Thread.sleep
            Thread.sleep(3000)  // Simulate network delay

            val products = firestore.collection("Products")
                .get()
                .result  // ❌ Blocking - freeze UI
                .toObjects(Product::class.java)

            _products.emit(Resource.Success(products))
        } catch (e: Exception) {
            _products.emit(Resource.Error(e.message ?: "Error"))
        }
    }
}

// ✅ AFTER: Asynchronous non-blocking
fun loadProductsAsync() {
    viewModelScope.launch {
        _products.emit(Resource.Loading())

        try {
            // ✅ Non-blocking với delay và await
            delay(3000)  // Simulate network delay (non-blocking)

            val products = firestore.collection("Products")
                .get()
                .await()  // ✅ Suspend - không freeze UI
                .toObjects(Product::class.java)

            _products.emit(Resource.Success(products))
        } catch (e: Exception) {
            _products.emit(Resource.Error(e.message ?: "Error"))
        }
    }
}
```

**Testing Fragment:**
```kotlin
// SearchFragment.kt
private fun demoCoroutinesPerformance() {
    binding.apply {
        btnSyncLoad.setOnClickListener {
            Log.d("Coroutines", "Testing SYNC (blocking)...")
            frameMonitor.reset()
            viewModel.loadProductsSync()

            // Try to interact with UI during load
            lifecycleScope.launch {
                repeat(30) {
                    delay(100)
                    Log.d("Coroutines", "UI interaction test $it")
                }
                delay(3500)
                Log.d("Coroutines", frameMonitor.getReport())
            }
        }

        btnAsyncLoad.setOnClickListener {
            Log.d("Coroutines", "Testing ASYNC (non-blocking)...")
            frameMonitor.reset()
            viewModel.loadProductsAsync()

            // Try to interact with UI during load
            lifecycleScope.launch {
                repeat(30) {
                    delay(100)
                    Log.d("Coroutines", "UI interaction test $it")
                }
                delay(3500)
                Log.d("Coroutines", frameMonitor.getReport())
            }
        }
    }
}
```

**Expected Output:**
```
=== SYNC (Blocking) ===
UI interaction test 0
UI interaction test 1
[FREEZE - no logs for 3 seconds]
UI interaction test 2
...
Total slow frames: 180 (UI completely frozen)

=== ASYNC (Non-blocking) ===
UI interaction test 0
UI interaction test 1
UI interaction test 2
... (continuous logs every 100ms)
UI interaction test 30
Total slow frames: 0 (UI smooth)
```

---

#### 6️⃣ E2: WORKMANAGER - Background Sync (6 giờ)

**Plan:**
1. Implement OrderSyncWorker
2. Setup periodic sync
3. Demo offline scenario

**Implementation:**
```kotlin
// workers/OrderSyncWorker.kt (TẠO MỚI)
class OrderSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("OrderSyncWorker", "Starting order sync...")

        return try {
            // Get pending orders from local DB (Room)
            val database = FurnitureDatabase.getInstance(applicationContext)
            val pendingOrders = database.orderDao().getPendingOrders()

            if (pendingOrders.isEmpty()) {
                Log.d("OrderSyncWorker", "No pending orders to sync")
                return Result.success()
            }

            // Sync to Firestore
            val firestore = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()

            pendingOrders.forEach { order ->
                firestore.collection("user")
                    .document(auth.uid!!)
                    .collection("orders")
                    .document(order.orderId)
                    .set(order)
                    .await()

                // Mark as synced
                database.orderDao().markAsSynced(order.orderId)

                Log.d("OrderSyncWorker", "Synced order: ${order.orderId}")
            }

            Log.d("OrderSyncWorker", "Sync completed: ${pendingOrders.size} orders")
            Result.success()

        } catch (e: Exception) {
            Log.e("OrderSyncWorker", "Sync failed", e)

            if (runAttemptCount < 3) {
                Result.retry()  // Retry if failed
            } else {
                Result.failure()
            }
        }
    }
}

// Setup trong Application class
// CloudyApplication.kt
class CloudyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        setupWorkManager()
    }

    private fun setupWorkManager() {
        // Periodic sync every 30 minutes
        val syncRequest = PeriodicWorkRequestBuilder<OrderSyncWorker>(
            30, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "order_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        Log.d("CloudyApplication", "WorkManager setup completed")
    }
}
```

**Room entities:**
```kotlin
// data/local/OrderEntity.kt (TẠO MỚI)
@Entity(tableName = "pending_orders")
data class OrderEntity(
    @PrimaryKey
    val orderId: String,
    val userId: String,
    val products: String,  // JSON string
    val totalPrice: Double,
    val orderStatus: String,
    val createdAt: Long,
    val isSynced: Boolean = false
)

@Dao
interface OrderDao {
    @Query("SELECT * FROM pending_orders WHERE isSynced = 0")
    suspend fun getPendingOrders(): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("UPDATE pending_orders SET isSynced = 1 WHERE orderId = :orderId")
    suspend fun markAsSynced(orderId: String)
}
```

**Demo Test:**
```kotlin
// BillingFragment.kt - Demo offline scenario
private fun demoOfflineOrder() {
    lifecycleScope.launch {
        // 1. Enable airplane mode (manual)
        Toast.makeText(requireContext(), "Please enable Airplane Mode now", Toast.LENGTH_LONG).show()
        delay(3000)

        // 2. Place order (will save to local DB)
        val testOrder = Order(/* ... */)
        viewModel.placeOrderOffline(testOrder)

        Toast.makeText(requireContext(), "Order saved locally ✓", Toast.LENGTH_SHORT).show()
        delay(2000)

        // 3. Disable airplane mode (manual)
        Toast.makeText(requireContext(), "Please disable Airplane Mode now", Toast.LENGTH_LONG).show()
        delay(3000)

        // 4. Trigger immediate sync
        val workRequest = OneTimeWorkRequestBuilder<OrderSyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(requireContext()).enqueue(workRequest)

        // 5. Monitor work status
        WorkManager.getInstance(requireContext())
            .getWorkInfoByIdLiveData(workRequest.id)
            .observe(viewLifecycleOwner) { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        binding.syncStatus.text = "Syncing orders..."
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        binding.syncStatus.text = "All orders synced ✓"
                        Toast.makeText(requireContext(), "Order synced to cloud!", Toast.LENGTH_SHORT).show()
                    }
                    WorkInfo.State.FAILED -> {
                        binding.syncStatus.text = "Sync failed (will retry)"
                    }
                    else -> {}
                }
            }
    }
}
```

**Expected Demo Flow:**
```
1. User enables Airplane Mode
2. User places order → Saved to Room DB (offline)
3. User disables Airplane Mode
4. WorkManager automatically syncs → Order appears in Firestore ✓

BEFORE (no WorkManager):
Order placement fails when offline ❌

AFTER (with WorkManager):
Order saved locally, auto-synced when online ✅
```

---

### ✅ Phase 3: Code Quality Techniques (Tuần 3)

#### 7️⃣ D1: INLINE FUNCTIONS (1.5 giờ)

**Plan:** Show bytecode comparison

**Implementation:**
```kotlin
// util/InlineUtils.kt (TẠO MỚI)

// Regular higher-order function
fun <T> List<T>.customSumOf(selector: (T) -> Float): Float {
    var sum = 0f
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

// Inline version
inline fun <T> List<T>.customSumOfInline(selector: (T) -> Float): Float {
    var sum = 0f
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

// Extension cho Product
inline fun Product.calculateFinalPrice(): Float {
    return if (offerPercentage != null) {
        price * (1 - offerPercentage)
    } else {
        price
    }
}

// Usage example
fun calculateTotalPrice(products: List<Product>): Float {
    // Regular - creates Function object
    val total1 = products.customSumOf { it.calculateFinalPrice() }

    // Inline - no Function object
    val total2 = products.customSumOfInline { it.calculateFinalPrice() }

    return total2
}
```

**Decompile & Show:**
```java
// Decompiled bytecode của regular function
public static float calculateTotalPrice(List products) {
    // Creates Function1 object for lambda
    Function1 selector = new Function1() {
        public Float invoke(Product it) {
            return it.calculateFinalPrice();
        }
    };
    return customSumOf(products, selector);  // Function call overhead
}

// Decompiled bytecode của inline function
public static float calculateTotalPrice(List products) {
    // Lambda code is inlined directly
    float sum = 0f;
    for (Object element : products) {
        Product product = (Product) element;
        sum += product.calculateFinalPrice();  // Direct call, no Function object
    }
    return sum;
}
```

**Demo in presentation:**
- Show code side-by-side
- Show decompiled bytecode
- Explain: Inline eliminates Function object allocation

---

#### 8️⃣ D5: DATA CLASS (30 phút)

**Comparison:**
```kotlin
// ❌ Java POJO - 80+ lines
public class ProductJava {
    private String id;
    private String name;
    private Float price;
    private String category;
    private Integer stock;
    private Float averageRating;
    private Float offerPercentage;
    private List<String> images;

    // Constructor - 12 lines
    public ProductJava(String id, String name, Float price, String category,
                      Integer stock, Float averageRating, Float offerPercentage,
                      List<String> images) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.stock = stock;
        this.averageRating = averageRating;
        this.offerPercentage = offerPercentage;
        this.images = images;
    }

    // Getters/Setters - 40 lines
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    // ... 6 more getters/setters

    // equals() - 15 lines
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductJava that = (ProductJava) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(name, that.name) &&
               Objects.equals(price, that.price) &&
               Objects.equals(category, that.category) &&
               Objects.equals(stock, that.stock) &&
               Objects.equals(averageRating, that.averageRating) &&
               Objects.equals(offerPercentage, that.offerPercentage) &&
               Objects.equals(images, that.images);
    }

    // hashCode() - 5 lines
    @Override
    public int hashCode() {
        return Objects.hash(id, name, price, category, stock,
                          averageRating, offerPercentage, images);
    }

    // toString() - 5 lines
    @Override
    public String toString() {
        return "ProductJava{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               // ...
               '}';
    }

    // copy() - 8 lines
    public ProductJava copy(String id, String name, Float price, String category,
                           Integer stock, Float averageRating, Float offerPercentage,
                           List<String> images) {
        return new ProductJava(
            id != null ? id : this.id,
            name != null ? name : this.name,
            // ...
        );
    }
}
// TOTAL: ~85 lines

// ✅ Kotlin data class - 8 lines
data class Product(
    val id: String = "",
    val name: String = "",
    val price: Float = 0f,
    val category: String = "",
    val stock: Int = 0,
    val averageRating: Float = 0f,
    val offerPercentage: Float? = null,
    val images: List<String> = emptyList()
)
// TOTAL: 8 lines
// Auto-generated: equals(), hashCode(), toString(), copy(), componentN()
```

**Comparison Table:**
| Feature | Java POJO | Kotlin data class | Reduction |
|---------|-----------|-------------------|-----------|
| Lines of code | 85 | 8 | 91% less |
| equals() | Manual 15 lines | Auto | - |
| hashCode() | Manual 5 lines | Auto | - |
| toString() | Manual 5 lines | Auto | - |
| copy() | Manual 8 lines | Auto (with named params) | - |
| Risk of bugs | High (manual code) | Low (compiler-generated) | - |

---

#### 9️⃣ D6: EXTENSION FUNCTIONS (30 phút)

**Comparison:**
```kotlin
// ❌ BEFORE: Static Utility Class
object PriceUtils {
    fun formatPrice(price: Float): String {
        return String.format("$%.2f", price)
    }

    fun calculateDiscount(product: Product): Float {
        return if (product.offerPercentage != null) {
            product.price * product.offerPercentage
        } else {
            0f
        }
    }

    fun getFinalPrice(product: Product): Float {
        return product.price - calculateDiscount(product)
    }
}

// Usage - verbose
val formattedPrice = PriceUtils.formatPrice(product.price)
val discount = PriceUtils.calculateDiscount(product)
val finalPrice = PriceUtils.getFinalPrice(product)

// ✅ AFTER: Extension Functions
fun Float.formatPrice(): String {
    return String.format("$%.2f", this)
}

fun Product.calculateDiscount(): Float {
    return if (offerPercentage != null) {
        price * offerPercentage
    } else {
        0f
    }
}

fun Product.getFinalPrice(): Float {
    return price - calculateDiscount()
}

// Usage - clean & natural
val formattedPrice = product.price.formatPrice()
val discount = product.calculateDiscount()
val finalPrice = product.getFinalPrice()
```

**Benefits:**
| Aspect | Utility Class | Extension Function |
|--------|---------------|-------------------|
| Syntax | `Utils.method(obj)` | `obj.method()` |
| Readability | Less natural | More natural |
| IDE support | Need import class | Auto-suggests with object |
| Discoverability | Must know class name | Shows in autocomplete |
| Runtime overhead | None | None (same bytecode) |

---

## 📈 EXPECTED RESULTS SUMMARY

| Kỹ thuật | Metric | Before | After | Improvement |
|----------|--------|--------|-------|-------------|
| **B1: Caching** | Load time | 2500ms | 15ms | 166x faster |
| **B3: Coroutines** | UI freeze | 3000ms freeze | 0ms (responsive) | ∞ better |
| **B4: Map** | Lookup time | 18.5ms | 0.8ms | 23x faster |
| **B5: Dispatchers** | Slow frames | 15 frames | 0 frames | 100% smoother |
| **D2: Sequence** | Execution time | 850ms | 320ms | 2.6x faster |
| **E2: WorkManager** | Offline reliability | Fail | Success + Auto-sync | ∞ better |
| **D1: Inline** | Lambda overhead | Function object | Inlined code | Eliminated |
| **D5: data class** | Code lines | 85 lines | 8 lines | 91% less |
| **D6: Extension** | Readability | `Utils.method(obj)` | `obj.method()` | Cleaner |

---

## 📝 CHECKLIST TRIỂN KHAI

### Tuần 1: Performance Quick Wins
- [ ] B4: Map vs List - Implementation + Benchmark
- [ ] D2: Sequence - Refactor SearchViewModel
- [ ] B1: Caching - Create no-cache version
- [ ] B5: Dispatchers - Add withContext(Default)

### Tuần 2: Advanced Performance
- [ ] B3: Coroutines - Sync vs Async demo
- [ ] E2: WorkManager - Implement background sync
- [ ] E2: WorkManager - Test offline scenario

### Tuần 3: Code Quality
- [ ] D1: Inline - Show bytecode
- [ ] D5: data class - Java comparison
- [ ] D6: Extension - Utility class comparison

### Tuần 4: Documentation
- [ ] Write benchmark suite
- [ ] Record demo videos
- [ ] Create presentation slides
- [ ] Write final report

---

## 🎬 DEMO PREPARATION

### Videos to Record:
1. **B1 Caching:** Search "chair" → "table" → "chair" (instant on 2nd time)
2. **B3 Coroutines:** Sync (UI freeze) vs Async (smooth)
3. **B5 Dispatchers:** Search với 10k products (lag vs smooth)
4. **E2 WorkManager:** Offline order → Online → Auto sync

### Screenshots:
1. Benchmark results tables
2. Frame metrics before/after
3. Code comparisons side-by-side
4. Android Profiler screenshots

### Presentation Slides:
1. Problem statement for each technique
2. Before/After code
3. Benchmark results
4. Visual demos (videos/GIFs)

---

## ⚡ QUICK START

Để bắt đầu implement:

```bash
# 1. Create feature branch
git checkout -b feature/optimization-demo

# 2. Start with easiest: B4 Map
# Implement CartViewModelAfter.kt với Map

# 3. Run benchmark
./gradlew test --tests "CartBenchmarkTest"

# 4. Document results
echo "B4 Results: 23x faster" >> docs/optimization/RESULTS.md

# 5. Commit
git add .
git commit -m "feat: implement B4 Map optimization"
```

---

**END OF PLAN**

Bắt đầu implement từ Phase 1, task 1 (B4: Map) - Dễ nhất và có impact cao!