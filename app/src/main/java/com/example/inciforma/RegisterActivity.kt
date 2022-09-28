package com.example.inciforma

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        if(auth.currentUser != null){
            backInMap()
        }

        findViewById<TextView>(R.id.txtLink).setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            val edtEmail = findViewById<EditText>(R.id.edtEmail).text.toString()
            val edtSenha = findViewById<EditText>(R.id.edtSenha).text.toString()

            if(edtEmail.isEmpty() || edtSenha.isEmpty()) {
                Toast.makeText(baseContext, "Preencha os campos.",
                    Toast.LENGTH_SHORT).show()
            } else if(edtSenha.length < 6) {
                Toast.makeText(baseContext, "Senha muito curta.",
                    Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword(edtEmail, edtSenha)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            auth.currentUser!!.sendEmailVerification()
                                .addOnCompleteListener { tasc ->
                                    if (tasc.isSuccessful) {
                                        Toast.makeText(
                                            baseContext, "Email de verificação enviado, confirme seu email e realize o login.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        auth.signOut()
                                    } else {
                                        Toast.makeText(
                                            baseContext, "Não foi possível enviar o email de verificação.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(baseContext, "Não foi possível enviar o email de verificação, tente novamente mais tarde.",
                                        Toast.LENGTH_LONG).show()
                                }

                            backInMap()
                        } else {
                            Toast.makeText(
                                baseContext, "Não foi possível criar a conta.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
    }

    override fun onBackPressed() {
        backInMap()
    }

    private fun backInMap() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}