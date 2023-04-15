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

import android.graphics.Color
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.codelabs.buildyourfirstmap.place.Place
import com.google.codelabs.buildyourfirstmap.place.PlaceRenderer
import com.google.codelabs.buildyourfirstmap.place.PlacesReader
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.clustering.ClusterManager

class MainActivity : AppCompatActivity() {

    private val places: List<Place> by lazy {
        PlacesReader(this).read()
    }
    // Get the Firebase Firestore instance
    val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        /*for(x in 0..places.size){
            addData(places[x],"places")
        }*/
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

            //addMarkers(googleMap)
            addClusteredMarkers(googleMap)

            // Set custom info window adapter
            // googleMap.setInfoWindowAdapter(MarkerInfoWindowAdapter(this))
        }
    }

    fun calculateViewport(map: GoogleMap, lat: Double, lng: Double, bounds : LatLngBounds): Pair<LatLng, LatLng> {
        // Create a LatLng object from lat and lng coordinates
        val position = LatLng(lat, lng)

        // Create a circle with a 10-meter radius around the point
        val circleOptions = CircleOptions()
            .center(position)
            .radius(2.0)
            .strokeColor(Color.CYAN)
            .fillColor(Color.TRANSPARENT)
        val circle = map.addCircle(circleOptions)

        // Get the bounds of the circle
       // val bounds = circle.center
        //val bounds = LatLngBounds.builder().include(position).build()

        // Get the northeast and southwest points of the bounds
        val ne = bounds.northeast
        val sw = bounds.southwest



        // Fit the map to the new bounds
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))

        // Remove the circle from the map
        circle.remove()

        // Return the northeast and southwest points
        return Pair(ne, sw)
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

    fun addData(location: Place, collectionName: String) {
        val collectionRef = db.collection(collectionName)
        collectionRef.add(location)
            .addOnSuccessListener {
                println("Data added successfully!")
            }
            .addOnFailureListener { e ->
                println("Error adding data: $e")
            }
    }
}
