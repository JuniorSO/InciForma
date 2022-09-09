package com.example.inciforma

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
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
        else {
            if (auth.currentUser!!.photoUrl == null) {
                val modal = BottomSheetDialog(this)
                modal.setContentView(R.layout.logged_bottom_sheet)

                modal.findViewById<TextView>(R.id.txtUserEmail)!!.text = auth.currentUser!!.email
                modal.findViewById<TextView>(R.id.txtUserUID)!!.text = auth.currentUser!!.uid

                modal.findViewById<Button>(R.id.btnLogout)!!.setOnClickListener {
                    auth.signOut()
                    Toast.makeText(baseContext, "Você saiu da conta.",
                        Toast.LENGTH_SHORT).show()
                    modal.dismiss()
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

                modal.show()
            }
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
                //TODO: verificar se o usuário está com o e-mail verificado.
                logStuff()
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