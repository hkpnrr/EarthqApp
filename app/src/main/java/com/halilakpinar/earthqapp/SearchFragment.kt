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
import com.halilakpinar.earthqapp.Adapter.showCustomToast
import com.halilakpinar.earthqapp.Model.AfadEarthquake
import com.halilakpinar.earthqapp.Service.AfadAPI
import com.halilakpinar.earthqapp.Settings.Constants
import com.halilakpinar.earthqapp.Settings.Constants.COORDINATE_INTERVAL
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_home_map.progressBarMap
import kotlinx.android.synthetic.main.fragment_search.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class SearchFragment : Fragment() , GoogleMap.OnMapLongClickListener{

    private lateinit var mapFragment: SupportMapFragment
    private lateinit var mMap: GoogleMap
    private var compositeDisposable: CompositeDisposable?=null
    private var selectedLatitude:Double?=null
    private var selectedLongitude:Double?=null
    private var minLatitude:Double?=null
    private var minLongitude:Double?=null
    private var maxLatitude:Double?=null
    private var maxLongitude:Double?=null
    private var selectedDate:LocalDate?=null
    private var selectedMagnitude:String?=null
    private var startDate:String?=null
    private var endDate:String?=null
    private lateinit var dataList: List<AfadEarthquake>

    private val callback = OnMapReadyCallback { googleMap ->

        mMap=googleMap
        googleMap.setOnMapLongClickListener(this)
        val center = LatLng(0.0, 0.0)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center,1f))
    }


    private val callbackSearch = OnMapReadyCallback { googleMap ->

        mMap=googleMap
        googleMap.setOnMapLongClickListener(this)

        if(dataList.isEmpty()){
            Toast(requireContext()).showCustomToast ("Not Found Any Earthquake", requireActivity())

        }
        for (feature: AfadEarthquake in dataList){
            googleMap.addMarker(MarkerOptions().position(LatLng(feature.latitude.toDouble(),feature.longitude.toDouble())).title(feature.eventID))
        }

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(selectedLatitude!!,
            selectedLongitude!!
        ),7f))

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
    override fun onMapLongClick(p0: LatLng) {
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(p0).title("Selected Area"))
        selectedLatitude=p0.latitude
        selectedLongitude=p0.longitude
        minLatitude= selectedLatitude!! -COORDINATE_INTERVAL
        maxLatitude= selectedLatitude!! +COORDINATE_INTERVAL
        minLongitude= selectedLongitude!! -COORDINATE_INTERVAL
        maxLongitude= selectedLongitude!! +COORDINATE_INTERVAL
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
            endDate=selectedDate?.minusDays(-2).toString()
            startDate=selectedDate?.minusDays(2).toString()

        }

        buttonSearch.setOnClickListener {
            searchEarthquakes()
        }
    }

    fun searchEarthquakes(){
        if(editTextMagnitude.text.toString()!="" &&
            selectedDate!=null && selectedLatitude!=null && selectedLongitude!=null){

            selectedMagnitude=editTextMagnitude.text.toString()

            showProgressBar()
            loadData()

        }
        else{
            Toast(requireContext()).showCustomToast ("Enter inputs properly!", requireActivity())

        }
    }

    private fun handleError(t: Throwable) {
        Toast(requireContext()).showCustomToast ("Unexpected Error! Please try again. Error: "+t.localizedMessage, requireActivity())

    }

    fun showProgressBar(){
        progressBarMap.visibility=View.VISIBLE
    }

    fun hideProgressBar(){
        progressBarMap.visibility=View.GONE
    }

    fun loadData(){

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(Constants.TIME_OUT.toLong(), TimeUnit.SECONDS)
            .readTimeout(Constants.TIME_OUT.toLong(), TimeUnit.SECONDS)
            .writeTimeout(Constants.TIME_OUT.toLong(), TimeUnit.SECONDS)
            .build()

        val retrofit=Retrofit.Builder()
            .baseUrl(Constants.BASE_URL_AFAD)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().create(AfadAPI::class.java)

        compositeDisposable?.add(retrofit.getSearchEarthquakesNew(minLatitude.toString(),maxLatitude.toString(),minLongitude.toString(),maxLongitude.toString(),startDate.toString(),endDate.toString(), selectedMagnitude.toString())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({handleResponse(it)},{handleError(it)}))

    }

    private fun handleResponse(response:List<AfadEarthquake>){
        response?.let {
            hideProgressBar()

            if(response.isEmpty()){
                Toast(requireContext()).showCustomToast ("Not Found Any Earthquake", requireActivity())
            }

            dataList=response
            mapFragment?.getMapAsync(callbackSearch)

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable?.clear()
    }


}