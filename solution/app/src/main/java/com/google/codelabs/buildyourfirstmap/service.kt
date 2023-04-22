package com.google.codelabs.buildyourfirstmap

//data class Service(val trajet : Int, val dates : String,var hours: String, val depart : String, var status : String) :java.io.Serializable
data class ServicesList(val services : MutableList<Service>) :java.io.Serializable
class Service {
    var trajet : Int = -1; var dates : String = "";var hours: String =""; var depart : String=""; var status : String =""
constructor(){

}
    constructor(trajet : Int,dates : String,hours: String, depart : String, status : String)
    {
     this.trajet = trajet
     this.dates = dates
     this.hours = hours
     this.depart = depart
        this.status = status

    }
}

