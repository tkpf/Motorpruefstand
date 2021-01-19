package com.example.developertk.motorpruefstand

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_surveillance.*
import kotlinx.coroutines.delay
import java.io.*
import java.lang.System.out
import java.util.*
import kotlin.collections.ArrayList

private const val SELECT_DEVICE_REQUEST_CODE = 300

class MainActivity : AppCompatActivity()  {

    private var btConnect: Button? = null

    private val surveillanceFragment = SurveillanceFragment()

    //setting up Fragment Manager
    private val fragmentManager = supportFragmentManager
    private val fragmentTransaction = fragmentManager.beginTransaction()

    //Bluetooth things
    private val REQUEST_ENABLE_BT: Int = 1
    private var btAdapter: BluetoothAdapter? = null
    private var btSocket: BluetoothSocket? = null
    private var outStream: OutputStream? = null
    // Well known SPP UUID
    //todo correct?
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    // Insert your server's MAC address
    private val address = "E0:94:67:36:01:09"
    //permissions
    private var allPermissionsAccepted = false
    private val permissions: Array<String> = arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
    private val requestPermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                // Do something if permission granted
                if (isGranted) {
                    Log.i("DEBUG", "permission granted")
                } else {
                    Log.i("DEBUG", "permission denied")
                }
            }
   //Bluetooth Device Manager
    //todo minSDK set to 26, implement for older versions without deviceManager
   private val deviceManager: CompanionDeviceManager by lazy(LazyThreadSafetyMode.NONE) {
       getSystemService(CompanionDeviceManager::class.java)
   }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btAdapter = BluetoothAdapter.getDefaultAdapter()

        btConnect = Button(this).apply {
            text = "connect"
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                gravity = Gravity.CENTER
            }
            setOnClickListener(View.OnClickListener {
                if (allPermissionsAccepted || handlePermissions()){
                    startConnectionDialog()
                }  else showAlertBox("Permissions required", "To connect with bluetooth device, please accept all permissons.", finish = false)
            })
        }
        containerFrag.addView(btConnect)

        //displaying Status
        setStatusText(connection = false)

    }

    private fun setStatusText(connection: Boolean, deviceName: String = "None", dataTransferOn: String = "Off") {
        //todo keep, sending.../receiving...?
        var strConnection: String? = null
        var strTransferOn: String = "Off"
        if (connection) {
            strConnection = "connected"
            strTransferOn = dataTransferOn
        } else strConnection = "unconnected"
        tvState_desc.text = "Status:\nDevice:\nData transfer on:"
        tvState_cont.text = strConnection + "\n" + deviceName + "\n" + strTransferOn
    }

    private fun showAlertBox(title: String, message: String, finish: Boolean = true) {
        AlertDialog.Builder(this).apply {
            setTitle(title)
            setMessage(message + "\n Press OK to exit")
            setPositiveButton("OK", object : DialogInterface.OnClickListener{
                override fun onClick(arg0: DialogInterface, arg1: Int) {
                    if (finish) finish()
                }
            }).show()
        }
    }

    private fun handlePermissions(): Boolean {
        var permissionsGranted = true
        //check if permissions are already granted, denied or not yet asked
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) == PERMISSION_DENIED) {
                if (shouldShowRequestPermissionRationale(permission)) {
                    //todo is showing up wrong
                            showAlertBox("permission explanation", "Bluetooth is required for a Bluetooth connection. \n Besides a Bluetooth connection does reveal information about your relative location. For this please enable location-permission.")
                        }
                requestPermission.launch(permission)
            }
            permissionsGranted = permissionsGranted && (ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED)
        }
        return permissionsGranted
    }

    private fun enableBT(): Boolean{
        if (btAdapter == null) {
            showAlertBox("Fatal Error", "Bluetooth Not supported. Aborting.")
            return false
        }
        else if (btAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        return btAdapter!!.isEnabled
    }

    private fun startConnectionDialog() {
        //enable Bluetooth
        if (enableBT()) {
            //todo start device searching
            // To skip filtering based on name and supported feature flags (UUIDs),
            // don't include calls to setNamePattern() and addServiceUuid(),
            // respectively. This example uses Bluetooth.
            val deviceFilter: BluetoothDeviceFilter = BluetoothDeviceFilter.Builder()
                   // .setNamePattern(Pattern.compile("My device"))
                   // .addServiceUuid(ParcelUuid(UUID(0x123abcL, -1L)), null)
                    .build()

            // The argument provided in setSingleDevice() determines whether a single
            // device name or a list of device names is presented to the user as
            // pairing options.
            val pairingRequest: AssociationRequest = AssociationRequest.Builder()
                    .addDeviceFilter(deviceFilter)
                    .setSingleDevice(false)
                    .build()

            // When the app tries to pair with the Bluetooth device, show the
            // appropriate pairing request dialog to the user.
            deviceManager.associate(pairingRequest,
                    object : CompanionDeviceManager.Callback() {

                        override fun onDeviceFound(chooserLauncher: IntentSender) {
                            startIntentSenderForResult(chooserLauncher,
                                    SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0)
                        }

                        override fun onFailure(error: CharSequence?) {
                            // Handle failure
                        }
                    }, null)


        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SELECT_DEVICE_REQUEST_CODE -> when(resultCode) {
                Activity.RESULT_OK -> {
                    // User has chosen to pair with the Bluetooth device.
                    val deviceToPair: BluetoothDevice? =
                            data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    deviceToPair?.createBond()
                    // ... Continue interacting with the paired device.
                    //start surveillance Fragment
                    fragmentTransaction.add(R.id.containerFrag, surveillanceFragment)
                    fragmentTransaction.commit()
                    btConnect?.visibility = View.GONE
                }

            }
            REQUEST_ENABLE_BT -> when(resultCode) {
                //BT enabling was denied
                Activity.RESULT_CANCELED ->  showAlertBox("Enable Bluetooth", "Please enable Bluetooth to use this function.", finish = false)
            }
        }
    }


    public suspend fun establishConnectionToServer(){
        // Set up a pointer to the remote node using it's address.
        val device = btAdapter!!.getRemoteDevice(address)
        out.append("\n...Remote Device is "+ device.toString())
        runOnUiThread { setStatusText(connection = true, deviceName = device.toString()) }
        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
        } catch (e: IOException) {
            showAlertBox("Fatal Error", "In establishConnectionToServer() and socket create failed: " + e.message.toString() + ".")
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter!!.cancelDiscovery()
        // Establish the connection.  This will block until it connects.
        try {
            btSocket!!.connect()
            out.append("\n...Connection established and data link opened...")
        } catch (e: IOException) {
            try {
                out.append("...Socket was closed after connection intent failed...")
                btSocket!!.close()
            } catch (e2: IOException) {
                showAlertBox("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.message + ".")
            }
        }
    }
    public suspend fun sendMessageToServer(message: String){
        // Create a data stream so we can talk to server.
        out.append("\n...Sending message to server...")
        //val message = "Hello from Android.\n"
        out.append("\n...The message that we will send to the server is: $message")
        //update status text
        val device = btAdapter?.getRemoteDevice(address).toString()
        runOnUiThread { setStatusText(connection = true, deviceName = device , dataTransferOn = "sending" ) }
        try {
            outStream = btSocket!!.outputStream
        } catch (e: IOException) {
            showAlertBox("Fatal Error", "In sendingMessageToServer(ms) and output stream creation failed:" + e.message + ".")
        }

        val msgBuffer = message.toByteArray()
        try {
            outStream!!.write(msgBuffer)
        } catch (e: IOException) {
            var msg = "In sendingMessageToServer(ms) and an exception occurred during write: " + e.message
            if (address == "00:00:00:00:00:00") msg = "$msg.\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 37 in the java code"
            msg = "$msg.\n\nCheck that the SPP UUID: $MY_UUID exists on server.\n\n"
            showAlertBox("Fatal Error", msg)
        }
    }

    /**
     * receiving messages from server and save them into arraylist
     */
    public suspend fun receiveMessageFromServer(): ArrayList<Array<Double>> {
        //https://stackoverflow.com/questions/13984142/how-to-write-array-to-outputstream-in-java
        //todo writing getMessage, sendMessages methods?
        out.append("\n...Trying to receive Message from Server...")
        //val inStream: InputStream
        //Data to generate charts
        var data: ArrayList<Array<Double>> = arrayListOf()
        try {
            val dataInStream = DataInputStream(btSocket!!.inputStream)
            val objectInStream = ObjectInputStream(dataInStream)
            val device = btAdapter?.getRemoteDevice(address).toString()
            //update status text
            runOnUiThread { setStatusText(connection = true, deviceName = device , dataTransferOn = "receiving" ) }

            //set up ui-elements to write data in
            var gauge : pl.pawelkleczkowski.customgauge.CustomGauge? = null
            var tvValue: TextView? = null

            //needs to be primitive array type because sent from java written code
            var newChunkData: Array<Double>? = null
            while (true) {
                newChunkData = objectInStream.readObject() as Array<Double>
                //end of transfer is signalized through -1 per definition
                //if not write new elements in arraylist and update UI
                if (newChunkData[0] ==-1.0) break;
                while (data.size !=0 && newChunkData == data.last()) {
                    //auf neuen Werte warten
                    delay(10)
                }
                data.add(newChunkData)
                println("New Chunk of Data:\t" + newChunkData[0].toString() +"\t" + newChunkData[1].toString() +"\t"+ newChunkData[2].toString() +"\t"+ newChunkData[3].toString())
                updateUI(newChunkData);
            }
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        if (outStream != null) {
            try {
                outStream!!.flush()
            } catch (e: IOException) {
                showAlertBox("Fatal Error", "In onPause() and failed to flush output stream: " + e.message + ".")
            }
        }
        try {
            btSocket!!.close()
            //update status text
            runOnUiThread { setStatusText(connection = false ) }

        } catch (e2: IOException) {
            showAlertBox("Fatal Error", "In onPause() and failed to close socket." + e2.message + ".")
        }
        return data
    }

    private fun updateUI(chunkData: Array<Double>) {
        //to update UI must run in UiThread
        runOnUiThread {
            //data comes in following order: voltage, current, speed, torque
            gauge_voltage.value = chunkData[0].toInt()
            valueVoltage.text = String.format("%.${1}f", chunkData[0])
            //multiplying with 10 to increase precision of scala (no double allowed)
            gauge_current.value = chunkData[1].toInt()*10
            valueCurrent.text = String.format("%.${1}f", chunkData[1])
            gauge_speed.value = chunkData[2].toInt()
            valueSpeed.text = String.format("%.${0}f", chunkData[2])
            gauge_torque.value = chunkData[3].toInt() *100
            valueTorque.text = String.format("%.${1}f", chunkData[3])

            var efficiency: Int = (100*2*3.14 * chunkData[2]*chunkData[3]/chunkData[0]/chunkData[1]).toInt()
            progressBarEfficiency.progress = efficiency
            if (efficiency>100) efficiency = 100
            tvEfficiencyPerc.text = efficiency.toString() + "%"
        }
    }


}