package com.siddhesh.myweather.POJO

import com.google.gson.annotations.SerializedName

data class Weather (
    @SerializedName("id") val id:Int,
    @SerializedName("id1") val main:String,
    @SerializedName("id2") val description:String,
    @SerializedName("id3") val icon:String,
        )