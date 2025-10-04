package com.example.furniturecloudy.present.fragments.shopping

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.FragmentProfileBinding
import com.example.furniturecloudy.model.viewmodel.ProfileViewmodel
import com.example.furniturecloudy.present.LoginRegisterActivity
import com.example.furniturecloudy.util.Resource
import com.example.furniturecloudy.util.showBottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val viewModel : ProfileViewmodel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.constraintProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_userAccountFragment)
        }

        binding.linearWishlist.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_wishlistFragment)
        }

        binding.linearAllOrders.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_ordersFragment)
        }

        binding.linearLogOut.setOnClickListener {
            viewModel.logOut()
            val intent = Intent(requireContext(),LoginRegisterActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.user.collectLatest {
                    when(it){
                        is Resource.Error -> {
                            binding.progressbarSettings.visibility = View.GONE
                        }
                        is Resource.Loading -> {
                            binding.progressbarSettings.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            binding.progressbarSettings.visibility = View.GONE
                            Glide.with(requireView()).load(it.data!!.imagePath).error(ColorDrawable(
                                Color.BLACK)).into(binding.imageUser)
                            binding.tvUserName.text = it.data.firstName
                        }
                        else -> Unit
                    }
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        showBottomNavigationView()
    }

}