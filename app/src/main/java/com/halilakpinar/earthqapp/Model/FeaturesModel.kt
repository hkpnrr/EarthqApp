package com.halilakpinar.earthqapp.Model

data class FeaturesModel(var type:String,
                    var properties:PropertiesModel,
                    var geometry:GeometryModel,
                    var id:String
                    )