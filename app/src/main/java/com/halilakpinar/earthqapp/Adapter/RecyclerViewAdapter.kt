package com.halilakpinar.earthqapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.halilakpinar.earthqapp.Model.AfadEarthquake
import com.halilakpinar.earthqapp.R
import kotlinx.android.synthetic.main.recycler_row.view.*
import java.text.SimpleDateFormat

class RecyclerViewAdapter(val obj:List<AfadEarthquake>): RecyclerView.Adapter<RecyclerViewAdapter.RowHolder>() {

    class RowHolder(view:View): RecyclerView.ViewHolder(view) {

        fun bindAfad(earthquake: AfadEarthquake){

            itemView.textViewDepth.text="DEPTH: "+earthquake.depth+" KM"
            itemView.textViewMag.text="MAGNITUDE: "+earthquake.magnitude
            itemView.textViewLocation.text="LOCATION: "+earthquake.location
            itemView.textViewTime.text= "DATE: ${earthquake.date}"
            itemView.textViewCoor.text="COORDINATES: "+earthquake.latitude+"-"+earthquake.longitude
            itemView.textViewAddress.text="DISTRICT: "+earthquake.district+"/"+earthquake.province

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_row,parent,false)
        return RowHolder(view)
    }

    override fun getItemCount(): Int {
        return obj.size
    }

    override fun onBindViewHolder(holder: RowHolder, position: Int) {
        holder.bindAfad(obj[position])
    }
}