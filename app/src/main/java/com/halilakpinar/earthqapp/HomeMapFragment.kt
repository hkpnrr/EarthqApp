package com.halilakpinar.earthqapp

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.snackbar.Snackbar
import com.halilakpinar.earthqapp.Model.FeaturesModel
import com.halilakpinar.earthqapp.Model.NestedJSONModel
import com.halilakpinar.earthqapp.Service.EarthquakeAPI
import com.halilakpinar.earthqapp.Settings.Constants.BASE_URL
import com.halilakpinar.earthqapp.Settings.Constants.DATE_INTERVAL
import com.halilakpinar.earthqapp.Settings.Constants.TIME_OUT
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home_map.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class HomeMapFragment : Fragment() {

    private lateinit var mapFragment: SupportMapFragment

    private var compositeDisposable: CompositeDisposable?=null
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private var currentLatitude:Double?=null
    private var currentLongitude:Double?=null

    private var startDate:String?=null
    private var endDate:String?=null

    private lateinit var dataList: NestedJSONModel

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->

        val userLocation = LatLng(currentLatitude!!, currentLongitude!!)
        googleMap.isMyLocationEnabled=true

        for (feature:FeaturesModel in dataList.features){
            googleMap.addMarker(MarkerOptions().position(LatLng(feature.geometry.coordinates[1],feature.geometry.coordinates[0])).title(feature.id))
            println(feature.properties.place)
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,4f))

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapFragment = (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
        showProgressBar()
        compositeDisposable= CompositeDisposable()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        floatingActionButtonMap.setOnClickListener {
            val action = HomeMapFragmentDirections.actionHomeMapFragmentToHomeFragment()
            Navigation.findNavController(it).navigate(action)

        }
        registerLauncher()
        getCurrentLocation()

    }

    private fun getCurrentTime(){
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        endDate = LocalDateTime.now().format(formatter)
        startDate = LocalDateTime.now().minusDays(DATE_INTERVAL.toLong()).format(formatter)
    }

    fun getCurrentLocation(){

        if(ContextCompat.checkSelfPermission(requireActivity().applicationContext,android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),android.Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(requireView(),"Permission needed for location", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){
                    //request permission
                    permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            }else{

                //request permission
                permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }else{
            //permission granted
            fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location ->
                    currentLatitude=location.latitude
                    currentLongitude=location.longitude
                    getCurrentTime()

                    println(currentLatitude)
                    println(currentLongitude)
                    loadData()

                }
                .addOnFailureListener { exception ->

                    Toast.makeText(requireContext(),exception.localizedMessage, Toast.LENGTH_LONG).show()
                }
        }
    }

    fun registerLauncher(){

        permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){ result->
            if(result){
                //permission granted
                if(ContextCompat.checkSelfPermission(requireActivity().applicationContext,android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){


                    fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                        .addOnSuccessListener { location ->
                            currentLatitude=location.latitude
                            currentLongitude=location.longitude
                            getCurrentTime()

                            println(currentLatitude)
                            println(currentLongitude)
                            loadData()

                        }
                        .addOnFailureListener { exception ->

                            Toast.makeText(requireContext(),exception.localizedMessage,Toast.LENGTH_LONG).show()
                        }
                }

            }else{
                //permission denied
                Toast.makeText(requireContext(),"Permission denied!",Toast.LENGTH_LONG).show()
            }
        }
    }

    fun showProgressBar(){
        progressBarMap.visibility=View.VISIBLE
        floatingActionButtonMap.visibility=View.GONE

    }

    fun hideProgressBar(){
        progressBarMap.visibility=View.GONE
        floatingActionButtonMap.visibility=View.VISIBLE

    }
    fun loadData(){

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(TIME_OUT.toLong(),TimeUnit.SECONDS)
            .readTimeout(TIME_OUT.toLong(),TimeUnit.SECONDS)
            .writeTimeout(TIME_OUT.toLong(),TimeUnit.SECONDS)
            .build()
        val retrofit= Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().create(EarthquakeAPI::class.java)
//"2022-01-24","2023-01-26"
        compositeDisposable?.add(retrofit.getCurrentLocationData(currentLatitude.toString(),currentLongitude.toString(),startDate.toString(),endDate.toString())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({handleResponse(it)},{handleError(it)}))
    }

    private fun handleError(t: Throwable) {
        Log.d("handleError", "Error: $t")
        Toast.makeText(requireContext(),"Unexpected Error! Please try again. Error: "+t.localizedMessage,Toast.LENGTH_LONG).show()
    }

    private fun handleResponse(response: NestedJSONModel){
        response?.let {
            hideProgressBar()
            println(response.metadata.url)
            if(response.features.isNotEmpty()){
                println(response.metadata.url)
                println(response.features.get(0).properties.place)
                println(response.features.get(0).properties.mag)
                println(response.features.get(0).properties.time)
            }
            else{
                Toast.makeText(requireContext(),"Not Found Any Earthquake",Toast.LENGTH_LONG).show()

            }
            println(currentLatitude)
            println(currentLongitude)
            dataList=response
            mapFragment?.getMapAsync(callback)

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable?.clear()
    }
}