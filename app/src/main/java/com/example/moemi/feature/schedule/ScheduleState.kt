package com.example.moemi.feature.schedule

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.LocalTime

enum class TravelMode(val label: String) { WALK("도보"), BUS("버스") }

/** Color tone of a schedule block on the timeline. */
enum class ScheduleTone { PRIMARY, CAUTION, GOOD }

data class ScheduleUi(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    val place: String?,
    val tone: ScheduleTone,
    /** Preparation before leaving, in minutes. 0 means no prep block. */
    val prepMinutes: Int = 0,
    /** Travel time to the place, in minutes. 0 means no travel block. */
    val travelMinutes: Int = 0,
    val travelLabel: String? = null,
) {
    val departAt: LocalTime get() = start.minusMinutes(travelMinutes.toLong())
    val prepStartAt: LocalTime get() = departAt.minusMinutes(prepMinutes.toLong())
}

/** A reminder that is going to fire, shown in the reminder settings list. */
data class PlannedReminderUi(val time: LocalTime, val title: String, val offsetLabel: String)

/** Reminder offset presets in minutes. */
val ReminderPresets = listOf(1440, 60, 30, 10)

fun offsetLabel(minutes: Int): String = when {
    minutes % 1440 == 0 -> "${minutes / 1440}일 전"
    minutes % 60 == 0 -> "${minutes / 60}시간 전"
    else -> "${minutes}분 전"
}

/**
 * UI state of the schedule tab (timeline, new schedule, reminder settings).
 * Holds sample data until the schedule and reminder APIs are connected.
 */
class ScheduleState(today: LocalDate) {
    var selectedDate by mutableStateOf(today)

    val schedules = mutableStateListOf(
        ScheduleUi(
            1, "동아리 모임", today, LocalTime.of(14, 0), LocalTime.of(15, 30), "광주송정역",
            ScheduleTone.PRIMARY, prepMinutes = 30, travelMinutes = 40, travelLabel = "버스 송정98",
        ),
        ScheduleUi(2, "학원 상담", today, LocalTime.of(15, 0), LocalTime.of(16, 0), null, ScheduleTone.CAUTION),
        ScheduleUi(3, "학원 수업", today, LocalTime.of(18, 30), LocalTime.of(20, 0), null, ScheduleTone.GOOD),
        ScheduleUi(4, "체육대회", today.plusDays(2), LocalTime.of(9, 0), LocalTime.of(12, 0), "운동장", ScheduleTone.GOOD),
        ScheduleUi(5, "독서실", today.plusDays(4), LocalTime.of(19, 0), LocalTime.of(21, 0), null, ScheduleTone.PRIMARY),
    )

    /** Reminder offsets of the schedule being edited, in minutes before departure. */
    val reminderOffsets = mutableStateListOf(60, 30)
    var reminderEnabled by mutableStateOf(true)
    var defaultOffset by mutableIntStateOf(60)

    fun schedulesOn(date: LocalDate) = schedules.filter { it.date == date }.sortedBy { it.start }

    fun hasSchedule(date: LocalDate) = schedules.any { it.date == date }

    fun toneOf(date: LocalDate): ScheduleTone? = schedulesOn(date).firstOrNull()?.tone

    /** First pair of schedules on [date] whose times overlap, with the overlap in minutes. */
    fun conflictOn(date: LocalDate): Triple<ScheduleUi, ScheduleUi, Long>? {
        val list = schedulesOn(date)
        for (i in list.indices) for (j in i + 1 until list.size) {
            val a = list[i]
            val b = list[j]
            if (b.start < a.end) {
                val end = if (a.end < b.end) a.end else b.end
                return Triple(b, a, java.time.Duration.between(b.start, end).toMinutes())
            }
        }
        return null
    }

    fun toggleOffset(minutes: Int) {
        if (minutes in reminderOffsets) reminderOffsets.remove(minutes) else reminderOffsets.add(minutes)
        reminderOffsets.sortDescending()
    }

    fun reminderSummary(): String =
        if (!reminderEnabled || reminderOffsets.isEmpty()) "끔" else reminderOffsets.joinToString(", ") { offsetLabel(it) }

    fun plannedReminders(): List<PlannedReminderUi> {
        val target = schedules.first()
        val own = if (reminderEnabled) reminderOffsets.map {
            PlannedReminderUi(target.start.minusMinutes(it.toLong()), target.title, offsetLabel(it))
        } else emptyList()
        val others = schedules.drop(1).filter { it.date == target.date }.map {
            PlannedReminderUi(it.start.minusMinutes(defaultOffset.toLong()), it.title, offsetLabel(defaultOffset))
        }
        return (own + others).sortedBy { it.time }
    }

    fun add(schedule: ScheduleUi) {
        schedules.add(schedule.copy(id = (schedules.maxOfOrNull { it.id } ?: 0) + 1))
    }
}
