package com.halilakpinar.earthqapp.Model

data class NestedJSONModel(var type:String,
                            var metadata:MetadataModel,
                            var features:List<FeaturesModel>
                            )