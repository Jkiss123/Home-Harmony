package com.example.furniturecloudy.present.fragments.setting

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.furniturecloudy.data.User
import com.example.furniturecloudy.databinding.FragmentAccessoryBinding
import com.example.furniturecloudy.databinding.FragmentUserAccountBinding
import com.example.furniturecloudy.model.viewmodel.UserAccountViewmodel
import com.example.furniturecloudy.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
@AndroidEntryPoint
class UserAccountFragment : Fragment() {
    private lateinit var binding: FragmentUserAccountBinding
    private val viewModel: UserAccountViewmodel by viewModels()
    private var imageUri: Uri? = null
    private lateinit var imageActivityResultLauncher : ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            imageUri = it.data?.data
            Glide.with(this).load(imageUri).into(binding.imageUser)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUserAccountBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.user.collect{
                    when(it){
                        is Resource.Error -> {
                            Toast.makeText(requireContext(),it.message.toString(), Toast.LENGTH_SHORT).show()
                            binding.progressbarAccount.visibility = View.GONE

                        }
                        is Resource.Loading -> {showUserLoading()}
                        is Resource.Success -> {

                            hideUserLoading()
                            showUserData(it.data!!)

                        }
                        else -> Unit
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.updateInfor.collectLatest {
                    when(it){
                        is Resource.Error -> {
                            binding.buttonSave.stopAnimation()
                            Toast.makeText(requireContext(),it.message.toString(), Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {binding.buttonSave.startAnimation()}
                        is Resource.Success -> {
                            binding.buttonSave.revertAnimation()
                            findNavController().navigateUp()
                            Toast.makeText(requireContext(),"LUU xong roi", Toast.LENGTH_LONG).show()
                        }
                        else -> Unit
                    }
                }
            }
        }

        binding.buttonSave.setOnClickListener {
            binding.apply {
                val firstName = edFirstName.text.toString().trim()
                val lastName = edLastName.text.toString().toString()
                val email = edEmail.text.toString()
                val user = User(firstName,lastName,email,"")
                viewModel.updateUser(user,imageUri)
            }
        }

        binding.imageEdit.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            imageActivityResultLauncher.launch(intent)
        }




    }

    private fun showUserData(data: User) {
        binding.apply {
            Glide.with(this@UserAccountFragment).load(data.imagePath).error(ColorDrawable(Color.BLACK)).into(imageUser)
            edFirstName.setText(data.firstName)
            edLastName.setText(data.lastName)
            edEmail.setText(data.email)

        }
    }

    private fun hideUserLoading() {
        binding.apply {
            progressbarAccount.visibility = View.GONE
            imageUser.visibility = View.VISIBLE
            imageEdit.visibility = View.VISIBLE
            imageCloseUserAccount.visibility = View.VISIBLE
            edEmail.visibility = View.VISIBLE
            edFirstName.visibility = View.VISIBLE
            edLastName.visibility = View.VISIBLE
            tvUpdatePassword.visibility = View.VISIBLE
        }
    }

    private fun showUserLoading() {
        binding.apply {
            progressbarAccount.visibility = View.VISIBLE
            imageUser.visibility = View.INVISIBLE
            imageEdit.visibility = View.INVISIBLE
            imageCloseUserAccount.visibility = View.INVISIBLE
            edEmail.visibility = View.INVISIBLE
            edFirstName.visibility = View.INVISIBLE
            edLastName.visibility = View.INVISIBLE
            tvUpdatePassword.visibility = View.INVISIBLE
        }
    }


}