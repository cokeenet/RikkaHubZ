package me.rerere.rikkahub.ui.shortcut

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import me.rerere.rikkahub.R
import me.rerere.rikkahub.RouteActivity
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Avatar
import java.security.MessageDigest
import kotlin.math.max

object AssistantShortcutInstaller {
    fun isSupported(context: Context): Boolean {
        return ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    }

    fun requestPinShortcut(context: Context, assistant: Assistant) {
        if (!isSupported(context)) {
            Toast.makeText(context, R.string.assistant_page_desktop_shortcut_unsupported, Toast.LENGTH_SHORT).show()
            return
        }

        val label = assistant.name.ifBlank {
            context.getString(R.string.assistant_page_default_assistant)
        }
        val intent = Intent(context, RouteActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(RouteActivity.EXTRA_OPEN_ASSISTANT_ID, assistant.id.toString())
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val shortcut = ShortcutInfoCompat.Builder(context, "assistant_${assistant.id}")
            .setShortLabel(label.take(10).ifBlank { label })
            .setLongLabel(label)
            .setIcon(IconCompat.createWithAdaptiveBitmap(renderAvatarBitmap(context, assistant)))
            .setIntent(intent)
            .build()

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        Toast.makeText(context, R.string.assistant_page_desktop_shortcut_requested, Toast.LENGTH_SHORT).show()
    }

    private fun renderAvatarBitmap(context: Context, assistant: Assistant): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        drawBackground(canvas, rect, assistant.name.ifBlank { assistant.id.toString() })

        when (val avatar = assistant.avatar) {
            is Avatar.Image -> {
                val source = decodeImage(context, avatar.url)
                if (source != null) {
                    drawImage(canvas, source, rect)
                    if (source != bitmap && !source.isRecycled) {
                        source.recycle()
                    }
                } else {
                    drawText(canvas, rect, assistant.name.take(1).ifBlank { "?" }, textScale = 0.54f)
                }
            }

            is Avatar.Emoji -> drawText(canvas, rect, avatar.content, textScale = 0.58f)
            Avatar.Dummy -> drawText(canvas, rect, assistant.name.take(1).ifBlank { "?" }, textScale = 0.48f)
        }

        return bitmap
    }

    private fun drawBackground(canvas: Canvas, rect: RectF, seed: String) {
        val (from, to) = colorsFor(seed)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom,
                from,
                to,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(rect, rect.width() * 0.22f, rect.height() * 0.22f, paint)
    }

    private fun drawImage(canvas: Canvas, source: Bitmap, rect: RectF) {
        val path = Path().apply {
            addRoundRect(rect, rect.width() * 0.22f, rect.height() * 0.22f, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(source, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    }

    private fun drawText(canvas: Canvas, rect: RectF, text: String, textScale: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = rect.height() * textScale
            isFakeBoldText = true
        }
        val baseline = rect.centerY() - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(text, rect.centerX(), baseline, paint)
    }

    private fun decodeImage(context: Context, value: String): Bitmap? {
        return runCatching {
            val uri = Uri.parse(value)
            when (uri.scheme) {
                "content",
                "file" -> context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                null,
                "" -> BitmapFactory.decodeFile(value)
                else -> null
            }
        }.getOrNull()
    }

    private fun colorsFor(seed: String): Pair<Int, Int> {
        val bytes = MessageDigest.getInstance("SHA-1").digest(seed.toByteArray(Charsets.UTF_8))
        val a = bytes.getOrNull(0)?.toInt()?.and(0xFF) ?: 120
        val b = bytes.getOrNull(1)?.toInt()?.and(0xFF) ?: 80
        val c = bytes.getOrNull(2)?.toInt()?.and(0xFF) ?: 160
        return Color.rgb(max(48, a), max(64, b), max(80, c)) to
            Color.rgb(max(80, c), max(48, a), max(64, b))
    }
}
