// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.codelabs.buildyourfirstmap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.Group
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.codelabs.buildyourfirstmap.place.Place
import com.google.codelabs.buildyourfirstmap.place.PlaceRenderer
import com.google.codelabs.buildyourfirstmap.place.PlacesReader
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.maps.android.PolyUtil
import com.google.maps.android.clustering.ClusterManager
import kotlinx.coroutines.*
import okhttp3.*
import kotlinx.coroutines.tasks.await
import org.json.JSONException
import org.json.JSONObject
import java.io.*

class MainActivity : AppCompatActivity() {

    private val places: List<Place> by lazy {
        PlacesReader(this).read()
    }
    private val activity = this
    private val fileName = "route.nimbus"
    private var file: File = File(fileName)
    lateinit var actifPolyLine: PolylineOptions
    var list_route: MutableList<PolylineOptions> = mutableListOf()
    lateinit var listTrajet: MutableList<Trajet>
    var routes: Routes = Routes(mutableListOf())
    lateinit var polys : Polyline
    // Get the Firebase Firestore instance
    val db = FirebaseFirestore.getInstance()

    @OptIn(InternalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        /*for(x in 0..places.size){
            addData(places[x],"places")
        }*/
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted, perform operations that require external storage access
        } else {
            // Permission not yet granted, request permission from the user
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
        }
        var cardView: CardView? = null
        cardView = findViewById(R.id.base_cardview)
        val arrow = findViewById<ImageView>(R.id.show)
        val hiddenGroup = findViewById<Group>(R.id.card_group)
        arrow.setOnClickListener { view ->
            if (hiddenGroup.visibility === View.VISIBLE) {
                TransitionManager.beginDelayedTransition(cardView, AutoTransition())
                hiddenGroup.visibility = View.GONE
                arrow.setImageResource(R.drawable.ic_arrow_down_float)
            } else {
                TransitionManager.beginDelayedTransition(cardView, AutoTransition())
                hiddenGroup.visibility = View.VISIBLE
                arrow.setImageResource(R.drawable.ic_arrow_up_float)
            }
        }
        listTrajet = mutableListOf(
            Trajet(places[0], places[1], "Elf Axe Lourd", "Salle des fêtes d'Akwa, Douala"),
            Trajet(places[0], places[2], "Elf Axe Lourd", "Carrefour Dallip"),
            Trajet(places[5], places[4], "Ndokoti", "Poste Centrale de Bonanjo"),
            Trajet(places[5], places[3], "Ndokoti", "Délégation Régionale Des PTT Bonanjo"),
            Trajet(places[6], places[4], "Bonabéri", "Poste Centrale de Bonanjo"),
            Trajet(places[6], places[5], "Bonabéri", "Ndokoti"),
            Trajet(places[6], places[7], "Bonabéri", "Marché Centrale de Douala"),
            Trajet(places[10], places[9], "Carrefour des douanes du Cameroun", "PK 14"),
            Trajet(places[5], places[9], "Ndokoti", "PK 14")
        )

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            // Ensure all places are visible in the map
            googleMap.setOnMapLoadedCallback {
                val bounds = LatLngBounds.builder()
                places.forEach {
                    bounds.include(it.latLng)
                    //calculateViewport(googleMap, it.latLng.latitude, it.latLng.longitude, bounds.build())
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 20))
                }
            }
            GlobalScope.launch {
                val list = fillList()
                // Do something with the list, like display it in a RecyclerView
            }
            //addMarkers(googleMap)
            file = File(this@MainActivity.getFilesDir(), fileName)
            addClusteredMarkers(googleMap)

            // Set custom info window adapter
            // googleMap.setInfoWindowAdapter(MarkerInfoWindowAdapter(this))
            var adapter = TrajetItemAdapter(listTrajet)
            listTrajet.add(Trajet(places[5], places[9], "Ndokoti", "PK 14"))
            val layoutManager = LinearLayoutManager(applicationContext)
            var trajet_recycle: RecyclerView = findViewById(R.id.recyclerview_routesinfo)
            trajet_recycle.layoutManager = layoutManager
            trajet_recycle.adapter = adapter
            adapter.notifyDataSetChanged()
            // Applying OnClickListener to our Adapter
            adapter.setOnClickListener(object : TrajetItemAdapter.OnClickListener {
                override fun onClick(position: Int, model: Trajet) {
                    HidePolyline(googleMap)
                    drawPolyline(googleMap, list_route[position])
                    // Do something with the list, like display it in a RecyclerView
                }
            })

        }

    }

    /**
     * Adds markers to the map with clustering support.
     */
    private fun addClusteredMarkers(googleMap: GoogleMap) {
        // Create the ClusterManager class and set the custom renderer
        val clusterManager = ClusterManager<Place>(this, googleMap)
        clusterManager.renderer =
            PlaceRenderer(
                this,
                googleMap,
                clusterManager
            )

        // Set custom info window adapter
        clusterManager.markerCollection.setInfoWindowAdapter(MarkerInfoWindowAdapter(this))

        // Add the places to the ClusterManager
        clusterManager.addItems(places)
        clusterManager.cluster()

        // Show polygon
        clusterManager.setOnClusterItemClickListener { item ->
            addCircle(googleMap, item)
            return@setOnClusterItemClickListener false
        }

        // When the camera starts moving, change the alpha value of the marker to translucent
        googleMap.setOnCameraMoveStartedListener {
            clusterManager.markerCollection.markers.forEach { it.alpha = 0.3f }
            clusterManager.clusterMarkerCollection.markers.forEach { it.alpha = 0.3f }
        }

        googleMap.setOnCameraIdleListener {
            // When the camera stops moving, change the alpha value back to opaque
            clusterManager.markerCollection.markers.forEach { it.alpha = 1.0f }
            clusterManager.clusterMarkerCollection.markers.forEach { it.alpha = 1.0f }

            // Call clusterManager.onCameraIdle() when the camera stops moving so that re-clustering
            // can be performed when the camera stops moving
            clusterManager.onCameraIdle()
        }
    }

    private var circle: Circle? = null

    /**
     * Adds a [Circle] around the provided [item]
     */
    private fun addCircle(googleMap: GoogleMap, item: Place) {
        circle?.remove()
        circle = googleMap.addCircle(
            CircleOptions()
                .center(item.latLng)
                .radius(30.0)
                .fillColor(ContextCompat.getColor(this, R.color.colorPrimaryTranslucent))
                .strokeColor(ContextCompat.getColor(this, R.color.colorPrimary))
        )
    }

    private val busIcon: BitmapDescriptor by lazy {
        val color = ContextCompat.getColor(this, R.color.colorPrimary)
        BitmapHelper.vectorToBitmap(this, R.drawable.ic_buspark, color)
    }

    /**
     * Adds markers to the map. These markers won't be clustered.
     */
    private fun addMarkers(googleMap: GoogleMap) {
        places.forEach { place ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .title(place.name)
                    .position(place.latLng)
                    .icon(busIcon)
            )
            // Set place as the tag on the marker object so it can be referenced within
            // MarkerInfoWindowAdapter
            marker?.tag = place
        }
    }

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }

    fun checkFile(files: File): Boolean {
        return (files.exists())
    }

    fun writeFile() {
        // check if file exists

        if (checkFile(file)) {
            // read text from file
            var gson = Gson()
            var json: String = file.readText()
            routes = gson.fromJson(json, Routes::class.java)
            for (x in 0..(routes.route.size - 1)) {
                list_route.add(routes.route[x])
            }
            actifPolyLine = list_route[0]
            Log.d("MainACtivity", "Default Polyline selected")
            // = (objectInputStream.readObject() as? MutableList<PolylineOptions>)!!
            Log.d("MainACtivity", "Succesfully read from file")
        } else {
            // create new file and write text to it
            file.createNewFile()
            var gson = Gson()
            var json: String = gson.toJson(routes)
            file.writeText(json)
        }
    }

    //Display PolyLine on Google Map
    fun drawPolyline(map: GoogleMap, polylineOptions: PolylineOptions) {
        // Add the polyline to the map
        activity.runOnUiThread(java.lang.Runnable {
            addClusteredMarkers(map)
            actifPolyLine = polylineOptions
            polys = map.addPolyline(polylineOptions)

            // Hide the polyline
            polys.isVisible = true
        })
    }

    fun HidePolyline(map: GoogleMap) {
        // Add the polyline to the map
        activity.runOnUiThread(java.lang.Runnable {
           /* polys = map.addPolyline(actifPolyLine)
            // Hide the polyline
           polys.isVisible = false
            polys.remove()*/
            map.clear();
        })
    }

    fun fetchFirestoreDataPeriodically() {
        // Create a new coroutine scope
        val scope = CoroutineScope(Dispatchers.Default)

        // Start a coroutine that runs indefinitely
        scope.launch {
            while (true) {
                // Use Firestore to fetch data here
                val firestoreData = fetchFirestoreLocation()

                // Process the data or update UI as needed
                processData(firestoreData)

                // Wait for 10 seconds before fetching data again
                delay(10_000)
            }
        }
    }

    //Get route list asynchronously
    private suspend fun fillList() {
        return withContext(Dispatchers.IO) {
            // Perform a long-running operation to retrieve data
            // For example, retrieve data from an API
            if (!checkFile(file)) {
                for (x in 0..1) {
                    drawRouteOnMap(listTrajet[x].arrival.latLng, listTrajet[x].depart.latLng)
                }
                delay(15000)
                activity.runOnUiThread(java.lang.Runnable {
                    Log.d(
                        "MainACtivity",
                        "Routes, Fetched Succesfully, " + list_route.size.toString()
                    )
                    actifPolyLine = list_route[0]
                    routes.route = list_route
                    writeFile()
                    Log.d("MainACtivity", "Routes, Saved to Memory")
                })
            } else {

                writeFile()
            }
        }
    }

    // Function to draw a road on Google Map between two points
    fun drawRouteOnMap(origin: LatLng, destination: LatLng) {
        val endpointBuilder = StringBuilder()
        endpointBuilder.append("https://maps.googleapis.com/maps/api/directions/json?")
        endpointBuilder.append("origin=${origin.latitude},${origin.longitude}&")
        endpointBuilder.append("destination=${destination.latitude},${destination.longitude}&")
        endpointBuilder.append("key=AIzaSyC5rTG-2EgEQNPQpvlo5zwEh6_5sncUero")

        val url = endpointBuilder.toString()
        val request = Request.Builder().url(url).build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    var jsonResponse = JSONObject(response.body?.string())
                    var status = jsonResponse.getString("status")
                    Log.d(TAG, status)
                    Log.d(TAG, endpointBuilder.toString())
                    if (status == "OK") {
                        var routesArray = jsonResponse.getJSONArray("routes")
                        var route = routesArray.getJSONObject(0)
                        var overviewPolyline = route.getJSONObject("overview_polyline")

                        var encodedString = overviewPolyline.getString("points")
                        var pointsList = PolyUtil.decode(encodedString)

                        var polylineOptions = PolylineOptions()
                        polylineOptions.addAll(pointsList)
                        polylineOptions.color(Color.BLUE)
                        polylineOptions.width(10f)

                        activity.runOnUiThread(java.lang.Runnable {
                            list_route.add(polylineOptions)
                            Log.d(
                                "MainACtivity",
                                "Routes, Fetched Succesfully, " + list_route.size.toString()
                            )
                        })
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Log.d(TAG, e.message!!)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.d(TAG, e.message!!)
            }
        })
    }


    suspend fun fetchFirestoreLocation(): List<Location> {
        // Use Firestore to fetch data here
        // This should be a suspend function that returns the fetched data
        return db.collection("buses")
            .get()
            .await()
            .toObjects(Location::class.java)
    }

    fun processData(data: List<Location>) {
        // Process the data or update UI as needed
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //check if the request code matches the REQUEST_LOCATION
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, perform operations that require external storage access
                } else {
                    // Permission denied, handle the denial gracefully or show a message to the user
                }
                return
            }
        }
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, continue with file operations
            } else {
                // Permission denied, show explanation or handle failure gracefully
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
            }
        }
    }

}
