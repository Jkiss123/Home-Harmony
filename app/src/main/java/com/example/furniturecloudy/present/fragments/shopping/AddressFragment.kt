package com.example.furniturecloudy.present.fragments.shopping

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.furniturecloudy.R
import com.example.furniturecloudy.data.Address
import com.example.furniturecloudy.databinding.FragmentAddressBinding
import com.example.furniturecloudy.model.viewmodel.AddressViewmodel
import com.example.furniturecloudy.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddressFragment : Fragment() {
    private lateinit var binding: FragmentAddressBinding
    private val viewmodel: AddressViewmodel by viewModels()
    private var editingAddress: Address? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Nhận address từ arguments nếu có
        editingAddress = arguments?.getParcelable("address")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddressBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        setupClickListeners()
    }

    private fun setupUI() {
        // Hiển thị dữ liệu địa chỉ nếu tồn tại (edit mode)
        editingAddress?.let {
            populateAddressFields(it)
            // Cập nhật UI cho edit mode
            binding.tvPaymentMethods.text = "Chỉnh sửa địa chỉ"
            binding.buttonSave.text = "Cập nhật"
            binding.buttonDelelte.visibility = View.VISIBLE
        } ?: run {
            // Add mode
            binding.tvPaymentMethods.text = "Thêm địa chỉ"
            binding.buttonSave.text = "Lưu"
            binding.buttonDelelte.visibility = View.GONE
        }
    }

    private fun setupObservers() {
        // Observer cho add address
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewmodel.addNewAddress.collectLatest {
                    when (it) {
                        is Resource.Error -> {
                            binding.progressbarAddress.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                "Không thể thêm địa chỉ. Vui lòng thử lại",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is Resource.Loading -> {
                            binding.progressbarAddress.visibility = View.VISIBLE
                        }

                        is Resource.Success -> {
                            binding.progressbarAddress.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                "Thêm địa chỉ thành công",
                                Toast.LENGTH_SHORT
                            ).show()
                            clearForm()
                            findNavController().navigateUp()
                        }

                        else -> Unit
                    }
                }
            }
        }

        // Observer cho update address
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewmodel.updateAddress.collectLatest {
                    when (it) {
                        is Resource.Error -> {
                            binding.progressbarAddress.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                "Không thể cập nhật địa chỉ. Vui lòng thử lại",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is Resource.Loading -> {
                            binding.progressbarAddress.visibility = View.VISIBLE
                        }

                        is Resource.Success -> {
                            binding.progressbarAddress.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                "Cập nhật địa chỉ thành công",
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigateUp()
                        }

                        else -> Unit
                    }
                }
            }
        }

        // Observer cho delete address
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewmodel.deleteAddress.collectLatest {
                    when (it) {
                        is Resource.Error -> {
                            binding.progressbarAddress.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                "Không thể xóa địa chỉ. Vui lòng thử lại",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is Resource.Loading -> {
                            binding.progressbarAddress.visibility = View.VISIBLE
                        }

                        is Resource.Success -> {
                            binding.progressbarAddress.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                "Đã xóa địa chỉ thành công",
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigateUp()
                        }

                        else -> Unit
                    }
                }
            }
        }

        // Observer cho error
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewmodel.error.collectLatest {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            buttonSave.setOnClickListener {
                val addressfull = edAddressTitle.text.toString()
                val name = edFullName.text.toString()
                val phone = edPhone.text.toString()
                val city = edCity.text.toString()
                val wards = edState.text.toString()
                val district = edStreet.text.toString()
                val address = Address(name, phone, wards, district, city, addressfull)

                if (editingAddress != null) {
                    // Cập nhật địa chỉ (giữ lại id nếu có)
                    val updatedAddress = address.copy(id = editingAddress!!.id)
                    viewmodel.updateAddress(updatedAddress)
                } else {
                    // Thêm mới địa chỉ
                    viewmodel.addAddress(address)
                }
            }

            imageAddressClose.setOnClickListener {
                findNavController().navigateUp()
            }

            buttonDelelte.setOnClickListener {
                editingAddress?.let { addressToDelete ->
                    showDeleteConfirmationDialog(addressToDelete.id)
                }
            }
        }
    }

    private fun populateAddressFields(address: Address) {
        binding.edAddressTitle.setText(address.addressFull)
        binding.edFullName.setText(address.fullName)
        binding.edPhone.setText(address.phone)
        binding.edCity.setText(address.city)
        binding.edState.setText(address.wards)
        binding.edStreet.setText(address.district)
    }

    private fun clearForm() {
        binding.edAddressTitle.text?.clear()
        binding.edFullName.text?.clear()
        binding.edPhone.text?.clear()
        binding.edCity.text?.clear()
        binding.edState.text?.clear()
        binding.edStreet.text?.clear()
    }

    private fun showDeleteConfirmationDialog(addressId: String) {
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa địa chỉ này?")
            .setPositiveButton("Xóa") { dialogInterface, _ ->
                viewmodel.deleteAddress(addressId)
                dialogInterface.dismiss()
            }
            .setNegativeButton("Hủy") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()
        dialog.show()
    }
}