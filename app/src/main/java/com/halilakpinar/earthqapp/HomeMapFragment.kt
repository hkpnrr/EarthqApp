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
import com.halilakpinar.earthqapp.Model.AfadEarthquake
import com.halilakpinar.earthqapp.Service.AfadAPI
import com.halilakpinar.earthqapp.Settings.Constants
import com.halilakpinar.earthqapp.Settings.Constants.COORDINATE_INTERVAL
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class HomeMapFragment : Fragment() {

    private lateinit var mapFragment: SupportMapFragment
    private var compositeDisposable: CompositeDisposable?=null
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var currentLatitude:Double?=null
    private var currentLongitude:Double?=null
    private var minLatitude:Double?=null
    private var minLongitude:Double?=null
    private var maxLatitude:Double?=null
    private var maxLongitude:Double?=null
    private var startDate:String?=null
    private var endDate:String?=null
    private lateinit var dataList: List<AfadEarthquake>
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->

        val userLocation = LatLng(currentLatitude!!, currentLongitude!!)
        googleMap.isMyLocationEnabled=true

        for (feature:AfadEarthquake in dataList){
            googleMap.addMarker(MarkerOptions().position(LatLng(feature.latitude.toDouble(),feature.longitude.toDouble())).title(feature.eventID))
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,4f))

        googleMap.setOnMarkerClickListener {
            val builder = AlertDialog.Builder(requireContext())
            for (feature:AfadEarthquake in dataList){
                if(feature.eventID == it.title){
                    with(builder)
                    {
                        setTitle(feature.location)
                        setMessage("Magnitude: "+feature.magnitude+"\n"+"Date: "+feature.date+"\n"+
                                "Coordinates: "+feature.latitude+"-"+feature.longitude+"\n"+
                                "Depth :"+feature.depth+" KM"+"\n"+
                        "District :"+feature.district+"/"+feature.province)

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
                    minLatitude= currentLatitude!! -COORDINATE_INTERVAL
                    maxLatitude= currentLatitude!! +COORDINATE_INTERVAL
                    minLongitude= currentLongitude!! -COORDINATE_INTERVAL
                    maxLongitude= currentLongitude!! +COORDINATE_INTERVAL

                    getCurrentTime()
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
                            minLatitude= currentLatitude!! -COORDINATE_INTERVAL
                            maxLatitude= currentLatitude!! +COORDINATE_INTERVAL
                            minLongitude= currentLongitude!! -COORDINATE_INTERVAL
                            maxLongitude= currentLongitude!! +COORDINATE_INTERVAL

                            getCurrentTime()
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
            .connectTimeout(TIME_OUT.toLong(), TimeUnit.SECONDS)
            .readTimeout(TIME_OUT.toLong(), TimeUnit.SECONDS)
            .writeTimeout(TIME_OUT.toLong(), TimeUnit.SECONDS)
            .build()

        val retrofit=Retrofit.Builder()
            .baseUrl(Constants.BASE_URL_AFAD)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().create(AfadAPI::class.java)

        compositeDisposable?.add(retrofit.getCurrentLocationDataNew(minLatitude.toString(),maxLatitude.toString(),minLongitude.toString(),maxLongitude.toString(),startDate.toString(),endDate.toString())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({handleResponse(it)},{handleError(it)}))

    }

    private fun handleError(t: Throwable) {
        Toast.makeText(requireContext(),"Unexpected Error! Please try again. Error: "+t.localizedMessage,Toast.LENGTH_LONG).show()
    }

    private fun handleResponse(response:List<AfadEarthquake>){
        response?.let {
            hideProgressBar()

            if(response.isEmpty()){
                Toast.makeText(requireContext(),"Not Found Any Earthquake",Toast.LENGTH_LONG).show()
            }

            dataList=response
            mapFragment?.getMapAsync(callback)

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable?.clear()
    }
}