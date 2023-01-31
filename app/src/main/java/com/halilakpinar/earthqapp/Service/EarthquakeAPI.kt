package com.halilakpinar.earthqapp.Service

import com.halilakpinar.earthqapp.Model.NestedJSONModel
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface EarthquakeAPI {

    //https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2023-01-24T21:03:26&endtime=2023-01-26&latitude=36&longitude=29&maxradiuskm=500
    @GET("query?format=geojson&starttime=2023-01-24T21:03:26&endtime=2023-01-26&latitude=36&longitude=29&maxradiuskm=500")
    fun getData():Observable<NestedJSONModel>

    //query?format=geojson&starttime=2023-01-24T21:03:26&endtime=2023-01-26&maxradiuskm=500
    @GET("query?format=geojson&maxradiuskm=500")
    fun getCurrentLocationData(@Query("latitude") latitude:String,
                               @Query("longitude") longitude:String,
    @Query("starttime") startTime:String,
    @Query("endtime") endTime:String):Observable<NestedJSONModel>

    @GET("query?format=geojson")
    fun getSearchEarthquakes(@Query("latitude") latitude:String,
                               @Query("longitude") longitude:String,
                               @Query("starttime") startTime:String,
                               @Query("endtime") endTime:String,
                               @Query("maxradiuskm") radius:String,
                               @Query("minmagnitude") magnitude:String):Observable<NestedJSONModel>

    @GET("query?")
    fun getCurrentLocationData2(@Query("format") format:String,
                               @Query("starttime") startTime:String,
                               @Query("endtime") endTime:String):Observable<NestedJSONModel>
}