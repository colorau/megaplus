package com.lotteriasmais.megaplus.login

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.lotteriasmais.megaplus.R
import com.lotteriasmais.megaplus.databinding.LoginFragmentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()
    private var _binding: LoginFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LoginFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        observeEventsFromVM()
    }

    private fun setListeners() {
        binding.run{
            textViewRegister.setOnClickListener {
                progressBarLogin.visibility= View.VISIBLE
                val action = LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
                findNavController().navigate(action)
            }
            buttonLogin.setOnClickListener {
                val email = editTextEmail.text.toString().trim()
                val password = editTextPassword.text.toString().trim()
                viewModel.checkUser(email, password)
            }
            textViewForgotPassword.setOnClickListener {
                recoveryPassword()
            }
            signInBtn.setOnClickListener {
                progressBarLogin.visibility = View.VISIBLE
                signIn()
            }
            textViewSendMessage.setOnClickListener {
                viewModel.sendEmailVerificationMessage()
            }
        }
    }

    private fun observeEventsFromVM() {
        viewModel.fragmentEvent.asLiveData().observe(viewLifecycleOwner) { event ->
            when (event) {
                LoginViewModel.FragmentEvents.Processing -> Unit
                LoginViewModel.FragmentEvents.ShowEmailEmpty -> {
                    binding.editTextEmail.error= resources.getString(R.string.emailIsRequired)
                    binding.editTextEmail.requestFocus()
                }
                LoginViewModel.FragmentEvents.ShowEmailNotMatches -> {
                    binding.editTextEmail.error= resources.getString(R.string.email_Not_matches)
                    binding.editTextEmail.requestFocus()
                }
                LoginViewModel.FragmentEvents.ShowPasswordEmpty -> {
                    binding.editTextPassword.error= resources.getString(R.string.passwordIsRequired)
                    binding.editTextPassword.requestFocus()
                }
                LoginViewModel.FragmentEvents.ShowPasswordLess6Digits -> {
                    binding.editTextPassword.error= resources.getString(R.string.min_password_digits)
                    binding.editTextPassword.requestFocus()
                }
                LoginViewModel.FragmentEvents.FailureToSendLinkToEmail -> {
                    Toast.makeText(requireContext(), resources.getString(R.string.error_send_link), Toast.LENGTH_LONG).show()
                }
                LoginViewModel.FragmentEvents.ShowSendLinkToEmail -> {
                    Toast.makeText(requireContext(), resources.getString(R.string.link_send_your_email), Toast.LENGTH_LONG).show()
                }
                LoginViewModel.FragmentEvents.NavigateToSplashScreen -> {
                    val action = LoginFragmentDirections.actionLoginFragmentToFirstLaunchFragment()
                    findNavController().navigate(action)
                }
                LoginViewModel.FragmentEvents.NavigateToWellcomeScreen -> {
                    binding.progressBarLogin.visibility = View.GONE
                    val action = LoginFragmentDirections.actionLoginFragmentToWellcomeFragment()
                    findNavController().navigate(action)
                }
                LoginViewModel.FragmentEvents.NavigateToOnBoardingScreen -> {
                    val action = LoginFragmentDirections.actionLoginFragmentToOnBoardingFragment()
                    findNavController().navigate(action)
                }
            }
        }
    }

    private fun signIn() {
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        mGoogleSignInClient.signInIntent.also {
            startActivity.launch(it)
        }
    }

    private var startActivity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Log.d("Login", "it.resultCode= ${it.resultCode} RESULT_OK=$RESULT_OK it.data ${it.data.toString()}")
        if (it.resultCode == RESULT_OK) {
            val intent = it.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.firebaseAuthWithGoogle(account.idToken!!)
                binding.progressBarLogin.visibility = View.GONE
            } catch (e: ApiException) {
                Toast.makeText(requireContext(),"Erro da API obter conta do Google", Toast.LENGTH_LONG).show()
            }
        }else{
            Toast.makeText(requireContext(),"Erro RESULT CODE", Toast.LENGTH_LONG).show()
        }
    }

    private fun recoveryPassword(){
        val recoveryEmail = EditText(requireContext())
        val alertDialogRecoveryPassword = AlertDialog.Builder(requireContext())
        alertDialogRecoveryPassword.setTitle(resources.getString(R.string.password_recovery))
        alertDialogRecoveryPassword.setMessage(resources.getString(R.string.email_recovery))
        alertDialogRecoveryPassword.setView(recoveryEmail)
        alertDialogRecoveryPassword.setPositiveButton(
            resources.getString(R.string.yes)
        ) { _, _ ->
            val emailText = recoveryEmail.text.toString()
            if (emailText.isNotEmpty()) {
                viewModel.checkForgotPassword(emailText)
            } else {
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.enter_your_email),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        alertDialogRecoveryPassword.setNegativeButton(resources.getString(R.string.no)
        ) { _, _ ->

        }
        alertDialogRecoveryPassword.create().show()
    }

    override fun onResume() {
        super.onResume()
        binding.progressBarLogin.visibility= View.GONE
        viewModel.checkSplashShow()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}