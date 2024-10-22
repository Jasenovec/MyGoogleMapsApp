package com.jasenovec.mygooglemapsapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.gms.common.api.ApiException

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var btnChangeMapType: Button
    private lateinit var btnSearch: Button
    private lateinit var editTextSearch: EditText

    private var currentMapType = GoogleMap.MAP_TYPE_NORMAL

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "PlacesAPI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Places
        Places.initialize(applicationContext, "AIzaSyBTbkCPZ9b-C7kzN_mtQqbvMLSy_DPE90U")
        placesClient = Places.createClient(this)

        // Inicializar FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializar las vistas
        btnChangeMapType = findViewById(R.id.btnChangeMapType)
        btnSearch = findViewById(R.id.btnSearch)
        editTextSearch = findViewById(R.id.editTextSearch)

        // Configurar el fragmento del mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_container) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configurar listeners para los botones
        btnChangeMapType.setOnClickListener { changeMapType() }
        btnSearch.setOnClickListener { searchNearbyPlaces() }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Añadir un marcador en Sídney y mover la cámara
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Sídney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 12f))

        // Llama a centerMapOnMyLocation() después de verificar los permisos
        enableMyLocation()
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            centerMapOnMyLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun centerMapOnMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val myLocation = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f))
                } else {
                    showToast("No se pudo obtener la ubicación")
                }
            }.addOnFailureListener {
                showToast("Error al obtener la ubicación")
            }

            mMap.setOnMyLocationButtonClickListener {
                centerMapOnMyLocation()
                true
            }
        }
    }

    private fun searchNearbyPlaces() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Solicitar los campos más recientes: ID y ADDRESS_COMPONENTS
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.ADDRESS_COMPONENTS
            )

            val request = FindCurrentPlaceRequest.newInstance(placeFields)

            placesClient.findCurrentPlace(request)
                .addOnSuccessListener { response ->
                    for (placeLikelihood in response.placeLikelihoods) {
                        val place = placeLikelihood.place
                        val placeId = place.id ?: "ID no disponible"
                        val addressComponents = place.addressComponents?.asList()
                            ?.joinToString(", ") { it.name }
                            ?: "Dirección no disponible"

                        // Mostrar el marcador con el ID o los componentes de dirección
                        mMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(0.0, 0.0)) // Cambia según tus necesidades
                                .title("ID: $placeId, Dirección: $addressComponents")
                        )
                    }
                }
                .addOnFailureListener { exception ->
                    if (exception is ApiException) {
                        Log.e(TAG, "Place not found: ${exception.statusCode}")
                    } else {
                        showToast("Error al obtener lugares")
                    }
                }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                showToast("Permiso de ubicación denegado")
            }
        }
    }

    private fun changeMapType() {
        if (::mMap.isInitialized) {
            currentMapType = (currentMapType + 1) % 4
            val mapType = when (currentMapType) {
                0 -> GoogleMap.MAP_TYPE_NORMAL.also { showToast("Mapa Normal") }
                1 -> GoogleMap.MAP_TYPE_SATELLITE.also { showToast("Mapa Satélite") }
                2 -> GoogleMap.MAP_TYPE_TERRAIN.also { showToast("Mapa Terreno") }
                3 -> GoogleMap.MAP_TYPE_HYBRID.also { showToast("Mapa Híbrido") }
                else -> GoogleMap.MAP_TYPE_NORMAL
            }
            mMap.mapType = mapType
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
