package de.felixnuesse.usbbackup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import de.felixnuesse.usbbackup.appintro.IdentifiableAppIntroFragment
import de.felixnuesse.usbbackup.appintro.SlideLeaveInterface

class IntroActivity : AppIntro(), SlideLeaveInterface {

    companion object {
        private const val SLIDE_ID_WELCOME = "SLIDE_ID_WELCOME"
        private const val SLIDE_ID_NOTIFICATIONS = "SLIDE_ID_NOTIFICATIONS"
    }

    override fun onResume() {
        enableEdgeToEdge()
        super.onResume()
        setImmersiveMode()
        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure you don't call setContentView!

        setImmersiveMode()
        showStatusBar(true)
        isWizardMode = true
        isColorTransitionsEnabled = true

        // don't allow the intro to be bypassed
        isSystemBackButtonLocked = true


        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        addSlide(
            IdentifiableAppIntroFragment.createInstance(
                title = getString(R.string.intro_slide_welcome_title),
                description = getString(R.string.intro_slide_welcome_description),
                imageDrawable = R.drawable.undraw_vault,
                backgroundColorRes = R.color.intro_color1,
                id = SLIDE_ID_WELCOME,
                callback = this
            ))


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addSlide(
                IdentifiableAppIntroFragment.createInstance(
                    title = getString(R.string.intro_slide_notification_title),
                    description = getString(R.string.intro_slide_notification_description),
                    imageDrawable = R.drawable.undraw_notify,
                    backgroundColorRes = R.color.intro_color1,
                    id = SLIDE_ID_NOTIFICATIONS,
                    callback = this
                ))
            askForPermissions(
                permissions = arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                slideNumber = 2,
                required = false)
        }

        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.intro_slide_warning_title),
                description = getString(R.string.intro_slide_warning_description),
                imageDrawable = R.drawable.icon_warning,
                backgroundColorRes = R.color.md_theme_errorContainer_mediumContrast
            ))

        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.intro_slide_done_title),
                description = getString(R.string.intro_slide_done_description),
                imageDrawable = R.drawable.undraw_completed,
                backgroundColorRes = R.color.intro_color1
            ))
    }



    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Decide what to do when the user clicks on "Skip"
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Decide what to do when the user clicks on "Done"
        endIntro()
    }


    private fun endIntro() {
        Prefs(this).finishIntro("0.0.0")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun allowSlideLeave(id: String): Boolean {
        return when(id) {
            else -> true
        }
    }

    override fun onSlideLeavePrevented(id: String) {
        when(id) {
            else -> {}
        }
    }
}