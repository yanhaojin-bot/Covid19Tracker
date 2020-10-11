package com.example.covid19tracker

import android.graphics.RectF
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter (private val dailyData: List<CovidData>) : SparkAdapter(){

    var metric = Metrics.POSITIVE
    var daysApp = TimeScale.MAX


    override fun getCount() = dailyData.size

    override fun getItem(index: Int) = dailyData[index]


    override fun getY(index: Int): Float {
        val chosenDayData = dailyData[index]
        return when (metric) {
            Metrics.NEGATIVE -> chosenDayData.negativeIncrease.toFloat()
            Metrics.POSITIVE -> chosenDayData.positiveIncrease.toFloat()
            Metrics.DEATH -> chosenDayData.deathIncrease.toFloat()
        }
        return chosenDayData.positiveIncrease.toFloat()
    }

    override fun getDataBounds(): RectF {
        val bounds = super.getDataBounds()
        if (daysApp != TimeScale.MAX) {
            bounds.left = count - daysApp.numsDays.toFloat()
        }
        return bounds
    }
}