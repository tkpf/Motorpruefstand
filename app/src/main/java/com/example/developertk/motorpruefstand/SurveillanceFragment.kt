package com.example.developertk.motorpruefstand

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_surveillance.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking



//Data to generate charts
//from type parcelable to transport data to other fragment
@Parcelize
public class DataClass(var data: ArrayList<Array<Double>>? = arrayListOf()): Parcelable {}

/**
 * A simple [Fragment] subclass.
 * Use the [SurveillanceFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SurveillanceFragment : Fragment(){
    private val myData = DataClass()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val inflateView = inflater.inflate(R.layout.fragment_surveillance, container, false)
        val btstartDiagrammFrag = inflateView.findViewById<Button>(R.id.btGenerate)
        btstartDiagrammFrag.setOnClickListener(View.OnClickListener {
            //create coroutine (kotlin)/ new thread
            GlobalScope.launch {
                updateBluetoothData()
            }
            //moved to end of updateBluetoothData()
//            fragmentManager?.beginTransaction()?.apply {
//                replace(R.id.containerFrag, DiagrammFragment().apply {
//                    arguments = Bundle().apply { putParcelable("data", myData) }
//                })
//                addToBackStack(null)
//                commit()
//            }
        })

        // Inflate the layout for this fragment
        return inflateView
    }

    override fun onResume() {
        super.onResume()
//        //create coroutine (kotlin)/ new thread
//        GlobalScope.launch {
//            updateBluetoothData()
//        }

    }

    private fun updateBluetoothData() = runBlocking<Unit> {
        //https://kotlinlang.org/docs/reference/coroutines/basics.html
        val activity = activity as MainActivity?
        //establish connection
        val job_establishConnection = launch { activity?.establishConnectionToServer() }
        //wait until job is finished
        job_establishConnection.join()
        //send message
        val job_sendMessage = launch { activity?.sendMessageToServer("Hi there!\n") }
        job_sendMessage.join()
        //update bluetooth data
        //when all data is received, close socket
        //save received data
        val job_receiveData = launch { myData.data= activity?.receiveMessageFromServer() }
        job_receiveData.join()
        //start poping up with diagram fragment when all data is received
        //todo end implementation like this? alternative: button gets clickable if enough data is available
        activity?.runOnUiThread {
            fragmentManager?.beginTransaction()?.apply {
                replace(R.id.containerFrag, DiagrammFragment().apply {
                    arguments = Bundle().apply { putParcelable("data", myData) }
                })
                addToBackStack(null)
                commit()
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SurveillanceFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SurveillanceFragment()
    }

}