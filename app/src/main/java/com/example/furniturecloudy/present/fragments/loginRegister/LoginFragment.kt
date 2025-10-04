package com.example.furniturecloudy.present.fragments.loginRegister

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.FragmentLoginBinding
import com.example.furniturecloudy.model.viewmodel.LoginViewmodel
import com.example.furniturecloudy.present.ShoppingActivity
import com.example.furniturecloudy.util.Resource
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
@AndroidEntryPoint
class LoginFragment : Fragment() {
    private lateinit var binding:FragmentLoginBinding
    private val viewmodel:LoginViewmodel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Register for activity result
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            btnLoginDangnhap.setOnClickListener {
                    val email = edtLoginTaiKhoan.text.toString().trim()
                    val password =edtLoginMatKhau.text.toString()
                viewmodel.loginAccount(email,password)
            }
            txtvSlogan2Login.setOnClickListener {
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
            }
            txtQuenMatKhauLogin.setOnClickListener {
                findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
            }
            btnLoginGoogle.setOnClickListener {
                signInWithGoogle()
            }
            btnLoginFacebook.setOnClickListener {
                Toast.makeText(requireContext(), "Facebook đăng nhập sẽ được triển khai sau", Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewmodel.login.collect{
                    when(it){
                        is Resource.Error -> {
                            binding.btnLoginDangnhap.revertAnimation()
                            Toast.makeText(requireContext(),"Đăng nhập không thành công. Vui lòng kiểm tra email và mật khẩu",Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {
                            binding.btnLoginDangnhap.startAnimation()
                        }
                        is Resource.Success -> {
                            binding.btnLoginDangnhap.revertAnimation()
                            Intent(requireActivity(),ShoppingActivity::class.java).also { intent ->
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                            }
                        }
                        else -> Unit
                    }
                }
            }
            }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewmodel.signInWithGoogle(idToken)
            } else {
                Toast.makeText(requireContext(), "Không thể lấy thông tin Google. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.w("LoginFragment", "Google sign in failed", e)
            Toast.makeText(requireContext(), "Đăng nhập Google thất bại. Vui lòng thử lại", Toast.LENGTH_SHORT).show()
        }
    }
}