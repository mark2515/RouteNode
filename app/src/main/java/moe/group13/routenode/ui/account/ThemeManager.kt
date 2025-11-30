package moe.group13.routenode.ui.account

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import java.util.Calendar

object ThemeManager {

    fun applyAutoTheme(context: Context) {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION

        val hasPermission =
            ContextCompat.checkSelfPermission(context, fine) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, coarse) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            applyTimeBasedTheme()
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(context)

        try {
            client.lastLocation
                .addOnSuccessListener {
                    applyTimeBasedTheme()
                }
                .addOnFailureListener {
                    applyTimeBasedTheme()
                }
        } catch (e: SecurityException) {
            applyTimeBasedTheme()
        }
    }

    // Light from 7AM–7PM, Dark otherwise
    fun applyTimeBasedTheme() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isDay = hour in 7..19

        if (isDay) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}
