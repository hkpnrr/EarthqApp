package com.halilakpinar.earthqapp.Service

import com.halilakpinar.earthqapp.Model.AfadEarthquake
import com.halilakpinar.earthqapp.Model.AfadResponse
import com.halilakpinar.earthqapp.Model.NestedJSONModel
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface AfadAPI {

    //https://deprem.afad.gov.tr/apiv2/event/
    @GET("filter?maxrad=1000000")
    fun getCurrentLocationData(@Query("lat") latitude:String,
                               @Query("lon") longitude:String,
                               @Query("start") startTime:String,
                               @Query("end") endTime:String): Observable<List<AfadEarthquake>>


    @GET("filter?")
    fun getSearchEarthquakes(@Query("lat") latitude:String,
                             @Query("lon") longitude:String,
                             @Query("start") startTime:String,
                             @Query("end") endTime:String,
                             @Query("maxrad") radius:String,
                             @Query("minmag") magnitude:String):Observable<List<AfadEarthquake>>

    @GET("filter?")
    fun getCurrentLocationDataNew(@Query("minlat") minLatitude:String,
                               @Query("maxlat") maxLatitude:String,
                               @Query("minlon") minLongitude:String,
                               @Query("maxlon") maxLongitude:String,
                               @Query("start") startTime:String,
                               @Query("end") endTime:String): Observable<List<AfadEarthquake>>

    @GET("filter?")
    fun getSearchEarthquakesNew(@Query("minlat") minLatitude:String,
                                @Query("maxlat") maxLatitude:String,
                                @Query("minlon") minLongitude:String,
                                @Query("maxlon") maxLongitude:String,
                             @Query("start") startTime:String,
                             @Query("end") endTime:String,
                             @Query("minmag") magnitude:String):Observable<List<AfadEarthquake>>

}