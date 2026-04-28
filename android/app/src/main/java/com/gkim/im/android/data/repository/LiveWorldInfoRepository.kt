package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.Lorebook
import com.gkim.im.android.core.model.LorebookBinding
import com.gkim.im.android.core.model.LorebookEntry
import com.gkim.im.android.data.remote.im.CreateLorebookBindingRequestDto
import com.gkim.im.android.data.remote.im.CreateLorebookEntryRequestDto
import com.gkim.im.android.data.remote.im.CreateLorebookRequestDto
import com.gkim.im.android.data.remote.im.ImWorldInfoClient
import com.gkim.im.android.data.remote.im.LocalizedTextDto
import com.gkim.im.android.data.remote.im.LorebookBindingDto
import com.gkim.im.android.data.remote.im.LorebookDto
import com.gkim.im.android.data.remote.im.LorebookEntryDto
import com.gkim.im.android.data.remote.im.PerLanguageStringListDto
import com.gkim.im.android.data.remote.im.UpdateLorebookEntryRequestDto
import com.gkim.im.android.data.remote.im.UpdateLorebookRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LiveWorldInfoRepository(
    private val default: DefaultWorldInfoRepository,
    private val client: ImWorldInfoClient,
    private val baseUrlProvider: () -> String?,
    private val tokenProvider: () -> String?,
) : WorldInfoRepository {

    override fun observeLorebooks(): Flow<List<Lorebook>> = default.observeLorebooks()
    override fun observeEntries(): Flow<Map<String, List<LorebookEntry>>> =
        default.observeEntries()
    override fun observeBindings(): Flow<Map<String, List<LorebookBinding>>> =
        default.observeBindings()

    override suspend fun refresh() {
        val baseUrl = baseUrlProvider() ?: return
        val token = tokenProvider() ?: return
        val (lorebooks, entries, bindings) = runCatching {
            val listDto = withContext(Dispatchers.IO) { client.list(baseUrl, token) }
            val allLorebooks = listDto.lorebooks.map(LorebookDto::toLorebook)
            val entriesByLorebook = mutableMapOf<String, List<LorebookEntry>>()
            val bindingsByLorebook = mutableMapOf<String, List<LorebookBinding>>()
            for (lb in allLorebooks) {
                entriesByLorebook[lb.id] = withContext(Dispatchers.IO) {
                    client.listEntries(baseUrl, token, lb.id).entries
                        .map(LorebookEntryDto::toLorebookEntry)
                }
                bindingsByLorebook[lb.id] = withContext(Dispatchers.IO) {
                    client.listBindings(baseUrl, token, lb.id).bindings
                        .map(LorebookBindingDto::toLorebookBinding)
                }
            }
            Triple(allLorebooks, entriesByLorebook.toMap(), bindingsByLorebook.toMap())
        }.getOrNull() ?: return
        default.setSnapshot(lorebooks, entries, bindings)
    }

    override suspend fun createLorebook(
        draft: Lorebook,
    ): WorldInfoMutationResult<Lorebook> {
        val localResult = default.createLorebook(draft)
        if (localResult !is WorldInfoMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider() ?: return localResult
        val token = tokenProvider() ?: return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                client.create(
                    baseUrl = baseUrl,
                    token = token,
                    request = CreateLorebookRequestDto(
                        displayName = LocalizedTextDto(
                            localResult.value.displayName.english,
                            localResult.value.displayName.chinese,
                        ),
                        description = LocalizedTextDto(
                            localResult.value.description.english,
                            localResult.value.description.chinese,
                        ),
                        isGlobal = localResult.value.isGlobal,
                        tokenBudget = localResult.value.tokenBudget,
                        extensions = localResult.value.extensions,
                    ),
                )
            }
            val reconciled = remote.toLorebook()
            default.setSnapshot(
                lorebooks = default.lorebooks.value.map {
                    if (it.id == localResult.value.id) reconciled else it
                },
                entries = (default.entries.value - localResult.value.id) +
                    (reconciled.id to default.entries.value[localResult.value.id].orEmpty()),
                bindings = (default.bindings.value - localResult.value.id) +
                    (reconciled.id to default.bindings.value[localResult.value.id].orEmpty()),
            )
            WorldInfoMutationResult.Success(reconciled)
        } catch (t: Throwable) {
            default.removeLorebook(localResult.value.id)
            WorldInfoMutationResult.Failed(t)
        }
    }

    override suspend fun updateLorebook(
        lorebook: Lorebook,
    ): WorldInfoMutationResult<Lorebook> {
        val priorSnapshot = default.lorebooks.value.firstOrNull { it.id == lorebook.id }
        val localResult = default.updateLorebook(lorebook)
        if (localResult !is WorldInfoMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider() ?: return localResult
        val token = tokenProvider() ?: return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                client.update(
                    baseUrl = baseUrl,
                    token = token,
                    lorebookId = lorebook.id,
                    request = UpdateLorebookRequestDto(
                        displayName = LocalizedTextDto(
                            localResult.value.displayName.english,
                            localResult.value.displayName.chinese,
                        ),
                        description = LocalizedTextDto(
                            localResult.value.description.english,
                            localResult.value.description.chinese,
                        ),
                        isGlobal = localResult.value.isGlobal,
                        tokenBudget = localResult.value.tokenBudget,
                        extensions = localResult.value.extensions,
                    ),
                )
            }
            val reconciled = remote.toLorebook()
            default.replaceLorebook(reconciled)
            WorldInfoMutationResult.Success(reconciled)
        } catch (t: Throwable) {
            if (priorSnapshot != null) default.replaceLorebook(priorSnapshot)
            WorldInfoMutationResult.Failed(t)
        }
    }

    override suspend fun deleteLorebook(
        lorebookId: String,
    ): WorldInfoMutationResult<Lorebook> {
        val priorLorebook = default.lorebooks.value.firstOrNull { it.id == lorebookId }
        val priorEntries = default.entries.value[lorebookId].orEmpty()
        val priorBindings = default.bindings.value[lorebookId].orEmpty()
        val localResult = default.deleteLorebook(lorebookId)
        if (localResult !is WorldInfoMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider() ?: return localResult
        val token = tokenProvider() ?: return localResult

        return try {
            withContext(Dispatchers.IO) { client.delete(baseUrl, token, lorebookId) }
            localResult
        } catch (t: Throwable) {
            if (priorLorebook != null) {
                default.setSnapshot(
                    lorebooks = default.lorebooks.value + priorLorebook,
                    entries = default.entries.value + (lorebookId to priorEntries),
                    bindings = default.bindings.value + (lorebookId to priorBindings),
                )
            }
            WorldInfoMutationResult.Failed(t)
        }
    }

    override suspend fun duplicateLorebook(
        lorebookId: String,
    ): WorldInfoMutationResult<Lorebook> {
        val localResult = default.duplicateLorebook(lorebookId)
        if (localResult !is WorldInfoMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider() ?: return localResult
        val token = tokenProvider() ?: return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                client.duplicate(baseUrl, token, lorebookId)
            }
            val reconciled = remote.toLorebook()
            default.setSnapshot(
                lorebooks = default.lorebooks.value.map {
                    if (it.id == localResult.value.id) reconciled else it
                },
                entries = (default.entries.value - localResult.value.id) +
                    (reconciled.id to default.entries.value[localResult.value.id].orEmpty()),
                bindings = (default.bindings.value - localResult.value.id) +
                    (reconciled.id to default.bindings.value[localResult.value.id].orEmpty()),
            )
            WorldInfoMutationResult.Success(reconciled)
        } catch (t: Throwable) {
            default.removeLorebook(localResult.value.id)
            WorldInfoMutationResult.Failed(t)
        }
    }

    override suspend fun createEntry(
        lorebookId: String,
        draft: LorebookEntry,
    ): WorldInfoMutationResult<LorebookEntry> {
        val localResult = default.createEntry(lorebookId, draft)
        if (localResult !is WorldInfoMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider() ?: return localResult
        val token = tokenProvider() ?: return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                client.createEntry(
                    baseUrl = baseUrl,
                    token = token,
                    lorebookId = lorebookId,
                    request = CreateLorebookEntryRequestDto(
                        name = LocalizedTextDto(
                            localResult.value.name.english,
                            localResult.value.name.chinese,
                        ),
                        keysByLang = PerLanguageStringListDto.fromLanguageMap(
                            localResult.value.keysByLang,
                        ),
                        secondaryKeysByLang = PerLanguageStringListDto.fromLanguageMap(
                            localResult.value.secondaryKeysByLang,
                        ),
                        secondaryGate = localResult.value.secondaryGate.name.uppercase(),
                        content = LocalizedTextDto(
                            localResult.value.content.english,
                            localResult.value.content.chinese,
                        ),
                        enabled = localResult.value.enabled,
                        constant = localResult.value.constant,
                        caseSensitive = localResult.value.caseSensitive,
                        scanDepth = localResult.value.scanDepth,
                        insertionOrder = localResult.value.insertionOrder,
                        comment = localResult.value.comment,
                        extensions = localResult.value.extensions,
                    ),
                )
            }
            val reconciled = remote.toLorebookEntry()
            default.replaceEntries(
                lorebookId = lorebookId,
                entries = default.entries.value[lorebookId].orEmpty().map {
                    if (it.id == localResult.value.id) reconciled else it
                },
            )
            WorldInfoMutationResult.Success(reconciled)
        } catch (t: Throwable) {
            default.replaceEntries(
                lorebookId = lorebookId,
                entries = default.entries.value[lorebookId].orEmpty()
                    .filterNot { it.id == localResult.value.id },
            )
            WorldInfoMutationResult.Failed(t)
        }
    }

    override suspend fun updateEntry(
        entry: LorebookEntry,
    ): WorldInfoMutationResult<LorebookEntry> {
        val prior = default.entries.value[entry.lorebookId].orEmpty()
        val localResult = default.updateEntry(entry)
        if (localResult !is WorldInfoMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider() ?: return localResult
        val token = tokenProvider() ?: return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                client.updateEntry(
                    baseUrl = baseUrl,
                    token = token,
                    lorebookId = entry.lorebookId,
                    entryId = entry.id,
                    request = UpdateLorebookEntryRequestDto(
                        name = LocalizedTextDto(entry.name.english, entry.name.chinese),
                        keysByLang = PerLanguageStringListDto.fromLanguageMap(entry.keysByLang),
                        secondaryKeysByLang = PerLanguageStringListDto.fromLanguageMap(
                            entry.secondaryKeysByLang,
                        ),
                        secondaryGate = entry.secondaryGate.name.uppercase(),
                        content = LocalizedTextDto(
                            entry.content.english,
                            entry.content.chinese,
                        ),
                        enabled = entry.enabled,
                        constant = entry.constant,
                        caseSensitive = entry.caseSensitive,
                        scanDepth = entry.scanDepth,
                        insertionOrder = entry.insertionOrder,
                        comment = entry.comment,
                        extensions = entry.extensions,
                    ),
                )
            }
            val reconciled = remote.toLorebookEntry()
            default.replaceEntries(
                lorebookId = entry.lorebookId,
                entries = default.entries.value[entry.lorebookId].orEmpty().map {
                    if (it.id == reconciled.id) reconciled else it
                },
            )
            WorldInfoMutationResult.Success(reconciled)
        } catch (t: Throwable) {
            default.replaceEntries(entry.lorebookId, prior)
            WorldInfoMutationResult.Failed(t)
        }
    }

    override suspend fun deleteEntry(
        lorebookId: String,
        entryId: String,
    ): WorldInfoMutationResult<LorebookEntry> {
        val prior = default.entries.value[lorebookId].orEmpty()
        val localResult = default.deleteEntry(lorebookId, entryId)
        if (localResult !is WorldInfoMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider() ?: return localResult
        val token = tokenProvider() ?: return localResult

        return try {
            withContext(Dispatchers.IO) {
                client.deleteEntry(baseUrl, token, lorebookId, entryId)
            }
            localResult
        } catch (t: Throwable) {
            default.replaceEntries(lorebookId, prior)
            WorldInfoMutationResult.Failed(t)
        }
    }

    override suspend fun bind(
        lorebookId: String,
        characterId: String,
        isPrimary: Boolean,
    ): WorldInfoMutationResult<LorebookBinding> {
        val priorMap = default.bindings.value
        val localResult = default.bind(lorebookId, characterId, isPrimary)
        if (localResult !is WorldInfoMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider() ?: return localResult
        val token = tokenProvider() ?: return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                client.bind(
                    baseUrl = baseUrl,
                    token = token,
                    lorebookId = lorebookId,
                    characterId = characterId,
                    isPrimary = isPrimary,
                )
            }
            val reconciled = remote.toLorebookBinding()
            default.replaceBindings(
                lorebookId = lorebookId,
                bindings = default.bindings.value[lorebookId].orEmpty().map {
                    if (it.characterId == characterId) reconciled else it
                },
            )
            WorldInfoMutationResult.Success(reconciled)
        } catch (t: Throwable) {
            default.setSnapshot(
                lorebooks = default.lorebooks.value,
                entries = default.entries.value,
                bindings = priorMap,
            )
            WorldInfoMutationResult.Failed(t)
        }
    }

    override suspend fun updateBinding(
        lorebookId: String,
        characterId: String,
        isPrimary: Boolean,
    ): WorldInfoMutationResult<LorebookBinding> {
        val priorMap = default.bindings.value
        val localResult = default.updateBinding(lorebookId, characterId, isPrimary)
        if (localResult !is WorldInfoMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider() ?: return localResult
        val token = tokenProvider() ?: return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                client.updateBinding(
                    baseUrl = baseUrl,
                    token = token,
                    lorebookId = lorebookId,
                    characterId = characterId,
                    isPrimary = isPrimary,
                )
            }
            val reconciled = remote.toLorebookBinding()
            default.replaceBindings(
                lorebookId = lorebookId,
                bindings = default.bindings.value[lorebookId].orEmpty().map {
                    if (it.characterId == characterId) reconciled else it
                },
            )
            WorldInfoMutationResult.Success(reconciled)
        } catch (t: Throwable) {
            default.setSnapshot(
                lorebooks = default.lorebooks.value,
                entries = default.entries.value,
                bindings = priorMap,
            )
            WorldInfoMutationResult.Failed(t)
        }
    }

    override suspend fun unbind(
        lorebookId: String,
        characterId: String,
    ): WorldInfoMutationResult<LorebookBinding> {
        val priorBindings = default.bindings.value[lorebookId].orEmpty()
        val localResult = default.unbind(lorebookId, characterId)
        if (localResult !is WorldInfoMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider() ?: return localResult
        val token = tokenProvider() ?: return localResult

        return try {
            withContext(Dispatchers.IO) {
                client.unbind(baseUrl, token, lorebookId, characterId)
            }
            localResult
        } catch (t: Throwable) {
            default.replaceBindings(lorebookId, priorBindings)
            WorldInfoMutationResult.Failed(t)
        }
    }
}
