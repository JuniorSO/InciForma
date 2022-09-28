package com.example.inciforma

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ConfigActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        auth = Firebase.auth

        findViewById<TextView>(R.id.txtUserEmail).text = auth.currentUser!!.email

        findViewById<Button>(R.id.btnAutenticar).setOnClickListener {
            val edtSenha = findViewById<EditText>(R.id.edtSenha).text.toString()

            if (edtSenha.isEmpty()) {
                Toast.makeText(
                    baseContext, "Preencha o campo.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val credential = EmailAuthProvider
                    .getCredential(auth.currentUser!!.email.toString(), edtSenha)

                auth.currentUser!!.reauthenticate(credential)
                    .addOnCompleteListener {
                        findViewById<RelativeLayout>(R.id.ChangeEmail).visibility = View.VISIBLE
                        findViewById<RelativeLayout>(R.id.ChangeSenha).visibility = View.VISIBLE
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            baseContext, "Não foi possível autenticar.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        findViewById<Button>(R.id.btnNewEmail).setOnClickListener {
            val edtNewEmail = findViewById<EditText>(R.id.edtNewEmail).text.toString()

            if (edtNewEmail.isEmpty()) {
                Toast.makeText(
                    baseContext, "Preencha o campo de email.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                auth.currentUser!!.updateEmail(edtNewEmail)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                baseContext,
                                "Seu email foi atualizado, não esqueça de verificá-lo.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                baseContext, "Algo deu errado.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            baseContext,
                            "Não foi possível atualizar o email, tente novamente mais tarde.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        findViewById<Button>(R.id.btnNewSenha).setOnClickListener {
            val edtNewSenha = findViewById<EditText>(R.id.edtNewSenha).text.toString()

            if (edtNewSenha.isEmpty()) {
                Toast.makeText(
                    baseContext, "Preencha o campo de senha.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                auth.currentUser!!.updatePassword(edtNewSenha)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                baseContext, "Senha atualizada com sucesso.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                baseContext, "Não foi possível atualizar a senha.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            baseContext,
                            "Não foi possível atualizar a senha, tente novamente mais tarde.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}