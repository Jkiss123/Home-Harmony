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
    private lateinit var binding:FragmentAddressBinding
    private val viewmodel : AddressViewmodel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddressBinding.inflate(layoutInflater)
        return binding.root
    }
    // collect button add
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewmodel.addNewAddress.collectLatest {
                    when(it){
                        is Resource.Error -> {
                            binding.progressbarAddress.visibility = View.GONE
                            Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {
                            binding.progressbarAddress.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            binding.progressbarAddress.visibility = View.GONE
                            findNavController().navigateUp()
                        }
                        else -> Unit
                    }
                }
            }
        }

        //collect error
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewmodel.error.collectLatest {
                    Toast.makeText(requireContext(),it,Toast.LENGTH_SHORT).show()
                }
            }}

        binding.apply {
            buttonSave.setOnClickListener {
                val addressfull = edAddressTitle.text.toString()
                val name = edFullName.text.toString()
                val phone = edPhone.text.toString()
                val city = edCity.text.toString()
                val wards = edState.text.toString()
                val district = edStreet.text.toString()
                val address = Address(name,phone,wards,district,city,addressfull)
                viewmodel.addAddress(address)
            }
        }
    }



}