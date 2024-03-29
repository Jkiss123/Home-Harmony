package com.example.furniturecloudy.present.fragments.loginRegister

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.FragmentAccountOptionBinding
import com.example.furniturecloudy.databinding.FragmentIntroductionBinding


class AccountOptionFragment : Fragment() {
private  lateinit var binding: FragmentAccountOptionBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAccountOptionBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSignInOption.setOnClickListener {
            findNavController().navigate(R.id.action_accountOptionFragment_to_loginFragment)
        }
        binding.btnSignUpOption.setOnClickListener {
            findNavController().navigate(R.id.action_accountOptionFragment_to_registerFragment)
        }
    }

}