package com.example.furniturecloudy.present.fragments.loginRegister

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withStarted
import androidx.navigation.fragment.findNavController
import com.example.furniturecloudy.R
import com.example.furniturecloudy.data.User
import com.example.furniturecloudy.databinding.FragmentRegisterBinding
import com.example.furniturecloudy.model.viewmodel.RegisterViewmodel
import com.example.furniturecloudy.util.RegisterValidation
import com.example.furniturecloudy.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val TAG = "RegisterFragment"
@AndroidEntryPoint
class RegisterFragment : Fragment() {
   private lateinit var binding:FragmentRegisterBinding
    private val viewmodel:RegisterViewmodel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRegisterBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    binding.btnRegisterDangky.setOnClickListener {
        val user = User(binding.edtRegisterFirstName.text.toString().trim(),
            binding.edtRegisterLastName.text.toString().trim(),
            binding.edtRegisterEmail.text.toString().trim(),"")
        val password = binding.edtRegisterPassword.text.toString()
        viewmodel.createAccountWithEmailAndPassword(user,password)
    }

        binding.txtvRegisterSlogan2n.setOnClickListener {
        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
        //function
        checkButtonState()
        checkValidation()
    }
    
    private fun checkButtonState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewmodel.register.collect() {
                    when (it) {
                        is Resource.Error -> {
                            Log.e(TAG, it.message.toString())
                            binding.btnRegisterDangky.revertAnimation()
                            Toast.makeText(context, "Đăng ký không thành công. Vui lòng thử lại", Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {
                            binding.btnRegisterDangky.startAnimation()
                        }
                        is Resource.Success -> {
                            Log.d("Success", it.data.toString())
                            binding.btnRegisterDangky.revertAnimation()
                            Toast.makeText(context, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    fun checkValidation(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewmodel.validation.collect{validation ->
                    //validation.password dang tra ve la success ?
                    if (validation.password is RegisterValidation.Failed) {
                        withContext(Dispatchers.Main) {
                            binding.edtRegisterPassword.apply {
                                requestFocus()
                                error = validation.password.message
                            }
                        }
                    }

                    if(validation.email is RegisterValidation.Failed){
                        withContext(Dispatchers.Main){
                            binding.edtRegisterEmail.apply {
                                requestFocus()
                                error = validation.email.message
                            }
                        }
                    }
                }
            }
        }
    }


}