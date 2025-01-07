package com.tis.timestampcamerafree

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Geocoder
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.common.util.concurrent.ListenableFuture
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.DexterError
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : BaseActivity() {
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imageCapture: ImageCapture
    private var camera: Camera? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var isFlashOn = false
    private lateinit var cameraPreview: PreviewView
    private lateinit var btnToggleFlash: ImageButton
    private lateinit var btnToggleCamera: ImageButton
    private lateinit var btnCapture: ImageButton
    private lateinit var tvLocation: TextView
    private lateinit var imgPreview: ImageView
    private lateinit var outputFile: File
    private val PERMISSION_REQUEST_CODE = 1001
    private var permission = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationDetails: String
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        cameraPreview = findViewById(R.id.cameraPreview)
        btnToggleFlash = findViewById(R.id.btnToggleFlash)
        btnToggleCamera = findViewById(R.id.btnToggleCamera)
        btnCapture = findViewById(R.id.btnCapture)
        tvLocation = findViewById(R.id.tvLocation)
        imgPreview = findViewById(R.id.imgPreview)

        showPermission()

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnCapture.setOnClickListener {
            // Capture photo logic
            capturePhotoWithAddress()
        }

        btnToggleFlash.setOnClickListener {
            // Flash toggle logic
            toggleFlash()
        }

        btnToggleCamera.setOnClickListener {
            // Switch front/back camera logic
            switchCamera()
        }

        imgPreview.setOnClickListener {
            openPhotoInGallery()
        }
    }

    private fun fetchLocation(tvLocation: TextView) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 5)
                if (addresses!!.isNotEmpty()) {
                    //address = addresses[0].getAddressLine(0) ?: "Unknown Location"
                    val address = addresses[0]
                    val dateTime =
                        SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())
                    locationDetails = """
                    $dateTime
                    ${address.subLocality ?: ""}
                    ${address.locality ?: ""},
                    ${address.subAdminArea ?: ""}
                    ${address.adminArea ?: ""} ${address.countryName ?: ""} ${address.postalCode ?: ""}
                    Lat: ${location.latitude}
                    Lng: ${location.longitude}
                """.trimIndent()

                    latitude = location.latitude
                    longitude = location.longitude

                }
            } else {
                locationDetails = "Unknown Location"
            }
            tvLocation.text = locationDetails
        }.addOnFailureListener { e ->
            locationDetails = "Location not available"
        }
    }

    private fun toggleFlash() {
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            if (isFlashOn) {
                btnToggleFlash.setImageResource(R.drawable.baseline_flash_off_24)
            } else {
                btnToggleFlash.setImageResource(R.drawable.baseline_flash_on_24)
            }

            isFlashOn = !isFlashOn
            camera?.cameraControl?.enableTorch(isFlashOn)

        } else {
            Toast.makeText(this, "No flash available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // Restart the camera with the new selector
        startCamera()
    }

    private fun startCamera() {
        fetchLocation(tvLocation)
        // Get an instance of ProcessCameraProvider
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Add a listener to get the ProcessCameraProvider when it's ready
        cameraProviderFuture.addListener({
            try {
                // Get the ProcessCameraProvider instance
                val cameraProvider = cameraProviderFuture.get()

                // Create a preview use case
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(cameraPreview.surfaceProvider)  // Set the surface provider for the preview
                    }

                // Create an ImageCapture use case
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Unbind all previous use cases and bind the new ones to the lifecycle
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))  // Execute the listener on the main thread
    }

    private fun createFile(): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(storageDir, "TimeStamp_${System.currentTimeMillis()}.jpg")
    }

    private fun openPhotoInGallery() {
        val photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            outputFile
        )

        // You can now use the `photoUri` (content://) in an Intent
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(photoUri, "image/jpeg")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    companion object {
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    // This function will check if all permissions are granted
    private fun allPermissionsGranted(): Boolean {
        // Check if all permissions are granted
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // This function will check permissions and request them if needed
    private fun checkAndRequestPermissions() {
        if (!allPermissionsGranted()) {
            // Request permissions if any permission is not granted
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, proceed with your camera logic
                startCamera()
            } else {
                // Permissions denied, show a message or handle accordingly
                Toast.makeText(this, "Permissions required for camera", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPermission() {
        if (permission == 1) {
            openNotification()
        } else if (permission == 2) {
            openLocation()
        } else {
            // Request permissions
            checkAndRequestPermissions()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Create channel to show notifications.
                val channelId = getString(R.string.default_notification_channel_id)
                val channelName = getString(R.string.default_notification_channel_name)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        channelName,
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                )
            }
            Log.i("TAG", "all Permission Granted: ")
        }
    }

    private fun openNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
                permission = 2
                showPermission()
            } else { // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            permission = 2
            showPermission()
        }
    }

    private fun openLocation() {
        Dexter.withContext(this) // below line is use to request the number of permissions which are required in our app.
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) // after adding permissions we are calling an with listener method.
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(
                    multiplePermissionsReport: MultiplePermissionsReport
                ) { // this method is called when all permissions are granted
                    if (multiplePermissionsReport.areAllPermissionsGranted()) { // do you work now
                        startUpdates()
                        permission = 3
                        showPermission()
                    } else {
                        permission = 3
                        showPermission()
                    }
                }


                override fun onPermissionRationaleShouldBeShown(
                    list: List<PermissionRequest>,
                    permissionToken: PermissionToken
                ) { // this method is called when user grants some permission and denies some of them.
                    permissionToken.continuePermissionRequest()
                }


            }).withErrorListener { error: DexterError? ->
                permission = 3
                showPermission()
            } // below line is use to run the permissions on same thread and to check the permissions
            .onSameThread().check()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            permission = 2
            showPermission()
        } else {
            permission = 2
            showPermission()
        }
    }

    private fun capturePhotoWithAddress() {
        val photoFile = createFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Decode the captured image into a bitmap
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(mutableBitmap)

                    // Configure paint for text overlay
                    val paint = Paint().apply {
                        color = Color.WHITE
                        textSize = 60f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }

                    // Add address overlay text
                    val lines = locationDetails.split("\n")
                    var yPosition = mutableBitmap.height - 200f // Start from the bottom
                    for (line in lines.reversed()) {
                        canvas.drawText(line, 20f, yPosition, paint)
                        yPosition -= 50f // Move up for each line
                    }

                    // Save the modified image to the gallery
                    saveToGalleryWithLocation(
                        mutableBitmap,
                        latitude,
                        longitude
                    ).toString()

                    // Display the thumbnail in the ImageView
                    imgPreview.setImageBitmap(mutableBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    private fun saveToGalleryWithLocation(bitmap: Bitmap, latitude: Double, longitude: Double) {
        val filename = "TimeStamp_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
        }

        val contentUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (contentUri != null) {
            // Save the bitmap to the specified content URI
            contentResolver.openOutputStream(contentUri).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream!!)
            }

            // Add location metadata to the saved photo
            val photoPath = getFilePathFromUri(contentUri)
            outputFile = File(photoPath.toString())
            if (photoPath != null) {
                saveLocationToExif(photoPath, latitude, longitude)
            }

            //Toast.makeText(this, "Photo saved to gallery with location", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLocationToExif(filePath: String, latitude: Double, longitude: Double) {
        try {
            val exif = ExifInterface(filePath)

            // Convert latitude and longitude to EXIF format
            val latValue = convertToExifFormat(latitude)
            val lonValue = convertToExifFormat(longitude)

            // Set GPS latitude
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latValue)
            exif.setAttribute(
                ExifInterface.TAG_GPS_LATITUDE_REF,
                if (latitude >= 0) "N" else "S"
            )

            // Set GPS longitude
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, lonValue)
            exif.setAttribute(
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                if (longitude >= 0) "E" else "W"
            )

            // Save the updated Exif data
            exif.saveAttributes()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun convertToExifFormat(value: Double): String {
        val degrees = Math.abs(value).toInt()
        val minutes = ((Math.abs(value) - degrees) * 60).toInt()
        val seconds = (((Math.abs(value) - degrees) * 60 - minutes) * 60 * 1000).toInt()

        return "$degrees/1,$minutes/1,$seconds/1000"
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        var filePath: String? = null
        contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    filePath = cursor.getString(columnIndex)
                }
            }
        return filePath
    }
}