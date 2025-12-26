package com.example.furniturecloudy.present

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.furniturecloudy.databinding.ActivityFakeDataManagerBinding
import com.example.furniturecloudy.util.FirebaseFakeDataManager
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity để quản lý fake data trên Firebase
 *
 * 🎯 CÁCH MỞ:
 * Có 2 cách để mở Activity này:
 *
 * 1. Từ adb command:
 *    adb shell am start -n com.example.furniturecloudy/.present.FakeDataManagerActivity
 *
 * 2. Từ code (thêm vào Settings hoặc bất kỳ đâu):
 *    startActivity(Intent(this, FakeDataManagerActivity::class.java))
 *
 * ⚠️ CHÚ Ý: Activity này CHỈ dùng cho development/benchmark
 */
@AndroidEntryPoint
class FakeDataManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFakeDataManagerBinding

    @Inject
    lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFakeDataManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        refreshStats()
    }

    private fun setupUI() {
        // Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Fake Data Manager"
            setDisplayHomeAsUpEnabled(true)
        }

        // Button listeners
        binding.btnInsert1000.setOnClickListener {
            showInsertDialog(1000)
        }

        binding.btnInsert5000.setOnClickListener {
            showInsertDialog(5000)
        }

        binding.btnDeleteAll.setOnClickListener {
            showDeleteDialog()
        }

        binding.btnRefresh.setOnClickListener {
            refreshStats()
        }
    }

    private fun showInsertDialog(count: Int) {
        AlertDialog.Builder(this)
            .setTitle("Insert Fake Data")
            .setMessage("Bạn có chắc muốn insert $count fake products lên Firebase?\n\nThời gian ước tính: ~${count / 100} giây")
            .setPositiveButton("Insert") { _, _ ->
                insertFakeProducts(count)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete All Fake Data")
            .setMessage("⚠️ Bạn có chắc muốn XÓA TẤT CẢ fake products?\n\nHành động này KHÔNG THỂ HOÀN TÁC!")
            .setPositiveButton("Delete") { _, _ ->
                deleteAllFakeProducts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun insertFakeProducts(count: Int) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "Đang insert $count products..."

        lifecycleScope.launch {
            val result = FirebaseFakeDataManager.insertFakeProducts(firestore, count)

            result.onSuccess { insertedCount ->
                binding.tvStatus.text = "✅ Đã insert $insertedCount products thành công!"
                Toast.makeText(
                    this@FakeDataManagerActivity,
                    "Success: Inserted $insertedCount products",
                    Toast.LENGTH_LONG
                ).show()
                refreshStats()
            }

            result.onFailure { error ->
                binding.tvStatus.text = "❌ Lỗi: ${error.message}"
                Toast.makeText(
                    this@FakeDataManagerActivity,
                    "Error: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

            binding.progressBar.visibility = View.GONE
        }
    }

    private fun deleteAllFakeProducts() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "Đang xóa fake products..."

        lifecycleScope.launch {
            val result = FirebaseFakeDataManager.deleteAllFakeProducts(firestore)

            result.onSuccess { deletedCount ->
                binding.tvStatus.text = "✅ Đã xóa $deletedCount fake products!"
                Toast.makeText(
                    this@FakeDataManagerActivity,
                    "Success: Deleted $deletedCount products",
                    Toast.LENGTH_LONG
                ).show()
                refreshStats()
            }

            result.onFailure { error ->
                binding.tvStatus.text = "❌ Lỗi: ${error.message}"
                Toast.makeText(
                    this@FakeDataManagerActivity,
                    "Error: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

            binding.progressBar.visibility = View.GONE
        }
    }

    private fun refreshStats() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val fakeCountResult = FirebaseFakeDataManager.countFakeProducts(firestore)
            val totalCountResult = FirebaseFakeDataManager.countAllProducts(firestore)

            fakeCountResult.onSuccess { fakeCount ->
                totalCountResult.onSuccess { totalCount ->
                    val realCount = totalCount - fakeCount

                    binding.tvFakeCount.text = "$fakeCount fake products"
                    binding.tvRealCount.text = "$realCount real products"
                    binding.tvTotalCount.text = "$totalCount total"

                    binding.tvStatus.text = "📊 Stats updated"
                }
            }

            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}