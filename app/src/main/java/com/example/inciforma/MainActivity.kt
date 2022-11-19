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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.IOException
import java.lang.NullPointerException
import java.util.*
import kotlin.collections.ArrayList

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

        map.setPadding(0, 128, 0, 144)

        map.setMinZoomPreference(3.0f)
        map.setMaxZoomPreference(20.0f)

        val db = Firebase.firestore

        db.collection("pings")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {

                    val latitude = document.data["latitude"] as Double
                    val longitude = document.data["longitude"] as Double

                    if (document.data["type"] == "Acidente Veicular") {
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(latitude, longitude))
                                .title(document.id)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        )
                    } else if (document.data["type"] == "Morte") {
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(latitude, longitude))
                                .title(document.id)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )
                    } else if (document.data["type"] == "Roubo") {
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(latitude, longitude))
                                .title(document.id)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        )
                    } else if (document.data["type"] == "Explosão") {
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(latitude, longitude))
                                .title(document.id)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                        )
                    } else if (document.data["type"] == "Tiroteio") {
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(latitude, longitude))
                                .title(document.id)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                        )
                    } else if (document.data["type"] == "Agressão") {
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(latitude, longitude))
                                .title(document.id)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                        )
                    } else {
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(latitude, longitude))
                                .title(document.id)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        )
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    baseContext, "Ocorreu um erro ao tentar ler o banco.",
                    Toast.LENGTH_SHORT
                ).show()
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

        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val edtSearch = findViewById<EditText>(R.id.edtSearch).text.toString()

            if (edtSearch.isEmpty()) {
                Toast.makeText(
                    baseContext, "Preencha o campo para pesquisar",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                try {
                    val addresses: List<Address> =
                        geocoder.getFromLocationName(
                            edtSearch, 1
                        )
                    val address = addresses[0]
                    val locationLat = address.latitude
                    val locationLng = address.longitude

                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                locationLat,
                                locationLng
                            ), 19.0f
                        )
                    )
                } catch (e: IOException) {
                    Toast.makeText(
                        baseContext,
                        "Ocorreu um erro ao tentar procurar este endereço.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

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
                                ArrayAdapter.createFromResource(
                                    this, R.array.tipoInci,
                                    android.R.layout.simple_spinner_item
                                ).also { adapter ->
                                    // Specify the layout to use when the list of choices appears
                                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                    // Apply the adapter to the spinner
                                    spnType.adapter = adapter

                                    view.findViewById<Button>(R.id.btnClose)
                                        .setOnClickListener { alert.dismiss() }
                                    view.findViewById<Button>(R.id.btnNovoInci).setOnClickListener {
                                        val edtLocal =
                                            view.findViewById<EditText>(R.id.edtLocal).text.toString()
                                        val edtBairro =
                                            view.findViewById<EditText>(R.id.edtBairro).text.toString()
                                        val edtDesc =
                                            view.findViewById<EditText>(R.id.edtDesc).text.toString()
                                        val simpleDateFormat =
                                            SimpleDateFormat("HH:mm:ss, dd/MM/yyyy", Locale.ENGLISH)

                                        if (edtLocal.isEmpty() || edtDesc.isEmpty() || edtBairro.isEmpty()) {
                                            Toast.makeText(
                                                baseContext, "Preencha todos os campos.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {

                                            val type = spnType.selectedItem.toString()
                                            val time = simpleDateFormat.format(location.time)
                                            var locationLat = -23.52305446306669
                                            var locationLng = -46.47576908722295

                                            try {
                                                val addresses: List<Address> =
                                                    geocoder.getFromLocationName(
                                                        "$edtLocal - $edtBairro", 1
                                                    )
                                                val address = addresses[0]
                                                locationLat = address.latitude
                                                locationLng = address.longitude
                                            } catch (e: IOException) {
                                                Toast.makeText(
                                                    baseContext,
                                                    "Ocorreu um erro ao tentar procurar este endereço.",
                                                    Toast.LENGTH_LONG
                                                ).show()
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
                                                .addOnSuccessListener { document ->
                                                    Toast.makeText(
                                                        baseContext, "Marcador registrado.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    map.addMarker(
                                                        MarkerOptions()
                                                            .position(
                                                                LatLng(
                                                                    locationLat,
                                                                    locationLng
                                                                )
                                                            )
                                                            .title(document.id)
                                                            .icon(
                                                                BitmapDescriptorFactory.defaultMarker(
                                                                    BitmapDescriptorFactory.HUE_CYAN
                                                                )
                                                            )
                                                    )
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(
                                                        baseContext,
                                                        "Não foi possível registrar o marcador. Tente novamente mais tarde.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }

                                            map.moveCamera(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(
                                                        locationLat,
                                                        locationLng
                                                    ), 15.0f
                                                )
                                            )
                                        }

                                        alert.dismiss()
                                    }
                                }

                                alert = build.create()
                                alert.show()
                                alert.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                            } else {
                                Toast.makeText(
                                    baseContext, "Verifique seu email antes de criar incidentes.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                baseContext,
                                "Você precisa de uma conta ativa antes de criar incidentes.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            baseContext, "Por favor, ative o GPS.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener {
                    if (auth.currentUser != null) {
                        if (auth.currentUser!!.isEmailVerified) {
                            val build = AlertDialog.Builder(this)
                            val view = layoutInflater.inflate(R.layout.dialog_incident, null)

                            build.setView(view)

                            val spnType = view.findViewById<Spinner>(R.id.spnTipo)
                            ArrayAdapter.createFromResource(
                                this, R.array.tipoInci,
                                android.R.layout.simple_spinner_item
                            ).also { adapter ->
                                // Specify the layout to use when the list of choices appears
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                // Apply the adapter to the spinner
                                spnType.adapter = adapter

                                view.findViewById<Button>(R.id.btnClose)
                                    .setOnClickListener { alert.dismiss() }
                                view.findViewById<Button>(R.id.btnNovoInci).setOnClickListener {
                                    val edtLocal =
                                        view.findViewById<EditText>(R.id.edtLocal).text.toString()
                                    val edtBairro =
                                        view.findViewById<EditText>(R.id.edtBairro).text.toString()
                                    val edtDesc =
                                        view.findViewById<EditText>(R.id.edtDesc).text.toString()
                                    val simpleDateFormat =
                                        SimpleDateFormat("HH:mm:ss, dd/MM/yyyy", Locale.ENGLISH)

                                    if (edtLocal.isEmpty() || edtDesc.isEmpty() || edtBairro.isEmpty()) {
                                        Toast.makeText(
                                            baseContext, "Preencha todos os campos.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {

                                        val type = spnType.selectedItem.toString()
                                        val time = simpleDateFormat.format(Date())
                                        var locationLat = -23.52305446306669
                                        var locationLng = -46.47576908722295

                                        try {
                                            val addresses: List<Address> =
                                                geocoder.getFromLocationName(
                                                    "$edtLocal - $edtBairro",
                                                    1
                                                )
                                            val address = addresses[0]
                                            locationLat = address.latitude
                                            locationLng = address.longitude
                                        } catch (e: IOException) {
                                            Toast.makeText(
                                                baseContext,
                                                "Ocorreu um erro ao tentar procurar este endereço.",
                                                Toast.LENGTH_LONG
                                            ).show()
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
                                            .addOnSuccessListener { document ->
                                                Toast.makeText(
                                                    baseContext, "Marcador registrado.",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                map.addMarker(
                                                    MarkerOptions()
                                                        .position(LatLng(locationLat, locationLng))
                                                        .title(document.id)
                                                        .icon(
                                                            BitmapDescriptorFactory.defaultMarker(
                                                                BitmapDescriptorFactory.HUE_CYAN
                                                            )
                                                        )
                                                )
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(
                                                    baseContext,
                                                    "Não foi possível registrar o marcador. Tente novamente mais tarde.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }

                                        map.moveCamera(
                                            CameraUpdateFactory.newLatLngZoom(
                                                LatLng(
                                                    locationLat,
                                                    locationLng
                                                ), 15.0f
                                            )
                                        )
                                    }

                                    alert.dismiss()
                                }
                            }

                            alert = build.create()
                            alert.show()
                            alert.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                        } else {
                            Toast.makeText(
                                baseContext, "Verifique seu email antes de criar incidentes.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Você precisa de uma conta ativa antes de criar incidentes.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

        }

        map.setOnMarkerClickListener { marker ->
            val build = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.dialog_marker, null)

            build.setView(view)

            val id = marker.title.toString()
            var upVoted = false
            var downVoted = false

            val btnDownVote = view.findViewById<Button>(R.id.btnDownVote)
            val btnUpVote = view.findViewById<Button>(R.id.btnUpVote)

            view.findViewById<Button>(R.id.btnClose).setOnClickListener { alert.dismiss() }
            view.findViewById<Button>(R.id.btnDltInci).setOnClickListener {
                db.collection("pings").document(id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(
                            baseContext, "Incidente excluído.",
                            Toast.LENGTH_SHORT
                        ).show()

                        marker.remove()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            baseContext, "Não foi possível excluir o incidente.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                alert.dismiss()
            }
            btnDownVote.setOnClickListener {
                btnDownVote.isEnabled = false

                if (!downVoted) {
                    if(!upVoted) {
                    db.collection("pings").document(id)
                        .update("rate", FieldValue.increment(-1))

                    db.collection("users").document(auth.currentUser!!.uid)
                        .update("downVote", FieldValue.arrayUnion(id))

                    db.collection("users").document(auth.currentUser!!.uid)
                        .update("upVote", FieldValue.arrayRemove(id))

                    var rateAlt = view.findViewById<TextView>(R.id.inciRate).text.toString().toInt()
                    rateAlt -= 1
                    view.findViewById<TextView>(R.id.inciRate).text = rateAlt.toString()

                    btnDownVote.backgroundTintList = getColorStateList(R.color.btnBackground)
                    } else if (upVoted) {
                        db.collection("pings").document(id)
                            .update("rate", FieldValue.increment(-2))

                        db.collection("users").document(auth.currentUser!!.uid)
                            .update("downVote", FieldValue.arrayUnion(id))

                        db.collection("users").document(auth.currentUser!!.uid)
                            .update("upVote", FieldValue.arrayRemove(id))

                        var rateAlt = view.findViewById<TextView>(R.id.inciRate).text.toString().toInt()
                        rateAlt -= 2
                        view.findViewById<TextView>(R.id.inciRate).text = rateAlt.toString()

                        btnUpVote.backgroundTintList = getColorStateList(R.color.transparent)
                        btnDownVote.backgroundTintList = getColorStateList(R.color.btnBackground)

                        upVoted = false
                    }

                    downVoted = true
                } else if(downVoted) {
                    db.collection("pings").document(id)
                        .update("rate", FieldValue.increment(1))

                    db.collection("users").document(auth.currentUser!!.uid)
                        .update("downVote", FieldValue.arrayRemove(id))

                    var rateAlt = view.findViewById<TextView>(R.id.inciRate).text.toString().toInt()
                    rateAlt += 1
                    view.findViewById<TextView>(R.id.inciRate).text = rateAlt.toString()

                    btnDownVote.backgroundTintList = getColorStateList(R.color.transparent)
                    downVoted = false
                }

                btnDownVote.isEnabled = true
            }
            btnUpVote.setOnClickListener {
                btnUpVote.isEnabled = false

                if (!upVoted) {
                    if(!downVoted) {
                        db.collection("pings").document(id)
                            .update("rate", FieldValue.increment(1))

                        db.collection("users").document(auth.currentUser!!.uid)
                            .update("upVote", FieldValue.arrayUnion(id))

                        db.collection("users").document(auth.currentUser!!.uid)
                            .update("downVote", FieldValue.arrayRemove(id))

                        var rateAlt =
                            view.findViewById<TextView>(R.id.inciRate).text.toString().toInt()
                        rateAlt += 1
                        view.findViewById<TextView>(R.id.inciRate).text = rateAlt.toString()

                        btnUpVote.backgroundTintList = getColorStateList(R.color.btnBackground)
                    } else if (downVoted) {
                        db.collection("pings").document(id)
                            .update("rate", FieldValue.increment(2))

                        db.collection("users").document(auth.currentUser!!.uid)
                            .update("upVote", FieldValue.arrayUnion(id))

                        db.collection("users").document(auth.currentUser!!.uid)
                            .update("downVote", FieldValue.arrayRemove(id))

                        var rateAlt =
                            view.findViewById<TextView>(R.id.inciRate).text.toString().toInt()
                        rateAlt += 2
                        view.findViewById<TextView>(R.id.inciRate).text = rateAlt.toString()

                        btnDownVote.backgroundTintList = getColorStateList(R.color.transparent)
                        btnUpVote.backgroundTintList = getColorStateList(R.color.btnBackground)

                        downVoted = false
                    }

                    upVoted = true
                } else if(upVoted) {
                    db.collection("pings").document(id)
                        .update("rate", FieldValue.increment(-1))

                    db.collection("users").document(auth.currentUser!!.uid)
                        .update("upVote", FieldValue.arrayRemove(id))

                    var rateAlt = view.findViewById<TextView>(R.id.inciRate).text.toString().toInt()
                    rateAlt -= 1
                    view.findViewById<TextView>(R.id.inciRate).text = rateAlt.toString()

                    btnUpVote.backgroundTintList = getColorStateList(R.color.transparent)
                    upVoted = false
                }

                btnUpVote.isEnabled = true
            }

            alert = build.create()
            alert.show()
            alert.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))


            val ping = db.collection("pings").document(id)
            ping.get()
                .addOnSuccessListener { document ->
                    try {
                        view.findViewById<TextView>(R.id.inciType).text =
                            document.data!!["type"].toString()
                        view.findViewById<TextView>(R.id.inciDesc).text =
                            document.data!!["description"].toString()
                        view.findViewById<TextView>(R.id.inciDate).text =
                            document.data!!["time"].toString()
                        view.findViewById<TextView>(R.id.inciRate).text =
                            document.data!!["rate"].toString()
                        val addresses: List<Address> =
                            geocoder.getFromLocation(
                                document.data!!["latitude"] as Double,
                                document.data!!["longitude"] as Double,
                                1
                            )
                        val address = addresses[0].getAddressLine(0)
                        view.findViewById<TextView>(R.id.inciAddress).text = address

                        if(auth.currentUser!!.uid == document.data!!["uid"].toString()) {
                            view.findViewById<Button>(R.id.btnDltInci).visibility = View.VISIBLE
                            view.findViewById<Button>(R.id.btnEdtInci).visibility = View.VISIBLE
                        }
                    } catch (e: IOException) {

                    } catch (e: NullPointerException) {
                        alert.dismiss()

                        Toast.makeText(
                            baseContext, "Esse incidente não está disponível.",
                            Toast.LENGTH_SHORT
                        ).show()

                        marker.remove()
                    }
                }
                .addOnFailureListener {
                    alert.dismiss()
                    Toast.makeText(
                        baseContext, "Não foi possível recuperar os dados desse incidente.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            db.collection("users").document(auth.currentUser!!.uid).get()
                .addOnSuccessListener { document ->
                        val downVoteArray = document.data!!["downVote"] as ArrayList<String>
                        val upVoteArray = document.data!!["upVote"] as ArrayList<String>

                        for (vote in downVoteArray) {
                            if (vote == id) {
                                downVoted = true
                                upVoted = false
                                btnDownVote.backgroundTintList = getColorStateList(R.color.btnBackground)
                            }
                        }
                        for (vote in upVoteArray) {
                            if (vote == id) {
                                upVoted = true
                                downVoted = false
                                btnUpVote.backgroundTintList = getColorStateList(R.color.btnBackground)
                            }
                        }
                    }

            if(auth.currentUser != null){
                btnDownVote.isEnabled = true
                btnUpVote.isEnabled = true
            }

            true
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

