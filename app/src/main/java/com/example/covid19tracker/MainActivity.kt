package com.example.covid19tracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.ticker.TickerUtils
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL = "https://covidtracking.com/api/v1/"
private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {
    private val ALL_STATE: String = "All (Nationwide)"
    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH-mm-ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidService = retrofit.create(CovidService::class.java)
        //Fetch the national data
        covidService.getNationalData().enqueue(object:Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.e(TAG, "onResponse$response")
                val nationalData = response.body()
                if (nationalData == null) {
                    Log.w(TAG, "DID NOT RECEIVE A VALID RESPONSE BODY?")
                    return
                }
                setupEventLisenter()
                nationalDailyData = nationalData.reversed()
                Log.i(TAG, "update the graph")
                updateDisplayWithData(nationalDailyData)
            }


            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure$t")
            }


        })

        //update the graph

        covidService.getStatesData().enqueue(object:Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.e(TAG, "onResponse$response")
                val stateData = response.body()
                if (stateData == null) {
                    Log.w(TAG, "DID NOT RECEIVE A VALID RESPONSE BODY?")
                    return
                }
                perStateDailyData = stateData.reversed().groupBy{it.state}
                Log.i(TAG, "update spinner with state label")

                updateSpinnerWithStateData(perStateDailyData.keys)
            }


            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure$t")
            }


        })
    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
        val stateAbbrList = stateNames.toMutableList()
        stateAbbrList.sort()
        stateAbbrList.add(0, ALL_STATE)

        spinnerSelect.attachDataSource(stateAbbrList)
        spinnerSelect.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val selectedState = parent.getItemAtPosition(position) as String
            val selectedData = perStateDailyData[selectedState] ?: nationalDailyData
            updateDisplayWithData(selectedData)
        }

    }

    private fun setupEventLisenter() {

        tickerView.setCharacterLists(TickerUtils.provideAlphabeticalList())

        //add a listener for the user scrubbing on the chart
        sparkView.isScrubEnabled = true

        sparkView.setScrubListener {  itemData ->
            if (itemData is CovidData) {
                updateInfoForDate(itemData)
            }
        }

        //respond to radio button selected events
        radioGroupTimeSelection.setOnCheckedChangeListener { _, checkedId ->
            adapter.daysApp = when (checkedId) {
                R.id.radioButtonWeek -> TimeScale.WEEK
                R.id.radioButtonMonth -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
        }
        
        radioGroupMetricSelection.setOnCheckedChangeListener { group, checkedId -> 
            when (checkedId) {
                R.id.radioButtonPositive -> updateDisplayMetric(Metrics.POSITIVE)
                R.id.radioButtonNegative -> updateDisplayMetric(Metrics.NEGATIVE)
                R.id.radioButtonDeath -> updateDisplayMetric(Metrics.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metrics) {

        val colorRes = when (metric) {
            Metrics.NEGATIVE -> R.color.colorNegative
            Metrics.POSITIVE -> R.color.colorPositive
            Metrics.DEATH -> R.color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this, colorRes)
        sparkView.lineColor = colorInt
        tickerView.setTextColor(colorInt)

        adapter.metric = metric
        adapter.notifyDataSetChanged()

        updateInfoForDate(currentlyShownData.last())

    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData
        //Create a mew SparkAdapter with the data
        adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        //update radio buttons
        //DISPLAY metric
        radioButtonPositive.isChecked = true
        radioButtonMax.isChecked = true

        updateDisplayMetric(Metrics.POSITIVE)

    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when (adapter.metric) {
            Metrics.POSITIVE -> covidData.positiveIncrease
            Metrics.NEGATIVE -> covidData.negativeIncrease
            Metrics.DEATH -> covidData.deathIncrease
        }
        tickerView.text = NumberFormat.getInstance().format( numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)
    }
}