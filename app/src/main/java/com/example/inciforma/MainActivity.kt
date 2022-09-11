package com.example.inciforma

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var auth: FirebaseAuth

    private fun showUserModal() {
        if (auth.currentUser == null) {
            val modal = BottomSheetDialog(this)
            modal.setContentView(R.layout.user_bottom_sheet)

            val txtCriar = modal.findViewById<TextView>(R.id.txtCriar)
            val txtEntrar = modal.findViewById<TextView>(R.id.txtEntrar)

            txtCriar!!.setOnClickListener {
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
                modal.dismiss()
                finish()
            }

            txtEntrar!!.setOnClickListener {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                modal.dismiss()
                finish()
            }

            modal.show()
        }
        else if (auth.currentUser!!.photoUrl == null) {
                val modal = BottomSheetDialog(this)
                modal.setContentView(R.layout.logged_bottom_sheet)

                if(!auth.currentUser!!.isEmailVerified){
                    val btnVerify = modal.findViewById<Button>(R.id.btnVerify)!!
                    btnVerify.visibility = View.VISIBLE
                    btnVerify.setOnClickListener {
                        auth.currentUser!!.sendEmailVerification()
                            .addOnCompleteListener { task ->
                                if(task.isSuccessful) {
                                    Toast.makeText(baseContext, "Confirme seu email e realize o login.",
                                        Toast.LENGTH_SHORT).show()
                                    auth.signOut()
                                    modal.dismiss()
                                }
                                else {
                                    Toast.makeText(baseContext, "Algo deu errado, certifique seu email.",
                                        Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }

                modal.findViewById<TextView>(R.id.txtUserEmail)!!.text = auth.currentUser!!.email
                modal.findViewById<TextView>(R.id.txtUserUID)!!.text = auth.currentUser!!.uid

                modal.findViewById<Button>(R.id.btnLogout)!!.setOnClickListener {
                    auth.signOut()
                    Toast.makeText(baseContext, "Você saiu da conta.",
                        Toast.LENGTH_SHORT).show()
                    modal.dismiss()
                }

                modal.findViewById<Button>(R.id.btnDltAccount)!!.setOnClickListener {
                    try {
                        auth.currentUser!!.delete()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(baseContext, "Conta excluída.",
                                        Toast.LENGTH_SHORT).show()
                                    modal.dismiss()
                                }
                                else {
                                    Toast.makeText(baseContext, "Por favor, faça login novamente para excluir essa conta.",
                                        Toast.LENGTH_SHORT).show()
                                    modal.dismiss()
                                    auth.signOut()
                                }
                                }
                    }
                    catch(e: FirebaseAuthRecentLoginRequiredException) {
                        Toast.makeText(baseContext, "Por favor, faça login novamente para excluir essa conta.",
                            Toast.LENGTH_SHORT).show()
                        modal.dismiss()
                        auth.signOut()
                    }
                }

                modal.show()
            }
            else {
                val modal = BottomSheetDialog(this)
                modal.setContentView(R.layout.glogged_bottom_sheet)

                modal.findViewById<TextView>(R.id.txtUserName)!!.text = auth.currentUser!!.displayName
                modal.findViewById<TextView>(R.id.txtUserEmail)!!.text = auth.currentUser!!.email
                modal.findViewById<TextView>(R.id.txtUserUID)!!.text = auth.currentUser!!.uid

                val uri: Uri = Uri.parse(auth.currentUser!!.photoUrl.toString())
                modal.findViewById<SimpleDraweeView>(R.id.pfpUser)!!.setImageURI(uri, null)

                modal.findViewById<Button>(R.id.btnLogout)!!.setOnClickListener {
                    auth.signOut()
                    Toast.makeText(baseContext, "Você saiu da conta.",
                        Toast.LENGTH_SHORT).show()
                    modal.dismiss()
                }

                modal.findViewById<Button>(R.id.btnDltAccount)!!.setOnClickListener {
                    try {
                        auth.currentUser!!.delete()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(baseContext, "Conta excluída.",
                                        Toast.LENGTH_SHORT).show()
                                    modal.dismiss()
                                }
                                else {
                                    Toast.makeText(baseContext, "Por favor, faça login novamente para excluir essa conta.",
                                        Toast.LENGTH_SHORT).show()
                                    modal.dismiss()
                                    auth.signOut()
                                    }
                                }
                    }
                    catch(e: FirebaseAuthRecentLoginRequiredException) {
                        Toast.makeText(baseContext, "Por favor, faça login novamente para excluir essa conta.",
                            Toast.LENGTH_SHORT).show()
                        modal.dismiss()
                        auth.signOut()
                    }
                }

                modal.show()
            }
        }

    private fun logStuff() {
        Log.i("ID DO USUÁRIO", auth.currentUser!!.displayName + " / " + auth.currentUser!!.uid + " / " + auth.currentUser  + " / " + auth.currentUser!!.email  + " / " + auth.currentUser!!.photoUrl)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Fresco.initialize(this)

        auth = Firebase.auth

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<Button>(R.id.btnAbout).setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btnUser).setOnClickListener {
            showUserModal()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        findViewById<Button>(R.id.btnInci).setOnClickListener {
            if(auth.currentUser != null) {
                if(auth.currentUser!!.isEmailVerified) {
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(-23.522191184043685, -46.47568209691291))
                            .title("ETEC Zona Leste")
                    )
                }
                else {
                    Toast.makeText(baseContext, "Verifique seu email.",
                        Toast.LENGTH_SHORT).show()
                }
            }
            else {
                Toast.makeText(baseContext, "Você precisa de uma conta para registrar incidentes.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}