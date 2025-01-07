package com.tis.timestampcamerafree

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationRequest
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ProcessLifecycleOwner
import com.birjuvachhani.locus.Locus
import com.tis.timestampcamerafree.lifecycle.AppLifecycleListener


abstract class BaseActivity : AppCompatActivity() {
    private lateinit var mContext: Context

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onResume() {
        super.onResume()
        //if (appRepository.isLoggedIn)
        // initLocation(BaseActivity::class.java)

    }

    //Location Listener
    var isLocationStarted = false

    @RequiresApi(Build.VERSION_CODES.S)
    protected fun initLocation(cls: Class<*>?) {

        val request = LocationRequest.Builder(60000).build()
        Intent(this, cls).apply {
            putExtra("request", request)
        }
        startUpdates()
    }

    fun startUpdates() {
        isLocationStarted = true
        Locus.configure {
            enableBackgroundUpdates = false
            forceBackgroundUpdates = false
            shouldResolveRequest = true
        }
        Locus.startLocationUpdates(this) { result ->
            result.location?.let(::onLocationUpdate)
            result.error?.let(::onError)
        }
    }

    private fun onError(error: Throwable?) {
    }

    private fun onLocationUpdate(location: Location) {
        val latitude = location.latitude.toString()
        val longitude = location.longitude.toString()
    }

    override fun onPause() {
        super.onPause()
        stopUpdates()
    }

    private fun stopUpdates() {
        if (isLocationStarted)
            Locus.stopLocationUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        //ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleListener(this))
        supportActionBar?.apply {
            hide()
        }

    }

    fun hideKeyboard(activity: Activity?) {
        activity?.let {
            val imm = it.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            //Find the currently focused view, so we can grab the correct window token from it.
            var view = it.currentFocus
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = View(activity)
            }
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    open fun changeActivity(clz: Class<*>?) {
        val i = Intent(this, clz)
        changeActivity(i)
    }

    open fun changeActivityWith(clz: Class<*>?) {
        val intent = Intent(this, clz)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    open fun changeActivity(i: Intent?) {
        startActivity(i)
    }

    fun playAlertTone(context: Context, status: String) {
        val alertToneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, alertToneUri)

        // Check if the tone has already been played for this status
        ringtone.play()

        // Optional: Stop after 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            ringtone.stop()
        }, 1000)
    }

    fun redirectTo(cls: Class<*>, isFinishRequired: Boolean = true, extras: Bundle? = null) {
        startActivity(Intent(this, cls).apply {
            extras?.let { putExtras(it) }
        })
        if (isFinishRequired) finish()
    }
}