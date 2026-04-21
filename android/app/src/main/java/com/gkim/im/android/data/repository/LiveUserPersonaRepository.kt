package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.UserPersona
import com.gkim.im.android.data.remote.im.ImBackendClient
import com.gkim.im.android.data.remote.im.UserPersonaDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LiveUserPersonaRepository(
    private val default: DefaultUserPersonaRepository,
    private val backendClient: ImBackendClient,
    private val baseUrlProvider: () -> String?,
    private val tokenProvider: () -> String?,
) : UserPersonaRepository {

    override fun observePersonas(): Flow<List<UserPersona>> = default.observePersonas()

    override fun observeActivePersona(): Flow<UserPersona?> = default.observeActivePersona()

    override suspend fun refresh() {
        val baseUrl = baseUrlProvider() ?: return
        val token = tokenProvider() ?: return
        val merged = runCatching {
            coroutineScope {
                val listDeferred = async(Dispatchers.IO) {
                    backendClient.listPersonas(baseUrl, token)
                }
                val activeDeferred = async(Dispatchers.IO) {
                    runCatching { backendClient.getActivePersona(baseUrl, token) }.getOrNull()
                }
                val listDto = listDeferred.await()
                val activeDto = activeDeferred.await()
                mergeRemote(listDto.personas, listDto.activePersonaId, activeDto)
            }
        }.getOrNull() ?: return
        default.setSnapshot(merged)
    }

    override suspend fun create(persona: UserPersona): UserPersonaMutationResult {
        val localResult = default.create(persona)
        if (localResult !is UserPersonaMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider()
        val token = tokenProvider()
        if (baseUrl == null || token == null) return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                backendClient.createPersona(
                    baseUrl = baseUrl,
                    token = token,
                    persona = UserPersonaDto.fromUserPersona(localResult.persona),
                )
            }
            val reconciled = remote.toUserPersona()
            default.setSnapshot(
                default.personas.value.map {
                    if (it.id == localResult.persona.id) reconciled else it
                },
            )
            UserPersonaMutationResult.Success(reconciled)
        } catch (t: Throwable) {
            default.setSnapshot(
                default.personas.value.filterNot { it.id == localResult.persona.id },
            )
            UserPersonaMutationResult.Failed(t)
        }
    }

    override suspend fun update(persona: UserPersona): UserPersonaMutationResult {
        val snapshot = default.personas.value
        val localResult = default.update(persona)
        if (localResult !is UserPersonaMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider()
        val token = tokenProvider()
        if (baseUrl == null || token == null) return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                backendClient.updatePersona(
                    baseUrl = baseUrl,
                    token = token,
                    persona = UserPersonaDto.fromUserPersona(localResult.persona),
                )
            }
            val reconciled = remote.toUserPersona()
            default.setSnapshot(
                default.personas.value.map {
                    if (it.id == reconciled.id) reconciled else it
                },
            )
            UserPersonaMutationResult.Success(reconciled)
        } catch (t: Throwable) {
            default.setSnapshot(snapshot)
            UserPersonaMutationResult.Failed(t)
        }
    }

    override suspend fun delete(personaId: String): UserPersonaMutationResult {
        val snapshot = default.personas.value
        val localResult = default.delete(personaId)
        if (localResult !is UserPersonaMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider()
        val token = tokenProvider()
        if (baseUrl == null || token == null) return localResult

        return try {
            withContext(Dispatchers.IO) {
                backendClient.deletePersona(baseUrl = baseUrl, token = token, personaId = personaId)
            }
            localResult
        } catch (t: Throwable) {
            default.setSnapshot(snapshot)
            UserPersonaMutationResult.Failed(t)
        }
    }

    override suspend fun activate(personaId: String): UserPersonaMutationResult {
        val snapshot = default.personas.value
        val localResult = default.activate(personaId)
        if (localResult !is UserPersonaMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider()
        val token = tokenProvider()
        if (baseUrl == null || token == null) return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                backendClient.activatePersona(baseUrl = baseUrl, token = token, personaId = personaId)
            }
            val activated = remote.toUserPersona()
            default.setSnapshot(
                default.personas.value.map { p ->
                    when {
                        p.id == activated.id -> activated
                        p.isActive -> p.copy(isActive = false)
                        else -> p
                    }
                },
            )
            UserPersonaMutationResult.Success(activated)
        } catch (t: Throwable) {
            default.setSnapshot(snapshot)
            UserPersonaMutationResult.Failed(t)
        }
    }

    override suspend fun duplicate(personaId: String): UserPersonaMutationResult {
        val localResult = default.duplicate(personaId)
        if (localResult !is UserPersonaMutationResult.Success) return localResult
        val baseUrl = baseUrlProvider()
        val token = tokenProvider()
        if (baseUrl == null || token == null) return localResult

        return try {
            val remote = withContext(Dispatchers.IO) {
                backendClient.createPersona(
                    baseUrl = baseUrl,
                    token = token,
                    persona = UserPersonaDto.fromUserPersona(localResult.persona),
                )
            }
            val reconciled = remote.toUserPersona()
            default.setSnapshot(
                default.personas.value.map {
                    if (it.id == localResult.persona.id) reconciled else it
                },
            )
            UserPersonaMutationResult.Success(reconciled)
        } catch (t: Throwable) {
            default.setSnapshot(
                default.personas.value.filterNot { it.id == localResult.persona.id },
            )
            UserPersonaMutationResult.Failed(t)
        }
    }

    private fun mergeRemote(
        remoteList: List<UserPersonaDto>,
        activePersonaId: String?,
        activeDto: UserPersonaDto?,
    ): List<UserPersona> {
        val byId = remoteList.associateBy { it.id }.toMutableMap()
        if (activeDto != null) {
            byId[activeDto.id] = activeDto
        }
        val targetActiveId = activePersonaId ?: activeDto?.id
        return byId.values.map { it.toUserPersona() }.map { persona ->
            if (targetActiveId != null) persona.copy(isActive = persona.id == targetActiveId)
            else persona
        }
    }
}
