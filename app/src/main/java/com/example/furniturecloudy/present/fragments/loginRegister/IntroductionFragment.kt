package com.example.furniturecloudy.present.fragments.loginRegister

import android.content.Intent
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
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.FragmentIntroductionBinding
import com.example.furniturecloudy.model.viewmodel.IntroductionViewmodel
import com.example.furniturecloudy.model.viewmodel.IntroductionViewmodel.Companion.ACCOUNT_OPTION_FRAGMENT
import com.example.furniturecloudy.model.viewmodel.IntroductionViewmodel.Companion.SHOPPING_ACTIVITY
import com.example.furniturecloudy.present.ShoppingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class IntroductionFragment : Fragment() {
private lateinit var binding:FragmentIntroductionBinding
private val viewmodel : IntroductionViewmodel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentIntroductionBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnIntroducStart.setOnClickListener {
            findNavController().navigate(R.id.action_introductionFragment_to_accountOptionFragment)
            viewmodel.startButtonClicked()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewmodel.navigate.collect{
                    when(it){
                     SHOPPING_ACTIVITY -> {
                         Intent(requireActivity(), ShoppingActivity::class.java).also { intent ->
                             intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                             startActivity(intent)
                             }
                         }
                        ACCOUNT_OPTION_FRAGMENT ->{
                            findNavController().navigate(R.id.action_introductionFragment_to_accountOptionFragment)
                        }
                    }
                }
            }
        }
    }

}