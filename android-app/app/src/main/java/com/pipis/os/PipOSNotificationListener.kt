package com.pipis.os

import android.app.Notification
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Typeface
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator

class PipOSNotificationListener : NotificationListenerService() {
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var currentCard: View? = null
    private var dismissRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val notification = sbn.notification ?: return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val extras = notification.extras ?: return
        val message = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)
        if (message.isNullOrBlank() && sender.isNullOrBlank()) return

        val appName = runCatching {
            val info = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(sbn.packageName)

        val avatar = runCatching {
            notification.getLargeIcon()?.loadDrawable(this)
                ?: packageManager.getApplicationIcon(sbn.packageName)
        }.getOrNull()

        handler.post {
            showCard(appName, sender?.toString().orEmpty(), message?.toString().orEmpty(), avatar, notification.contentIntent)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // The PipOS card has its own short display lifetime.
    }

    private fun showCard(
        appName: String,
        sender: String,
        message: String,
        avatar: android.graphics.drawable.Drawable?,
        contentIntent: android.app.PendingIntent?
    ) {
        val wm = windowManager ?: return
        if (!android.provider.Settings.canDrawOverlays(this)) return
        removeCurrentCard()

        val accent = Color.rgb(181, 138, 255)
        val deepBlack = Color.rgb(8, 6, 13)
        val dp = resources.displayMetrics.density
        fun d(value: Int) = (value * dp).toInt()

        val outer = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(d(5), Color.argb(75, 181, 138, 255))
                cornerRadius = d(21).toFloat()
            }
            elevation = d(18).toFloat()
            clipToOutline = true
            alpha = 0f
            translationY = -d(28).toFloat()
            setPadding(d(3), d(3), d(3), d(3))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(d(13), d(12), d(15), d(12))
            background = GradientDrawable().apply {
                setColor(deepBlack)
                setStroke(d(1), accent)
                cornerRadius = d(18).toFloat()
            }
        }

        val avatarView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(39, 29, 53))
                setStroke(d(1), Color.rgb(126, 78, 190))
            }
            clipToOutline = true
            if (avatar != null) setImageDrawable(avatar)
            else setImageResource(android.R.drawable.sym_def_app_icon)
        }
        card.addView(avatarView, LinearLayout.LayoutParams(d(42), d(42)).apply {
            marginEnd = d(12)
        })

        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val appLabel = TextView(this).apply {
            text = appName
            setTextColor(accent)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            maxLines = 1
        }
        textColumn.addView(appLabel)

        if (sender.isNotBlank()) {
            val senderLabel = TextView(this).apply {
                text = sender
                setTextColor(Color.rgb(224, 207, 255))
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                maxLines = 1
            }
            textColumn.addView(senderLabel)
        }

        if (message.isNotBlank()) {
            val body = TextView(this).apply {
                text = message
                setTextColor(Color.rgb(205, 180, 242))
                textSize = 13f
                maxLines = 3
                setPadding(0, d(3), 0, 0)
            }
            textColumn.addView(body)
        }

        card.addView(textColumn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        outer.addView(card, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))

        // A short, translucent scan sweep creates the hologram entrance effect.
        val sweep = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.TRANSPARENT, Color.argb(48, 190, 125, 255), Color.TRANSPARENT)
            )
            isClickable = false
        }
        outer.addView(sweep, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            d(34),
            Gravity.TOP
        ))

        outer.setOnClickListener {
            removeCurrentCard()
            runCatching { contentIntent?.send() }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = d(30)
            width = WindowManager.LayoutParams.MATCH_PARENT
        }

        runCatching {
            wm.addView(outer, params)
            currentCard = outer
            outer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(280)
                .setInterpolator(DecelerateInterpolator())
                .start()

            outer.post {
                val travel = (outer.height - d(34)).coerceAtLeast(d(40)).toFloat()
                ValueAnimator.ofFloat(-d(34).toFloat(), travel).apply {
                    duration = 620
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { animator ->
                        sweep.translationY = animator.animatedValue as Float
                    }
                    start()
                }
            }

            val dismiss = Runnable { removeCurrentCard() }
            dismissRunnable = dismiss
            handler.postDelayed(dismiss, 5_000L)
        }.onFailure {
            currentCard = null
        }
    }

    private fun removeCurrentCard() {
        dismissRunnable?.let(handler::removeCallbacks)
        dismissRunnable = null
        val card = currentCard ?: return
        currentCard = null
        card.animate().alpha(0f).translationY(-24f).setDuration(160).withEndAction {
            runCatching { windowManager?.removeView(card) }
        }.start()
    }

    override fun onDestroy() {
        removeCurrentCard()
        super.onDestroy()
    }
}
