package com.halilakpinar.earthqapp.Model

data class PropertiesModel(val mag:Double,
                           val place:String,
                           val time:Long,
                           val updated:Long,
                           var tz:String,
                           var url:String,
                           var detail:String,
                           var felt:Int,
                           var cdi:Double,
                           var mmi:Double,
                           val alert:String,
                           var status:String,
                           var tsunami:Int,
                           var sig:Int,
                           var net:String,
                           var code:String,
                           var ids:String,
                           var sources:String,
                           var types:String,
                           var nst:Int,
                           var dmin:Double,
                           var rms:Double,
                           var gap:Double,
                           var magType:String,
                           var type:String,
                           var title:String
                           )