package com.example.developertk.motorpruefstand

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.fragment_diagramm.*


/**
 * A simple [Fragment] subclass.
 * Use the [DiagrammFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DiagrammFragment : Fragment() {
    //https://github.com/PhilJay/MPAndroidChart
    private var myData: DataClass? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            myData = it.getParcelable("data")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_diagramm, container, false)
        val chart_VI = view.findViewById<LineChart>(R.id.chart_current_voltage)
        chart_VI.data = generateCurrentTimeData()
        chart_VI.invalidate() //refresh
        // Inflate the layout for this fragment
        val chart_Mw = view.findViewById<LineChart>(R.id.chart_speed_torque)
        chart_Mw.apply {
            data = generateSpeedTorqueData()
            //todo implement color
        }
        chart_Mw.invalidate()
        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param parcelable with torque-speed data
         * @return A new instance of fragment DiagrammFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(parcelable: Parcelable) =
            DiagrammFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("data", parcelable)
                }
            }
    }

    //test data
    private fun getDataAsLineData() : LineData {
        var dataX: FloatArray= floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f)
        var dataY: FloatArray= floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f)

        var entries = ArrayList<Entry>()

        for (i in dataX.indices) {
            //turn the data into Entry objects
            entries.add(Entry(dataX[i], dataY[i]))
        }
        //create dataset for individual styling
        var dataSet = LineDataSet(entries, "Voltage-Current")
        dataSet.setColor(R.color.design_default_color_error)
        dataSet.setValueTextColor(R.color.white)
        //Create LineData and set data to chart
        return LineData(dataSet)

    }

    private fun generateCurrentTimeData(): LineData {
        val dataChunks = myData?.data
        //generate and fill entries
        val entries = ArrayList<Entry>().apply {
            for (i in dataChunks!!.indices){
                add(Entry((i*50).toFloat(), dataChunks[i][1].toFloat()))
            }
        }
        //create dataset for styling
        val dataSet = LineDataSet(entries, "characteristic Speed-Torque-Line").apply {
            setColor(R.color.design_default_color_error)
            setValueTextColor(R.color.white)
        }
        return LineData(dataSet)
    }

    private fun generateSpeedTorqueData(): LineData {
        val dataChunks = myData?.data
        //generate and fill entries
        val entries = ArrayList<Entry>().apply {
            for (i in dataChunks!!.indices){
                add(Entry(dataChunks[i][2].toFloat(), dataChunks[i][3].toFloat()))
            }
        }
        //create dataset for styling
        val dataSet = LineDataSet(entries, "characteristic Speed-Torque-Line").apply {
            setColor(R.color.design_default_color_error)
            setValueTextColor(R.color.white)
        }
        return LineData(dataSet)
    }

}