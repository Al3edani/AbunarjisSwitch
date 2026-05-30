package com.abunarjis

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var switcher: LocaleSwitcher
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var btnSwitch: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var infoText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        switcher = LocaleSwitcher(this)
        buildUI()
        showCurrentStatus()
    }

    private fun buildUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF0D1117.toInt())
            setPadding(48, 48, 48, 48)
        }
        val title = TextView(this).apply {
            text = "Abunarjis DiLink"
            textSize = 28f
            setTextColor(0xFF00FF9D.toInt())
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        root.addView(title)
        val subtitle = TextView(this).apply {
            text = "Arabic Language Switcher"
            textSize = 14f
            setTextColor(0xFF6B7280.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 32)
        }
        root.addView(subtitle)
        infoText = TextView(this).apply {
            text = "جاري قراءة النظام..."
            textSize = 13f
            setTextColor(0xFF9CA3AF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        root.addView(infoText)
        btnSwitch = Button(this).apply {
            text = "تحويل للعربية 🔄"
            textSize = 18f
            setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFF00FF9D.toInt())
            setPadding(64, 24, 64, 24)
            setOnClickListener { startSwitch() }
        }
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 16, 0, 16) }
        root.addView(btnSwitch, btnParams)
        progressBar = ProgressBar(this).apply { visibility = View.GONE }
        root.addView(progressBar)
        statusText = TextView(this).apply {
            text = ""
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 8)
        }
        root.addView(statusText)
        resultText = TextView(this).apply {
            text = ""
            textSize = 12f
            setTextColor(0xFF6B7280.toInt())
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }
        root.addView(resultText)
        setContentView(root)
    }

    private fun showCurrentStatus() {
        val real = switcher.getCurrentLocale()
        val claimed = switcher.getBYDClaimedLocale()
        infoText.text = "اللغة الفعلية: $real\nما تدّعيه BYD: $claimed"
        infoText.setTextColor(
            if (real.startsWith("ar")) 0xFF00FF9D.toInt() else 0xFFFF9500.toInt()
        )
    }

    private fun startSwitch() {
        btnSwitch.isEnabled = false
        progressBar.visibility = View.VISIBLE
        statusText.text = "⚙️ جاري التحويل..."
        statusText.setTextColor(0xFFFFD60A.toInt())
        resultText.text = ""
        Thread {
            val results = mutableListOf<Pair<String, Boolean>>()
            results.add("Shell Settings" to switcher.methodShellSettings())
            results.add("ActivityManager" to switcher.methodActivityManagerLegacy())
            results.add("SystemProperties" to switcher.methodSystemProperties())
            val anySuccess = results.any { it.second }
            val isArabic = switcher.getCurrentLocale().startsWith("ar")
            Handler(Looper.getMainLooper()).post {
                progressBar.visibility = View.GONE
                btnSwitch.isEnabled = true
                resultText.text = results.joinToString("\n") { (name, ok) ->
                    "${if (ok) "✓" else "✗"} $name"
                }
                if (isArabic || anySuccess) {
                    statusText.text = "✅ تم التحويل للعربية!"
                    statusText.setTextColor(0xFF00FF9D.toInt())
                    Handler(Looper.getMainLooper()).postDelayed({ recreate() }, 2000)
                } else {
                    statusText.text = "⚠️ يحتاج صلاحيات إضافية"
                    statusText.setTextColor(0xFFFF453A.toInt())
                }
                showCurrentStatus()
            }
        }.start()
    }
}