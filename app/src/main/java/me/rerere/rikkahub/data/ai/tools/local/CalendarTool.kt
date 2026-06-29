package me.rerere.rikkahub.data.ai.tools.local

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

internal fun buildCalendarQueryTool(context: Context): Tool = Tool(
    name = "calendar_query",
    description = """
        Query calendar events on the user's device within a time range.
        Specify a custom interval with begin/end, or use range: today, week, or month.
        Returns events with title, description, location, start/end times, all-day flag, and calendar name.
        The device timezone is '${ZoneId.systemDefault()}' (UTC offset ${OffsetDateTime.now().offset});
        times without an explicit offset are interpreted in this timezone.
        Requires READ_CALENDAR permission.
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
                    put("description", "End time. Defaults to the end of the selected range.")
                })
                put("range", buildJsonObject {
                    put("type", "string")
                    put("enum", buildJsonArray {
                        add("today")
                        add("week")
                        add("month")
                    })
                    put("description", "Preset used when begin is omitted. Default today.")
                })
                put("query", buildJsonObject {
                    put("type", "string")
                    put("description", "Optional keyword to filter by event title.")
                })
                put("limit", buildJsonObject {
                    put("type", "integer")
                    put("description", "Maximum number of events to return. Default 20, maximum 100.")
                })
            }
        )
    },
    execute = { args ->
        if (!hasCalendarReadPermission(context)) {
            return@Tool listOf(
                UIMessagePart.Text(
                    buildJsonObject {
                        put("error", "NO_PERMISSION")
                        put("message", "Calendar read permission is not granted. Enable the Calendar local tool permission and try again.")
                    }.toString()
                )
            )
        }

        val params = args.jsonObject
        val limit = params["limit"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()?.coerceIn(1, 100) ?: 20
        val query = params["query"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

        val now = ZonedDateTime.now()
        val zone = now.zone
        val beginRaw = params["begin"]?.jsonPrimitive?.contentOrNull
        val endRaw = params["end"]?.jsonPrimitive?.contentOrNull
        val rangePreset = params["range"]?.jsonPrimitive?.contentOrNull ?: "today"

        val startTime: ZonedDateTime
        val endTime: ZonedDateTime
        try {
            startTime = if (beginRaw != null) {
                parseCalendarTime(beginRaw, zone)
            } else when (rangePreset) {
                "week" -> now.toLocalDate().atStartOfDay(zone).minusDays(now.dayOfWeek.value.toLong() - 1)
                "month" -> now.toLocalDate().withDayOfMonth(1).atStartOfDay(zone)
                else -> now.toLocalDate().atStartOfDay(zone)
            }
            endTime = if (endRaw != null) {
                parseCalendarTime(endRaw, zone)
            } else when (rangePreset) {
                "week" -> startTime.plusDays(7)
                "month" -> startTime.plusMonths(1)
                else -> now.toLocalDate().plusDays(1).atStartOfDay(zone)
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

        val events = buildJsonArray {
            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            )
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath(startTime.toInstant().toEpochMilli().toString())
                .appendPath(endTime.toInstant().toEpochMilli().toString())
                .build()
            val selection = query?.let { "${CalendarContract.Instances.TITLE} LIKE ?" }
            val selectionArgs = query?.let { arrayOf("%$it%") }
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val allDay = cursor.getInt(6) == 1
                    val dtStart = cursor.getLong(4)
                    val dtEnd = cursor.getLong(5)
                    add(buildJsonObject {
                        put("id", cursor.getLong(0))
                        put("title", cursor.getString(1) ?: "")
                        put("description", cursor.getString(2) ?: "")
                        put("location", cursor.getString(3) ?: "")
                        if (allDay) {
                            put("start", Instant.ofEpochMilli(dtStart).atZone(ZoneOffset.UTC).toLocalDate().toString())
                            put("end", if (dtEnd > 0) Instant.ofEpochMilli(dtEnd).atZone(ZoneOffset.UTC).toLocalDate().toString() else "")
                        } else {
                            put("start", Instant.ofEpochMilli(dtStart).atZone(zone).withNano(0).toString())
                            put("end", if (dtEnd > 0) Instant.ofEpochMilli(dtEnd).atZone(zone).withNano(0).toString() else "")
                        }
                        put("all_day", allDay)
                        put("calendar", cursor.getString(7) ?: "")
                    })
                    count++
                }
            }
        }

        listOf(
            UIMessagePart.Text(
                buildJsonObject {
                    put("range_start", startTime.withNano(0).toString())
                    put("range_end", endTime.withNano(0).toString())
                    put("count", events.size)
                    put("events", events)
                }.toString()
            )
        )
    }
)

internal fun buildCalendarCreateTool(context: Context): Tool = Tool(
    name = "calendar_create",
    description = """
        Create a new calendar event on the user's device.
        Requires title and start time. End defaults to 1 hour after start.
        Supports description, location, and all_day.
        Requires READ_CALENDAR and WRITE_CALENDAR permissions.
    """.trimIndent().replace("\n", " "),
    needsApproval = { true },
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("title", buildJsonObject {
                    put("type", "string")
                    put("description", "Event title.")
                })
                put("description", buildJsonObject {
                    put("type", "string")
                    put("description", "Event notes.")
                })
                put("location", buildJsonObject {
                    put("type", "string")
                    put("description", "Event location.")
                })
                put("start", buildJsonObject {
                    put("type", "string")
                    put("description", "Start time. ISO date, local date-time, offset date-time, Instant, or epoch milliseconds.")
                })
                put("end", buildJsonObject {
                    put("type", "string")
                    put("description", "End time. Defaults to 1 hour after start.")
                })
                put("all_day", buildJsonObject {
                    put("type", "boolean")
                    put("description", "Whether this is an all-day event. Default false.")
                })
            },
            required = listOf("title", "start")
        )
    },
    execute = { args ->
        if (!hasCalendarWritePermission(context)) {
            return@Tool listOf(
                UIMessagePart.Text(
                    buildJsonObject {
                        put("error", "NO_PERMISSION")
                        put("message", "Calendar write permission is not granted. Enable the Calendar local tool permission and try again.")
                    }.toString()
                )
            )
        }

        val params = args.jsonObject
        val title = params["title"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val startRaw = params["start"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        if (title == null || startRaw == null) {
            return@Tool listOf(
                UIMessagePart.Text(
                    buildJsonObject {
                        put("error", "MISSING_REQUIRED")
                        put("message", "Both title and start are required.")
                    }.toString()
                )
            )
        }

        val allDay = params["all_day"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
        val zone = ZoneId.systemDefault()
        val startTime: ZonedDateTime
        val endTime: ZonedDateTime
        try {
            startTime = parseCalendarTime(startRaw, zone)
            endTime = params["end"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?.let { parseCalendarTime(it, zone) }
                ?: if (allDay) startTime.toLocalDate().plusDays(1).atStartOfDay(zone) else startTime.plusHours(1)
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
                        put("message", "end must be later than start.")
                    }.toString()
                )
            )
        }

        val calendarId = getDefaultCalendarId(context)
        if (calendarId == null) {
            return@Tool listOf(
                UIMessagePart.Text(
                    buildJsonObject {
                        put("error", "NO_CALENDAR")
                        put("message", "No writable calendar account was found on this device.")
                    }.toString()
                )
            )
        }

        val (eventStartMillis, eventEndMillis, eventTimeZone) = if (allDay) {
            Triple(
                startTime.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                endTime.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                "UTC"
            )
        } else {
            Triple(
                startTime.toInstant().toEpochMilli(),
                endTime.toInstant().toEpochMilli(),
                zone.id
            )
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, params["description"]?.jsonPrimitive?.contentOrNull ?: "")
            put(CalendarContract.Events.EVENT_LOCATION, params["location"]?.jsonPrimitive?.contentOrNull ?: "")
            put(CalendarContract.Events.DTSTART, eventStartMillis)
            put(CalendarContract.Events.DTEND, eventEndMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, eventTimeZone)
            if (allDay) put(CalendarContract.Events.ALL_DAY, 1)
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        if (uri == null) {
            return@Tool listOf(
                UIMessagePart.Text(
                    buildJsonObject {
                        put("error", "INSERT_FAILED")
                        put("message", "Failed to insert calendar event.")
                    }.toString()
                )
            )
        }

        listOf(
            UIMessagePart.Text(
                buildJsonObject {
                    put("success", true)
                    put("event_id", ContentUris.parseId(uri))
                    put("title", title)
                    put("start", startTime.withNano(0).toString())
                    put("end", endTime.withNano(0).toString())
                    put("all_day", allDay)
                }.toString()
            )
        )
    }
)

private fun hasCalendarReadPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

private fun hasCalendarWritePermission(context: Context): Boolean =
    hasCalendarReadPermission(context) &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

private fun getDefaultCalendarId(context: Context): Long? {
    val projection = arrayOf(CalendarContract.Calendars._ID)
    val writableSelection =
        "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND ${CalendarContract.Calendars.SYNC_EVENTS} = 1"
    val writableArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
    context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        projection,
        "$writableSelection AND ${CalendarContract.Calendars.IS_PRIMARY} = 1",
        writableArgs,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getLong(0)
    }
    context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        projection,
        writableSelection,
        writableArgs,
        "${CalendarContract.Calendars.VISIBLE} DESC"
    )?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getLong(0)
    }
    return null
}

private fun parseCalendarTime(raw: String, zone: ZoneId): ZonedDateTime {
    val text = raw.trim()
    text.toLongOrNull()?.let { return Instant.ofEpochMilli(it).atZone(zone) }
    runCatching { return OffsetDateTime.parse(text).atZoneSameInstant(zone) }
    runCatching { return Instant.parse(text).atZone(zone) }
    runCatching { return LocalDateTime.parse(text).atZone(zone) }
    runCatching { return LocalDate.parse(text).atStartOfDay(zone) }
    error("Invalid time format: '$raw'. Use ISO-8601 date/date-time or epoch milliseconds.")
}
