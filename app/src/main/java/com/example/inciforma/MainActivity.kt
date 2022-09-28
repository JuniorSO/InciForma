package com.example.inciforma

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.SimpleDateFormat
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var auth: FirebaseAuth
    private lateinit var alert: AlertDialog
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder
    private var locationPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        geocoder = Geocoder(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(baseContext)

        getLocationPermission()

        Fresco.initialize(this)

        auth = Firebase.auth
        auth.setLanguageCode("pt")

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

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        var userLocation = LatLng(-23.522223117110297, -46.476093277668845)
        map = googleMap

        map.setPadding(0, 0, 0, 144)

        map.setMinZoomPreference(3.0f)
        map.setMaxZoomPreference(20.0f)

        val db = Firebase.firestore

        db.collection("pings")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {

                    val latitude = document.data["latitude"] as Double
                    val longitude = document.data["longitude"] as Double

                    googleMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(latitude, longitude))
                            .title(document.data["description"].toString())
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(baseContext, "Ocorreu um erro ao tentar ler o banco.",
                    Toast.LENGTH_SHORT).show()
            }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = LatLng((location.latitude), location.longitude)

                    map.isMyLocationEnabled = true
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15.0f))
                } else {
                    Toast.makeText(
                        baseContext, "Por favor, ative o GPS.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15.0f))

        findViewById<Button>(R.id.btnInci).setOnClickListener {
            if (!locationPermissionGranted) {
                getLocationPermission()
            }
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLocation = LatLng((location.latitude), location.longitude)
                        map.isMyLocationEnabled = true
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15.0f))

                        if (auth.currentUser != null) {
                            if (auth.currentUser!!.isEmailVerified) {
                                val build = AlertDialog.Builder(this)
                                val view = layoutInflater.inflate(R.layout.dialog_incident, null)

                                build.setView(view)

                                val spnType = view.findViewById<Spinner>(R.id.spnTipo)
                                ArrayAdapter.createFromResource(this, R.array.tipoInci,
                                    android.R.layout.simple_spinner_item).also { adapter ->
                                    // Specify the layout to use when the list of choices appears
                                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                    // Apply the adapter to the spinner
                                    spnType.adapter = adapter

                                view.findViewById<Button>(R.id.btnClose).setOnClickListener { alert.dismiss() }
                                view.findViewById<Button>(R.id.btnNovoInci).setOnClickListener {
                                    val edtLocal = view.findViewById<EditText>(R.id.edtLocal).text.toString()
                                    val edtDesc = view.findViewById<EditText>(R.id.edtDesc).text.toString()
                                    val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy, HH:mm:ss", Locale.ENGLISH)

                                    val type = spnType.selectedItem.toString()
                                    val time = simpleDateFormat.format(location.time)
                                    var locationLat = -23.52305446306669
                                    var locationLng = -46.47576908722295

                                    try {
                                        val addresses : List<Address> = geocoder.getFromLocationName(edtLocal, 1)
                                        val address = addresses[0]
                                        locationLat = address.latitude
                                        locationLng = address.longitude
                                        Log.i(TAG, address.toString())
                                    } catch (e: IOException) {
                                        Log.e(TAG, e.toString())
                                    }

                                    val ping = hashMapOf(
                                        "latitude" to locationLat,
                                        "longitude" to locationLng,
                                        "type" to type,
                                        "description" to edtDesc,
                                        "rate" to 0,
                                        "time" to time,
                                        "uid" to auth.currentUser!!.uid
                                    )

                                    db.collection("pings")
                                        .add(ping)
                                        .addOnSuccessListener { documentReference ->
                                            Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w(TAG, "Error adding document", e)
                                        }

                                    }
                                }

                                alert = build.create()
                                alert.show()
                                alert.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                            } else {
                                Toast.makeText(baseContext, "Verifique seu email antes de criar incidentes.",
                                    Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(baseContext, "Você precisa de uma conta ativa antes de criar incidentes.",
                                Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(
                            baseContext, "Por favor, ative o GPS.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

        }
    }

override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
) {
    locationPermissionGranted = false
    when (requestCode) {
        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

            // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                locationPermissionGranted = true
            }
            if (grantResults.isEmpty()) {
                Toast.makeText(
                    baseContext, "O aplicativo funciona melhor com a sua localização.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

private fun getLocationPermission() {
    if (ContextCompat.checkSelfPermission(
            this.applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        == PackageManager.PERMISSION_GRANTED
    ) {
        locationPermissionGranted = true
    } else {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
        )
    }
}

private fun showUserModal() {
    if (auth.currentUser == null) {
        val modal = BottomSheetDialog(this)
        modal.setContentView(R.layout.user_bottom_sheet)

        modal.findViewById<TextView>(R.id.txtCriar)!!.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            modal.dismiss()
            finish()
        }

        modal.findViewById<TextView>(R.id.txtEntrar)!!.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            modal.dismiss()
            finish()
        }

        modal.show()
        modal.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    } else if (auth.currentUser!!.photoUrl == null) {
        val modal = BottomSheetDialog(this)
        modal.setContentView(R.layout.logged_bottom_sheet)

        if (!auth.currentUser!!.isEmailVerified) {
            val btnVerify = modal.findViewById<Button>(R.id.btnVerify)!!
            btnVerify.visibility = View.VISIBLE
            btnVerify.setOnClickListener {
                auth.currentUser!!.sendEmailVerification()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                baseContext, "Confirme seu email e realize o login.",
                                Toast.LENGTH_SHORT
                            ).show()
                            auth.signOut()
                            modal.dismiss()
                        } else {
                            Toast.makeText(
                                baseContext, "Algo deu errado, certifique seu email.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            baseContext, "Não foi possível enviar, tente novamente mais tarde.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }

        modal.findViewById<TextView>(R.id.txtUserEmail)!!.text = auth.currentUser!!.email
        modal.findViewById<TextView>(R.id.txtUserUID)!!.text = auth.currentUser!!.uid

        modal.findViewById<Button>(R.id.btnConfig)!!.setOnClickListener {
            modal.dismiss()
            val intent = Intent(this, ConfigActivity::class.java)
            startActivity(intent)
            finish()
        }

        modal.findViewById<Button>(R.id.btnLogout)!!.setOnClickListener {
            auth.signOut()
            Toast.makeText(
                baseContext, "Você saiu da conta.",
                Toast.LENGTH_SHORT
            ).show()
            modal.dismiss()
        }

        modal.findViewById<Button>(R.id.btnDltAccount)!!.setOnClickListener {
            deleteAccount()
            modal.dismiss()
        }

        modal.show()
        modal.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    } else {
        val modal = BottomSheetDialog(this)
        modal.setContentView(R.layout.glogged_bottom_sheet)

        modal.findViewById<TextView>(R.id.txtUserName)!!.text = auth.currentUser!!.displayName
        modal.findViewById<TextView>(R.id.txtUserEmail)!!.text = auth.currentUser!!.email
        modal.findViewById<TextView>(R.id.txtUserUID)!!.text = auth.currentUser!!.uid

        val uri: Uri = Uri.parse(auth.currentUser!!.photoUrl.toString())
        modal.findViewById<SimpleDraweeView>(R.id.pfpUser)!!.setImageURI(uri, null)

        modal.findViewById<Button>(R.id.btnLogout)!!.setOnClickListener {
            auth.signOut()
            Toast.makeText(
                baseContext, "Você saiu da conta.",
                Toast.LENGTH_SHORT
            ).show()
            modal.dismiss()
        }

        modal.findViewById<Button>(R.id.btnDltAccount)!!.setOnClickListener {
            deleteGAccount()
            modal.dismiss()
        }

        modal.show()
        modal.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}

private fun deleteAccount() {
    val build = AlertDialog.Builder(this)
    val view = layoutInflater.inflate(R.layout.dialog_daccount, null)

    build.setView(view)

    view.findViewById<Button>(R.id.btnClose)!!.setOnClickListener { alert.dismiss() }
    view.findViewById<Button>(R.id.btnDltAccount)!!.setOnClickListener {
        val edtSenha = view.findViewById<EditText>(R.id.edtSenha).text.toString()

        if (edtSenha.isEmpty()) {
            Toast.makeText(
                baseContext, "Preencha o campo senha para excluir a conta.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val credential = EmailAuthProvider
                .getCredential(auth.currentUser!!.email.toString(), edtSenha)

            auth.currentUser!!.reauthenticate(credential)
                .addOnCompleteListener {
                    try {
                        auth.currentUser!!.delete()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        baseContext, "Conta excluída.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        baseContext,
                                        "Não foi possível excluir, faça login novamente para excluir essa conta.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    auth.signOut()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    baseContext,
                                    "Não foi possível excluir a conta, tente novamente mais tarde.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    } catch (e: FirebaseAuthRecentLoginRequiredException) {
                        Toast.makeText(
                            baseContext,
                            "Por favor, faça login novamente para excluir essa conta.",
                            Toast.LENGTH_SHORT
                        ).show()
                        auth.signOut()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        baseContext, "Não foi possível autenticar.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
        alert.dismiss()
    }

    alert = build.create()
    alert.show()
    alert.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
}

private fun deleteGAccount() {
    val build = AlertDialog.Builder(this)
    val view = layoutInflater.inflate(R.layout.dialog_dgaccount, null)

    build.setView(view)

    view.findViewById<Button>(R.id.btnClose)!!.setOnClickListener { alert.dismiss() }
    view.findViewById<Button>(R.id.btnDltAccount)!!.setOnClickListener {
        try {
            auth.currentUser!!.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            baseContext, "Conta excluída.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Por favor, faça login novamente para excluir essa conta.",
                            Toast.LENGTH_SHORT
                        ).show()
                        auth.signOut()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        baseContext,
                        "Não foi possível excluir a conta, tente novamente mais tarde.",
                        Toast.LENGTH_LONG
                    ).show()
                }
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Toast.makeText(
                baseContext, "Por favor, faça login novamente para excluir essa conta.",
                Toast.LENGTH_SHORT
            ).show()
            auth.signOut()
        }

        alert.dismiss()
    }

    alert = build.create()
    alert.show()
    alert.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
}

companion object {
    const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
}
}

