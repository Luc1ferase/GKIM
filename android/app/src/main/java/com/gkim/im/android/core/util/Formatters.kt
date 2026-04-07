package com.gkim.im.android.core.util

import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.core.model.ContactSortMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val chatFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val relativeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

fun formatChatTimestamp(raw: String): String =
    runCatching { chatFormatter.format(Instant.parse(raw).atZone(ZoneId.systemDefault())) }.getOrElse { raw }

fun formatRelativeLabel(raw: String): String =
    runCatching { relativeFormatter.format(Instant.parse(raw).atZone(ZoneId.systemDefault())) }.getOrElse { raw }

fun sortContacts(contacts: List<Contact>, mode: ContactSortMode): List<Contact> =
    when (mode) {
        ContactSortMode.Nickname -> contacts.sortedBy { it.nickname.lowercase(Locale.US) }
        ContactSortMode.AddedAscending -> contacts.sortedBy { it.addedAt }
        ContactSortMode.AddedDescending -> contacts.sortedByDescending { it.addedAt }
    }
