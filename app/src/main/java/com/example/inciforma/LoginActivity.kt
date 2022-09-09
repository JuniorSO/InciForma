package com.example.inciforma

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient : GoogleSignInClient

    private var openActivity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
            result: ActivityResult ->

        if(result.resultCode == RESULT_OK) {
            val intent = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)

            try {
                val account = task.getResult(ApiException::class.java)
                signInWithGoogleActivity(account.idToken!!)
            }
            catch (exception: ApiException) {
                Toast.makeText(baseContext, "Não foi possível entrar, tente novamente.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithGoogle() {
        val intent = googleSignInClient.signInIntent
        openActivity.launch(intent)
    }

    private fun signInWithGoogleActivity(token: String) {
        val credential = GoogleAuthProvider.getCredential(token, null)

        auth.signInWithCredential(credential).addOnCompleteListener(this) {
                task: Task<AuthResult> ->
            if(task.isSuccessful) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
                Toast.makeText(baseContext, "Aproveite o nosso app!",
                    Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(baseContext, "Não foi possível entrar, tente novamente.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val txtBtnGoogle = findViewById<SignInButton>(R.id.btnGLogin).getChildAt(0) as TextView
        txtBtnGoogle.text = getString(R.string.txtGBtn)

        auth = Firebase.auth

        val clientId = "476836883143-nekic7famq3hov4j74o0i80f90j86a1a.apps.googleusercontent.com"
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .requestProfile()
            .requestId()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val currentUser = auth.currentUser
        if(currentUser != null){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<TextView>(R.id.txtLink).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val edtEmail = findViewById<EditText>(R.id.edtEmail).text.toString()
            val edtSenha = findViewById<EditText>(R.id.edtSenha).text.toString()

            auth.signInWithEmailAndPassword(edtEmail, edtSenha)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, make a toast with the signed-in user's information
                        Toast.makeText(baseContext, "Aproveite o nosso app!",
                            Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(baseContext, "Não foi possível entrar na conta.",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }

        findViewById<SignInButton>(R.id.btnGLogin).setOnClickListener{
            signInWithGoogle()
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}