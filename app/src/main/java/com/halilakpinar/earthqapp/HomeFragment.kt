package com.halilakpinar.earthqapp

import android.content.Context
import android.content.pm.PackageManager
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
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.snackbar.Snackbar
import com.halilakpinar.earthqapp.Adapter.RecyclerViewAdapter
import com.halilakpinar.earthqapp.Model.NestedJSONModel
import com.halilakpinar.earthqapp.Service.EarthquakeAPI
import com.halilakpinar.earthqapp.Settings.Constants.BASE_URL
import com.halilakpinar.earthqapp.Settings.Constants.DATE_INTERVAL
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_home.*
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class HomeFragment : Fragment() {

    private var compositeDisposable:CompositeDisposable?=null

    private lateinit var locationManager:LocationManager

    private lateinit var permissionLauncher:ActivityResultLauncher<String>

    private var currentLatitude:Double?=null
    private var currentLongitude:Double?=null

    private var startDate:String?=null
    private var endDate:String?=null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var recyclerViewAdapter:RecyclerViewAdapter?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        compositeDisposable= CompositeDisposable()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val layoutManager: RecyclerView.LayoutManager=LinearLayoutManager(requireContext())
        recyclerView.layoutManager=layoutManager

        registerLauncher()

        getCurrentLocation()

        floatingActionButton.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToHomeMapFragment()
            Navigation.findNavController(it).navigate(action)
        }

    }

    private fun getCurrentTime(){
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        endDate = LocalDateTime.now().format(formatter)
        startDate = LocalDateTime.now().minusDays(DATE_INTERVAL.toLong()).format(formatter)
    }
    fun registerLauncher(){

        permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){ result->
            if(result){
                //permission granted
                if(ContextCompat.checkSelfPermission(requireActivity().applicationContext,android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                    //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,10000,0f,locationListener)

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

    fun getCurrentLocation(){
        locationManager= requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

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
            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,10000,0f,locationListener)
            fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location ->
                    currentLatitude=location.latitude
                    currentLongitude=location.longitude
                    getCurrentTime()

                    println(startDate+" start date")
                    println(currentLatitude)
                    println(currentLongitude)
                    loadData()

                }
                .addOnFailureListener { exception ->

                    Toast.makeText(requireContext(),exception.localizedMessage,Toast.LENGTH_LONG).show()
                }
        }

        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,10000,0f,locationListener)
    }

    fun loadData(){

        val retrofit=Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().create(EarthquakeAPI::class.java)
//"2022-01-24","2023-01-26"
        compositeDisposable?.add(retrofit.getCurrentLocationData(currentLatitude.toString(),currentLongitude.toString(),startDate.toString(),endDate.toString())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::handleResponse))

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
            println(currentLatitude)
            println(currentLongitude)

            recyclerViewAdapter= RecyclerViewAdapter(response.features)
            recyclerView.adapter=recyclerViewAdapter

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable?.clear()
    }


}