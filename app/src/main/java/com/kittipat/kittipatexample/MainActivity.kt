package com.kittipat.kittipatexample

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import butterknife.BindView
import butterknife.ButterKnife
import com.esri.android.map.GraphicsLayer
import com.esri.android.map.MapView
import com.esri.android.map.event.OnStatusChangedListener
import com.esri.core.geometry.CoordinateConversion
import com.esri.core.geometry.Point
import com.esri.core.geometry.SpatialReference
import com.esri.core.map.Graphic
import com.esri.core.symbol.PictureMarkerSymbol
import com.panoramagl.PLImage
import com.panoramagl.PLManager
import com.panoramagl.PLSphericalPanorama
import com.panoramagl.utils.PLUtils
import android.widget.Toast


class MainActivity : AppCompatActivity() , SensorEventListener {

    @BindView(R.id.map)var mMapView : MapView? = null
    @BindView(R.id.imvCompass)var imvCompass : ImageView? = null

    private var imvCurrentPin : ImageView? = null

    private val lat = 13.904727
    private val lon = 100.4476343
    private var plManager: PLManager? = null
    private val layerArrow = GraphicsLayer()
    private var pmsArrow: PictureMarkerSymbol? = null
    private var graphicArrow: Graphic? = null
    private var idGraphicArrow: Int = 0
    private var degree : Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        plManager = PLManager(this)
        plManager!!.setContentView(R.layout.activity_main)
        plManager!!.onCreate()
        ButterKnife.bind(this)

        checkNetwork()
        setLayoutMap()
        setLayoutPanorama()
        registerSensor()
        addPin()
        zoomToPin()

        //เมื่อคลิกที่ปุ่ม CurrentPin จะทำการ plan map ไปยัง Pin
        imvCurrentPin!!.setOnClickListener {
            mMapView!!.centerAndZoom(lat, lon, 15f)
        }
    }

    //ตรวจสอบการเชื่อมต่อ Internet
    private fun checkNetwork() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        if(netInfo != null && netInfo.isConnectedOrConnecting){
           Log.d("TAG","Connected")
        }else{
            Toast.makeText(applicationContext,"No connection internet",Toast.LENGTH_SHORT).show()
        }
    }

    // ซูมไปยัง Pin
    private fun zoomToPin() {
        mMapView!!.onStatusChangedListener = OnStatusChangedListener { o, status ->
            if (status == OnStatusChangedListener.STATUS.LAYER_LOADED) {
                mMapView!!.centerAndZoom(lat, lon, 15f)
            }
        }
    }

    //เพิ่ม Pin ไปยัง latitude longitude ที่กำหนด
    private fun addPin() {
        pmsArrow = PictureMarkerSymbol(this@MainActivity,
                resources.getDrawable(R.drawable.ic_navigation_black_24dp))
        var point = Point(lon, lat)
        val strPoint = CoordinateConversion.pointToDecimalDegrees(point,
                SpatialReference.create(SpatialReference.WKID_WGS84), 7)
        point = CoordinateConversion.decimalDegreesToPoint(strPoint,
                SpatialReference.create(SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE))
        graphicArrow = Graphic(point, pmsArrow)
        idGraphicArrow = layerArrow.addGraphic(graphicArrow)
        mMapView!!.addLayer(layerArrow)
    }

    // กำหนด Layout การแสดงภาพ Panorama
    private fun setLayoutPanorama() {
        val panorama = PLSphericalPanorama()
        panorama.camera.lookAt(0.0f, 90.0f)
        panorama.setImage(PLImage(PLUtils.getBitmap(this, R.drawable.streetview1), false))
        plManager?.panorama = panorama
    }

    // กำหนดชนิดของ Sensor
    private fun registerSensor() {
        val sensorManager: SensorManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_FASTEST)
    }

    // กำหนด Layout ของแผนที่
    private fun setLayoutMap() {
        mMapView = findViewById(R.id.map) as MapView
        imvCompass = findViewById(R.id.imvCompass) as ImageView
        imvCurrentPin = findViewById(R.id.imvCurrentPin) as ImageView
        var lyMap : RelativeLayout = findViewById(R.id.lyMap) as RelativeLayout
        lyMap.layoutParams.height = getResolution()
        mMapView!!.enableWrapAround(true)
        mMapView!!.isAllowRotationByPinch = true
        mMapView!!.rotationAngle = 0.0
    }

    // Get resolution ของหน้าจอออกมาเเละ หาร 2 กับความสูงเพื่อที่จะนำค่าความสูงที่มีขนาดครึ่งหน้าจอ ไป set LayoutMap
    private fun getResolution(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        return height / 2
    }

    // กำหนดค่าองศาให้กับ Pin และ เข็มทิศ ให้ตรงกับ Sensor ของโทรศัพท์
    private fun setDegreePin(event: SensorEvent) {
        degree = Math.round(event.values[0]).toFloat()
            try {
                pmsArrow!!.angle = (degree - mMapView!!.rotationAngle).toFloat()
                layerArrow.updateGraphic(idGraphicArrow, pmsArrow)
                imvCompass!!.rotation = (degree - mMapView!!.rotationAngle).toFloat()
            } catch (e: Exception) {
                e.printStackTrace()
            }
    }

    override fun onResume() {
        super.onResume()
        plManager?.onResume()
    }

    override fun onPause() {
        plManager?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        plManager?.onDestroy()
        super.onDestroy()
    }

    //ส่งชนิดของ Sensor ไปยัง Lib ที่ใช้เพื่อแสดงผลในการแสดงภาพ Panorama ตามองศาของ Sensor
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
            plManager?.panorama?.camera?.lookAt(this, 0.0f, event.values[0] + 180)
            setDegreePin(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    // เมื่อกดปุ่ม Back บนแอนดรอยด์ จะให้ plManager clear ค่าออกก่อนที่จะปิดโปรแกรม
    override fun onBackPressed() {
        plManager!!.onDestroy()
        plManager!!.clear()
        super.onBackPressed()
        finish()
    }
}

