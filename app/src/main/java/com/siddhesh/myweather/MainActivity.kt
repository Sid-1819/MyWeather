package com.siddhesh.myweather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.siddhesh.myweather.POJO.ModelClass
import com.siddhesh.myweather.Utilities.ApiUtilities
import com.siddhesh.myweather.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var activityMainBinding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        supportActionBar?.hide()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        activityMainBinding.relative.visibility = View.GONE
        getCurrentLocation()

        activityMainBinding.cityname.setOnEditorActionListener { v, actionId, keyEvent ->
            if(actionId==EditorInfo.IME_ACTION_SEARCH){
                getCityWeather(activityMainBinding.cityname.text.toString())
                val view = this.currentFocus
                if (view!=null){
                    val imm:InputMethodManager=getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken,0)
                    activityMainBinding.cityname.clearFocus()
                }
                true
            }else false
        }

    }

    private fun getCityWeather(cityName: String) {
        activityMainBinding.progressbar.visibility=View.VISIBLE
        ApiUtilities.getApiInterface()?.getCityWeatherData(cityName, api_key)?.enqueue(object :Callback<ModelClass>{
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                setDataOnViews(response.body())
            }

            override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                Toast.makeText(applicationContext, "Not a Valid City Name", Toast.LENGTH_SHORT).show()
            }

        })


    }

    private fun getCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                    return

                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        Toast.makeText(this, "NULL RECEIVED", Toast.LENGTH_SHORT).show()
                    } else {
                        //fetch weather here
                        fetchCurrentLocationWeather(
                            location.latitude.toString(),
                            location.longitude.toString()
                        )
                    }
                }
            } else {
                //open settings

                Toast.makeText(this, "turn on location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            //request
            requestPermission()
        }
    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {

        activityMainBinding.progressbar.visibility = View.VISIBLE
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude, longitude, api_key)
            ?.enqueue(object : Callback<ModelClass> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<ModelClass>, response: Response<ModelClass>) {
                    if (response.isSuccessful) {
                        setDataOnViews(response.body())
                    }
                }

                override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                    Toast.makeText(applicationContext, "ERROR", Toast.LENGTH_SHORT).show()
                }

            })

    }

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setDataOnViews(body: ModelClass?) {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm")
        val currentDate = sdf.format(Date())
        activityMainBinding.datetime.text = currentDate

        activityMainBinding.daymaxtemp.text = "Day " + kelvinToCelsius(body!!.main.temp_max) + "°"
        activityMainBinding.daytemp2.text = "Night " + kelvinToCelsius(body.main.temp_min) + "°"
        activityMainBinding.temp.text = "" + kelvinToCelsius(body.main.temp) + "°"
        activityMainBinding.feelslike.text = "" + kelvinToCelsius(body.main.feels_like) + "°"
        activityMainBinding.weathertype.text = body.weather[0].main
        activityMainBinding.txtSunrise.text = timeStampToLocalDate(body.sys.sunrise.toLong())
        activityMainBinding.txtSunset.text = timeStampToLocalDate(body.sys.sunset.toLong())
        activityMainBinding.txtPressure.text = body.main.pressure.toString()
        activityMainBinding.txtHumidity.text = body.main.humidity.toString() + " %"
        activityMainBinding.txtWindspeed.text = body.wind.speed.toString() + " m/s"
        activityMainBinding.txtTempFar.text =
            " " + ((kelvinToCelsius(body.main.temp)).times(1.8).plus(32).roundToInt())
        activityMainBinding.cityname.setText(body.name)

        updateUI(body.weather[0].id)


    }

    private fun updateUI(id: Int) {
        when (id) {
            in 200..232 -> {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.statusBarColor = resources.getColor(R.color.thunderstorm)
                activityMainBinding.relativeToolbar.setBackgroundColor(resources.getColor(R.color.thunderstorm))
                activityMainBinding.sublayout.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.thunderstrom_bg)
                activityMainBinding.llMainBgBelow.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.thunderstrom_bg)

                activityMainBinding.llMainBgAbove.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.thunderstrom_bg)

                activityMainBinding.weatherBg.setImageResource(R.drawable.thunderstrom_bg)
                activityMainBinding.weather.setImageResource(R.drawable.thunderstrom)
            }
            in 300..321 -> {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.statusBarColor = resources.getColor(R.color.drizzle)
                activityMainBinding.relativeToolbar.setBackgroundColor(resources.getColor(R.color.drizzle))
                activityMainBinding.sublayout.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.drizzle_bg)
                activityMainBinding.llMainBgBelow.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.drizzle_bg)

                activityMainBinding.llMainBgAbove.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.drizzle_bg)

                activityMainBinding.weatherBg.setImageResource(R.drawable.drizzle_bg)
                activityMainBinding.weather.setImageResource(R.drawable.drizzle)
            }
            in 500..531 -> {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.statusBarColor = resources.getColor(R.color.rain)
                activityMainBinding.relativeToolbar.setBackgroundColor(resources.getColor(R.color.rain))
                activityMainBinding.sublayout.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.rainy_bg)
                activityMainBinding.llMainBgBelow.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.rainy_bg)

                activityMainBinding.llMainBgAbove.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.rainy_bg)

                activityMainBinding.weatherBg.setImageResource(R.drawable.rainy_bg)
                activityMainBinding.weather.setImageResource(R.drawable.rain)


            }
            in 600..620 -> {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.statusBarColor = resources.getColor(R.color.snow)
                activityMainBinding.relativeToolbar.setBackgroundColor(resources.getColor(R.color.snow))
                activityMainBinding.sublayout.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.snow_bg)
                activityMainBinding.llMainBgBelow.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.snow_bg)

                activityMainBinding.llMainBgAbove.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.snow_bg)

                activityMainBinding.weatherBg.setImageResource(R.drawable.snow_bg)
                activityMainBinding.weather.setImageResource(R.drawable.snow)

            }
            in 700..781 -> {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.statusBarColor = resources.getColor(R.color.mist)
                activityMainBinding.relativeToolbar.setBackgroundColor(resources.getColor(R.color.mist))
                activityMainBinding.sublayout.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.mist_bg)
                activityMainBinding.llMainBgBelow.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.mist_bg)

                activityMainBinding.llMainBgAbove.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.mist_bg)

                activityMainBinding.weatherBg.setImageResource(R.drawable.mist_bg)
                activityMainBinding.weather.setImageResource(R.drawable.clouds)
            }
            800 -> {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.statusBarColor = resources.getColor(R.color.clear)
                activityMainBinding.relativeToolbar.setBackgroundColor(resources.getColor(R.color.clear))
                activityMainBinding.sublayout.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.clear_bg)
                activityMainBinding.llMainBgBelow.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.clear_bg)

                activityMainBinding.llMainBgAbove.background =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.clear_bg)

                activityMainBinding.weatherBg.setImageResource(R.drawable.clear_bg)
                activityMainBinding.weather.setImageResource(R.drawable.clear)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun timeStampToLocalDate(timestamp: Long): String {
        val localTime = timestamp.let {
            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }
        return localTime.toString()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )

    }

    private fun kelvinToCelsius(temp: Double): Double {
        var intTemp = temp
        intTemp = intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
        const val api_key = "5916da17b2442011014a93ba101f966a"
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}