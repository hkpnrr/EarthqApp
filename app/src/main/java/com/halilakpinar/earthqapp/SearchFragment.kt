package com.halilakpinar.earthqapp

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.halilakpinar.earthqapp.Model.FeaturesModel
import com.halilakpinar.earthqapp.Model.NestedJSONModel
import com.halilakpinar.earthqapp.Service.EarthquakeAPI
import com.halilakpinar.earthqapp.Settings.Constants
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_home_map.progressBarMap
import kotlinx.android.synthetic.main.fragment_search.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class SearchFragment : Fragment() , GoogleMap.OnMapLongClickListener{

    private lateinit var mapFragment: SupportMapFragment
    private lateinit var mMap: GoogleMap

    private var compositeDisposable: CompositeDisposable?=null

    private var selectedLatitude:Double?=null
    private var selectedLongitude:Double?=null
    private var selectedDate:LocalDate?=null
    private var selectedRadius:String?=null
    private var selectedMagnitude:String?=null

    private var startDate:String?=null
    private var endDate:String?=null

    private lateinit var dataList: NestedJSONModel

    private val callback = OnMapReadyCallback { googleMap ->

        mMap=googleMap
        googleMap.setOnMapLongClickListener(this)
        val center = LatLng(0.0, 0.0)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center,1f))
    }

    private val callbackSearch = OnMapReadyCallback { googleMap ->

        mMap=googleMap
        googleMap.setOnMapLongClickListener(this)

        if(dataList.features.isEmpty()){
            Toast.makeText(requireContext(),"Not Found Any Earthquake",Toast.LENGTH_LONG).show()
        }
        for (feature: FeaturesModel in dataList.features){
            googleMap.addMarker(MarkerOptions().position(LatLng(feature.geometry.coordinates[1],feature.geometry.coordinates[0])).title(feature.id))
            println(feature.properties.place)
        }

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(selectedLatitude!!,
            selectedLongitude!!
        ),7f))

        googleMap.setOnMarkerClickListener {
            val builder = AlertDialog.Builder(requireContext())
            for (feature:FeaturesModel in dataList.features){
                if(feature.id == it.title){
                    with(builder)
                    {
                        val sdf = SimpleDateFormat("dd/MM/yy hh:mm a")
                        val date =sdf.format(feature.properties.time)
                        setTitle(feature.properties.place)
                        setMessage("Magnitude: "+feature.properties.mag+"\n"+"Time: "+date+"\n"+
                                "Coordinates: "+feature.geometry.coordinates[0].toString()+" "+feature.geometry.coordinates[1].toString()+"\n"+
                                "Alert Level : "+feature.properties.alert)

                        show()
                    }
                }
            }

            return@setOnMarkerClickListener false
        }
    }

    override fun onMapLongClick(p0: LatLng) {
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(p0).title("Selected Area"))
        selectedLatitude=p0.latitude
        selectedLongitude=p0.longitude
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapFragment = (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
        mapFragment?.getMapAsync(callback)
        hideProgressBar()

        compositeDisposable= CompositeDisposable()

        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            selectedDate=null
            val actualMonth=month+1

            selectedDate= LocalDate.of(year,actualMonth,dayOfMonth)
            endDate=selectedDate?.minusDays(-15).toString()
            startDate=selectedDate?.minusDays(15).toString()

            println("currentDate " +selectedDate.toString())
            println("startDate " +startDate.toString())
            println("endDate " +endDate.toString())


        }

        buttonSearch.setOnClickListener {
            showProgressBar()
            searchEarthquakes()
        }
    }

    fun searchEarthquakes(){
        if(editTextRadius.text.toString()!="" && editTextMagnitude.text.toString()!="" &&
            selectedDate!=null && selectedLatitude!=null && selectedLongitude!=null){

            selectedMagnitude=editTextMagnitude.text.toString()
            selectedRadius=editTextRadius.text.toString()

            loadData()


        }
    }



    fun loadData(){

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(Constants.TIME_OUT.toLong(), TimeUnit.SECONDS)
            .readTimeout(Constants.TIME_OUT.toLong(), TimeUnit.SECONDS)
            .writeTimeout(Constants.TIME_OUT.toLong(), TimeUnit.SECONDS)
            .build()
        val retrofit= Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().create(EarthquakeAPI::class.java)
//"2022-01-24","2023-01-26"
        compositeDisposable?.add(retrofit.getSearchEarthquakes(selectedLatitude.toString(),selectedLongitude.toString(),startDate.toString(),endDate.toString(),
        selectedRadius.toString(),selectedMagnitude.toString())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({handleResponse(it)},{handleError(it)}))
    }

    private fun handleError(t: Throwable) {
        Log.d("handleError", "Error: $t")
        Toast.makeText(requireContext(),"Unexpected Error! Please try again. Error: "+t.localizedMessage,Toast.LENGTH_LONG).show()
    }

    fun showProgressBar(){
        progressBarMap.visibility=View.VISIBLE

    }

    fun hideProgressBar(){
        progressBarMap.visibility=View.GONE

    }

    private fun handleResponse(response: NestedJSONModel){
        response?.let {
            hideProgressBar()
            if(response.features.isNotEmpty()){
                println(response.features.get(0).properties.place)
                println(response.features.get(0).properties.mag)
                println(response.features.get(0).properties.time)
            }
            println(response.metadata.url)
            println(selectedLatitude)
            println(selectedLongitude)
            dataList=response
            mapFragment?.getMapAsync(callbackSearch)

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable?.clear()
    }


}