package com.google.codelabs.buildyourfirstmap

import com.google.android.gms.maps.model.PolylineOptions
import com.google.codelabs.buildyourfirstmap.place.Place

data class Trajet(val depart: Place, val arrival: Place, val departName: String, val arrivalName: String ) :java.io.Serializable
data class Activity(val trajet : Int, val dates : String) :java.io.Serializable
data class Routes(var route : MutableList<PolylineOptions>)