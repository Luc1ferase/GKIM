package com.gkim.im.android.feature.tavern

import com.gkim.im.android.core.model.AppLanguage
import com.gkim.im.android.data.repository.ExportedCardFormat
import com.gkim.im.android.data.repository.ExportedCardPayload

enum class CardExportTarget { Share, Downloads }

data class CardExportDialogState(
    val format: ExportedCardFormat,
    val language: String,
    val includeTranslationAlt: Boolean = false,
    val target: CardExportTarget = CardExportTarget.Share,
    val inFlight: Boolean = false,
    val errorCode: String? = null,
    val completed: Boolean = false,
)

internal fun initialExportDialogState(
    format: ExportedCardFormat,
    activeLanguage: AppLanguage,
): CardExportDialogState = CardExportDialogState(
    format = format,
    language = activeLanguage.toWireLanguage(),
)

internal fun AppLanguage.toWireLanguage(): String = when (this) {
    AppLanguage.English -> "en"
    AppLanguage.Chinese -> "zh"
}

internal fun CardExportDialogState.withLanguage(language: String): CardExportDialogState =
    copy(language = language)

internal fun CardExportDialogState.withIncludeTranslationAlt(include: Boolean): CardExportDialogState =
    copy(includeTranslationAlt = include)

internal fun CardExportDialogState.withTarget(target: CardExportTarget): CardExportDialogState =
    copy(target = target)

internal fun CardExportDialogState.markInFlight(): CardExportDialogState =
    copy(inFlight = true, errorCode = null, completed = false)

internal fun CardExportDialogState.markCompleted(): CardExportDialogState =
    copy(inFlight = false, completed = true)

internal fun CardExportDialogState.markFailed(code: String): CardExportDialogState =
    copy(inFlight = false, errorCode = code, completed = false)

fun interface CardExportDispatcher {
    suspend fun dispatch(payload: ExportedCardPayload, target: CardExportTarget): Result<Unit>
}

sealed interface CardExportInvocationOutcome {
    data class Success(val payload: ExportedCardPayload, val target: CardExportTarget) : CardExportInvocationOutcome
    data class Failed(val code: String) : CardExportInvocationOutcome
}

suspend fun invokeCardExport(
    cardId: String,
    state: CardExportDialogState,
    repository: com.gkim.im.android.data.repository.CardInteropRepository,
    dispatcher: CardExportDispatcher,
): CardExportInvocationOutcome {
    val exportResult = repository.exportCard(
        cardId = cardId,
        format = state.format,
        language = state.language,
        includeTranslationAlt = state.includeTranslationAlt,
    )
    val payload = exportResult.getOrElse {
        return CardExportInvocationOutcome.Failed(it.message ?: "export_failed")
    }
    val deliverResult = dispatcher.dispatch(payload, state.target)
    return deliverResult.fold(
        onSuccess = { CardExportInvocationOutcome.Success(payload, state.target) },
        onFailure = { CardExportInvocationOutcome.Failed(it.message ?: "dispatch_failed") },
    )
}
