package com.halilakpinar.earthqapp.Service

import com.halilakpinar.earthqapp.Model.NestedJSONModel
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.http.GET

interface EarthquakeAPI {

    //https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2023-01-24T21:03:26&endtime=2023-01-26&latitude=36&longitude=29&maxradiuskm=500
    @GET("query?format=geojson&starttime=2023-01-24T21:03:26&endtime=2023-01-26&latitude=36&longitude=29&maxradiuskm=500")
    fun getData():Observable<NestedJSONModel>
}