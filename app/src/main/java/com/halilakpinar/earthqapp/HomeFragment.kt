package com.halilakpinar.earthqapp

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.halilakpinar.earthqapp.Model.NestedJSONModel
import com.halilakpinar.earthqapp.Service.EarthquakeAPI
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


class HomeFragment : Fragment() {

    private val BASE_URL="https://earthquake.usgs.gov/fdsnws/event/1/"
    private var compositeDisposable:CompositeDisposable?=null


    private lateinit var locationManager:LocationManager
    private lateinit var locationListener: LocationListener

    private lateinit var permissionLauncher:ActivityResultLauncher<String>

    private var currentLatitude:Double?=null
    private var currentLongitude:Double?=null






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home2, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        compositeDisposable= CompositeDisposable()

        registerLauncher()

        getCurrentLocation()

        loadData()

    }

    fun registerLauncher(){

        permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){ result->
            if(result){
                //permission granted
                if(ContextCompat.checkSelfPermission(requireActivity().applicationContext,android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,10000,0f,locationListener)

                }

            }else{
                //permission denied
                Toast.makeText(requireContext(),"Permission denied!",Toast.LENGTH_LONG).show()
            }
        }
    }

    fun getCurrentLocation(){
        locationManager= requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager


        locationListener = object :LocationListener{

            override fun onLocationChanged(location: Location) {

                currentLatitude=location.latitude
                currentLongitude=location.longitude

            }

        }

        if(ContextCompat.checkSelfPermission(requireActivity().applicationContext,android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),android.Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(requireView(),"Permission needed for location",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){
                    //request permission
                    permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            }else{

                //request permission
                permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }else{
            //permission granted
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,10000,0f,locationListener)
        }

        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,10000,0f,locationListener)
    }



    fun loadData(){

        val retrofit=Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().create(EarthquakeAPI::class.java)

        println("enqueue öncesi")
        compositeDisposable?.add(retrofit.getCurrentLocationData("35","28","2023-01-24","2023-01-26")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::handleResponse))
        println("enqueue sonrası")

        /*
        val service = retrofit.create(EarthquakeAPI::class.java)
        val call = service.getData()

        println("enqueue öncesi")
        call.enqueue(object :Callback<NestedJSONModel>{
            override fun onResponse(
                call: Call<NestedJSONModel>,
                response: Response<NestedJSONModel>
            ) {
                if(response.isSuccessful){
                    response.body()?.let {

                        println(it.features.get(0).properties.place)
                        println(it.features.get(0).properties.mag)
                        println(it.features.get(0).properties.time)

                    }
                }
            }

            override fun onFailure(call: Call<NestedJSONModel>, t: Throwable) {
                println(t.localizedMessage)
            }

        })
        println("enqueue sonrası")
        */

    }

    private fun handleResponse(response:NestedJSONModel){
        response?.let {
            println(response.metadata.url)
            println(response.features.get(0).properties.place)
            println(response.features.get(0).properties.mag)
            println(response.features.get(0).properties.time)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable?.clear()
    }


}