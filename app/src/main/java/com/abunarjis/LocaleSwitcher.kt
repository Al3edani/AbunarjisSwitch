package com.abunarjis

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

class LocaleSwitcher(private val context: Context) {

    companion object {
        const val TARGET_LANG    = "ar"
        const val TARGET_COUNTRY = "SA"
        const val TARGET_LOCALE  = "ar-SA"
    }

    fun methodShellSettings(): Boolean {
        return try {
            val cmds = arrayOf(
                arrayOf("settings", "put", "system", "system_locales", TARGET_LOCALE),
                arrayOf("setprop", "persist.sys.locale", TARGET_LOCALE),
                arrayOf("setprop", "persist.sys.language", TARGET_LANG),
                arrayOf("setprop", "persist.sys.country", TARGET_COUNTRY)
            )
            var atLeastOne = false
            for (cmd in cmds) {
                try {
                    val p = Runtime.getRuntime().exec(cmd)
                    p.waitFor()
                    if (p.exitValue() == 0) atLeastOne = true
                } catch (_: Exception) {}
            }
            if (atLeastOne) sendLocaleChangedBroadcast()
            atLeastOne
        } catch (_: Exception) { false }
    }

    fun methodActivityManagerLegacy(): Boolean {
        return try {
            val locale = Locale(TARGET_LANG, TARGET_COUNTRY)
            val config = Configuration()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
            }
            val amNativeClass = Class.forName("android.app.ActivityManagerNative")
            val getDefault = amNativeClass.getMethod("getDefault")
            val am = getDefault.invoke(null)
            val updateConfig = am.javaClass.getMethod("updateConfiguration", Configuration::class.java)
            updateConfig.invoke(am, config)
            sendLocaleChangedBroadcast()
            true
        } catch (_: Exception) {
            methodActivityManagerNew()
        }
    }

    private fun methodActivityManagerNew(): Boolean {
        return try {
            val locale = Locale(TARGET_LANG, TARGET_COUNTRY)
            val config = Configuration()
            config.setLocales(LocaleList(locale))
            val atmClass = Class.forName("android.app.ActivityTaskManager")
            val getInstance = atmClass.getMethod("getInstance")
            val atm = getInstance.invoke(null)
            val updateConfig = atm.javaClass.getMethod("updateConfiguration", Configuration::class.java)
            updateConfig.invoke(atm, config)
            sendLocaleChangedBroadcast()
            true
        } catch (_: Exception) { false }
    }

    fun methodSystemProperties(): Boolean {
        return try {
            val spClass = Class.forName("android.os.SystemProperties")
            val setMethod = spClass.getMethod("set", String::class.java, String::class.java)
            setMethod.invoke(null, "persist.sys.locale",   TARGET_LOCALE)
            setMethod.invoke(null, "persist.sys.language", TARGET_LANG)
            setMethod.invoke(null, "persist.sys.country",  TARGET_COUNTRY)
            sendLocaleChangedBroadcast()
            true
        } catch (_: Exception) { false }
    }

    fun getCurrentLocale(): String = Locale.getDefault().toLanguageTag()

    fun getBYDClaimedLocale(): String {
        return try {
            val spClass = Class.forName("android.os.SystemProperties")
            val getMethod = spClass.getMethod("get", String::class.java, String::class.java)
            getMethod.invoke(null, "ro.product.locale", "unknown") as String
        } catch (_: Exception) { "unknown" }
    }

    private fun sendLocaleChangedBroadcast() {
        try { context.sendBroadcast(Intent(Intent.ACTION_LOCALE_CHANGED)) } catch (_: Exception) {}
    }
}