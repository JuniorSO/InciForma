package com.example.inciforma

import android.content.ContentValues
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var alert: AlertDialog

    private var openActivity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->

        if (result.resultCode == RESULT_OK) {
            val intent = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)

            try {
                val account = task.getResult(ApiException::class.java)
                signInWithGoogleActivity(account.idToken!!)
            } catch (exception: ApiException) {
                Toast.makeText(
                    baseContext, "Não foi possível entrar, tente novamente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnGLogin = findViewById<SignInButton>(R.id.btnGLogin)
        val txtBtnGLogin = btnGLogin.getChildAt(0) as TextView
        txtBtnGLogin.text = getString(R.string.txtGBtn)

        val btnLogin = findViewById<Button>(R.id.btnLogin)

        auth = Firebase.auth
        db = Firebase.firestore

        val clientId = getString(R.string.webClientId)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .requestProfile()
            .requestId()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        if (auth.currentUser != null) {
            onBackPressed()
        }

        findViewById<TextView>(R.id.txtLink).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnLogin.setOnClickListener {
            btnLogin.isEnabled = false

            val edtEmail = findViewById<EditText>(R.id.edtEmail).text.toString()
            val edtSenha = findViewById<EditText>(R.id.edtSenha).text.toString()

            if (edtEmail.isEmpty() || edtSenha.isEmpty()) {
                Toast.makeText(
                    baseContext, "Preencha os campos.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                auth.signInWithEmailAndPassword(edtEmail, edtSenha)
                    .addOnSuccessListener(this) {
                        Toast.makeText(
                            baseContext, "Aproveite o nosso app!",
                            Toast.LENGTH_SHORT
                        ).show()

                        onBackPressed()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            baseContext,
                            "Não foi possível entrar na conta. Verifique as informações.",
                            Toast.LENGTH_SHORT
                        ).show()

                        btnLogin.isEnabled = true
                    }
            }
        }

        btnGLogin.setOnClickListener {
            signInWithGoogle()
        }

        findViewById<TextView>(R.id.txtEsquecido).setOnClickListener {
            val build = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.dialog_password, null)

            val btnEnviar = view.findViewById<Button>(R.id.btnEnviar)

            build.setView(view)

            view.findViewById<Button>(R.id.btnClose)!!.setOnClickListener { alert.dismiss() }

            btnEnviar.setOnClickListener {
                btnEnviar.isEnabled = false

                val edtEmail = view.findViewById<EditText>(R.id.edtEmail).text.toString()

                if (edtEmail.isEmpty()) {
                    Toast.makeText(
                        baseContext, "Preencha o campo.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    auth.sendPasswordResetEmail(edtEmail)
                        .addOnSuccessListener {
                            Toast.makeText(
                                baseContext, "Foi enviado o email para troca da senha.",
                                Toast.LENGTH_SHORT
                            ).show()

                            alert.dismiss()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                baseContext, "Não foi possível enviar, tente novamente.",
                                Toast.LENGTH_SHORT
                            ).show()

                            btnEnviar.isEnabled = true
                        }
                }
            }

            alert = build.create()
            alert.show()
            alert.window!!.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun signInWithGoogle() {
        val intent = googleSignInClient.signInIntent
        openActivity.launch(intent)
    }

    private fun signInWithGoogleActivity(token: String) {
        val credential = GoogleAuthProvider.getCredential(token, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        baseContext, "Aproveite o nosso app!",
                        Toast.LENGTH_SHORT
                    ).show()

                    val votes = hashMapOf(
                        "downVote" to arrayListOf(""),
                        "upVote" to arrayListOf(""),
                    )

                    db.collection("users").document(auth.currentUser!!.uid)
                        .set(votes)
                        .addOnSuccessListener {
                            Log.d(
                                ContentValues.TAG,
                                "DocumentSnapshot successfully written!"
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.w(
                                ContentValues.TAG,
                                "Error writing document",
                                e
                            )
                        }

                    onBackPressed()
                } else {
                    Toast.makeText(
                        baseContext, "Não foi possível entrar, tente novamente.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}