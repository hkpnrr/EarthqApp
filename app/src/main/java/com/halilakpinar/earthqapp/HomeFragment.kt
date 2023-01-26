package com.halilakpinar.earthqapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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


        loadData()

    }



    fun loadData(){

        val retrofit=Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().create(EarthquakeAPI::class.java)

        println("enqueue öncesi")
        compositeDisposable?.add(retrofit.getData()
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