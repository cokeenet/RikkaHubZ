package me.rerere.rikkahub.data.ai.tools.local

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import me.rerere.ai.core.InputSchema
import me.rerere.ai.core.Tool
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.event.AppEvent
import me.rerere.rikkahub.data.event.AppEventBus
import me.rerere.rikkahub.utils.hasUsageStatsPermission
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

internal fun buildScreenTimeTool(context: Context, eventBus: AppEventBus): Tool = Tool(
    name = "get_screen_time",
    description = """
        Get the user's app screen usage over a time range.
        Specify a custom interval with begin/end, or use the range preset: today or week.
        Returns total foreground time and a per-app breakdown sorted by usage time.
        The device timezone is '${ZoneId.systemDefault()}' (UTC offset ${OffsetDateTime.now().offset});
        times without an explicit offset are interpreted in this timezone.
        Requires Android Usage access; if missing, the system settings page is opened.
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("begin", buildJsonObject {
                    put("type", "string")
                    put("description", "Start time. ISO date, local date-time, offset date-time, Instant, or epoch milliseconds.")
                })
                put("end", buildJsonObject {
                    put("type", "string")
                    put("description", "End time. Defaults to now.")
                })
                put("range", buildJsonObject {
                    put("type", "string")
                    put("enum", buildJsonArray {
                        add("today")
                        add("week")
                    })
                    put("description", "Preset used when begin is omitted. Default today.")
                })
                put("top", buildJsonObject {
                    put("type", "integer")
                    put("description", "Maximum number of top apps to return. Default 10, maximum 50.")
                })
            }
        )
    },
    execute = {
        if (!context.hasUsageStatsPermission()) {
            eventBus.emit(AppEvent.OpenUsageAccessSettings)
            return@Tool listOf(
                UIMessagePart.Text(
                    buildJsonObject {
                        put("error", "NO_PERMISSION")
                        put("message", "Usage access permission is not granted. The system settings page has been opened.")
                    }.toString()
                )
            )
        }

        val params = it.jsonObject
        val top = params["top"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()?.coerceIn(1, 50) ?: 10

        val now = ZonedDateTime.now()
        val zone = now.zone
        val beginRaw = params["begin"]?.jsonPrimitive?.contentOrNull
        val endRaw = params["end"]?.jsonPrimitive?.contentOrNull
        val rangePreset = params["range"]?.jsonPrimitive?.contentOrNull ?: "today"

        val endTime: ZonedDateTime
        val startTime: ZonedDateTime
        try {
            endTime = endRaw?.let { raw -> parseUsageTime(raw, zone) } ?: now
            startTime = if (beginRaw != null) {
                parseUsageTime(beginRaw, zone)
            } else when (rangePreset) {
                "week" -> now.minusDays(7)
                else -> now.toLocalDate().atStartOfDay(zone)
            }
        } catch (e: Exception) {
            return@Tool listOf(
                UIMessagePart.Text(
                    buildJsonObject {
                        put("error", "INVALID_TIME")
                        put("message", e.message ?: "Invalid time format.")
                    }.toString()
                )
            )
        }

        if (!startTime.isBefore(endTime)) {
            return@Tool listOf(
                UIMessagePart.Text(
                    buildJsonObject {
                        put("error", "INVALID_RANGE")
                        put("message", "begin must be earlier than end.")
                    }.toString()
                )
            )
        }

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager
        val launcherPackages = resolveLauncherPackages(packageManager)
        val startMs = startTime.toInstant().toEpochMilli()
        val endMs = endTime.toInstant().toEpochMilli()
        val foregroundMs = computeForegroundTime(
            usageStatsManager = usageStatsManager,
            startMs = startMs,
            endMs = endMs,
            excludedPackages = launcherPackages,
        )

        val sorted = foregroundMs.entries
            .filter { entry -> entry.value > 0 }
            .sortedByDescending { entry -> entry.value }
        val totalMs = sorted.sumOf { entry -> entry.value }

        val payload = buildJsonObject {
            put("range", if (beginRaw != null || endRaw != null) "custom" else rangePreset)
            put("start", startTime.withNano(0).toString())
            put("end", endTime.withNano(0).toString())
            put("total_ms", totalMs)
            put("total_minutes", totalMs / 60000)
            put("apps", buildJsonArray {
                sorted.take(top).forEach { entry ->
                    add(buildJsonObject {
                        put("package", entry.key)
                        put("app_name", resolveAppName(packageManager, entry.key))
                        put("total_ms", entry.value)
                        put("total_minutes", entry.value / 60000)
                    })
                }
            })
        }
        listOf(UIMessagePart.Text(payload.toString()))
    }
)

private const val LOOKBACK_MS = 12L * 60 * 60 * 1000

@Suppress("DEPRECATION", "NewApi")
private fun computeForegroundTime(
    usageStatsManager: UsageStatsManager,
    startMs: Long,
    endMs: Long,
    excludedPackages: Set<String>,
): Map<String, Long> {
    val foregroundMs = HashMap<String, Long>()
    val events = usageStatsManager.queryEvents(startMs - LOOKBACK_MS, endMs)
    val event = UsageEvents.Event()
    var currentPackage: String? = null
    var currentStart = 0L

    fun settle(until: Long) {
        val packageName = currentPackage
        currentPackage = null
        if (packageName == null || packageName in excludedPackages) return
        val from = maxOf(currentStart, startMs)
        val duration = until - from
        if (duration > 0) {
            foregroundMs[packageName] = (foregroundMs[packageName] ?: 0L) + duration
        }
    }

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        when (event.eventType) {
            UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                if (event.packageName != currentPackage) {
                    settle(event.timeStamp)
                    currentPackage = event.packageName
                    currentStart = event.timeStamp
                }
            }

            UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                if (event.packageName == currentPackage) {
                    settle(event.timeStamp)
                }
            }

            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                settle(event.timeStamp)
            }
        }
    }
    settle(endMs)
    return foregroundMs
}

private fun resolveLauncherPackages(packageManager: PackageManager): Set<String> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return runCatching {
        packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }.getOrDefault(emptySet())
}

private fun resolveAppName(packageManager: PackageManager, packageName: String): String {
    return runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)
}

private fun parseUsageTime(raw: String, zone: ZoneId): ZonedDateTime {
    val text = raw.trim()
    text.toLongOrNull()?.let { return Instant.ofEpochMilli(it).atZone(zone) }
    runCatching { return OffsetDateTime.parse(text).atZoneSameInstant(zone) }
    runCatching { return Instant.parse(text).atZone(zone) }
    runCatching { return LocalDateTime.parse(text).atZone(zone) }
    runCatching { return LocalDate.parse(text).atStartOfDay(zone) }
    error("Invalid time format: '$raw'. Use ISO-8601 date/date-time or epoch milliseconds.")
}
