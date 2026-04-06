import { defineStore } from 'pinia'
import { seedProviders } from '@/stores/seeds'
import type { AigcMode, AigcTask, CustomProviderConfig, MediaInput } from '@/types/aigc'
import { persistedStorage } from '@/utils/storage'

interface DraftRequest {
  mode: AigcMode
  prompt: string
  input?: MediaInput
}

export const useAigcStore = defineStore('aigc', {
  state: () => ({
    providers: seedProviders,
    activeProviderId: seedProviders[0]?.id ?? 'hunyuan',
    customProvider: {
      baseUrl: 'https://api.example.com/v1',
      apiKey: '',
      model: 'gpt-image-1',
    } as CustomProviderConfig,
    isLoading: false,
    draftRequest: {
      mode: 'text-to-image',
      prompt: '',
    } as DraftRequest,
    history: [] as AigcTask[],
  }),
  getters: {
    activeProvider(state) {
      return state.providers.find((item) => item.id === state.activeProviderId) ?? state.providers[0]
    },
  },
  actions: {
    setActiveProvider(providerId: string) {
      this.activeProviderId = providerId
    },
    updateCustomProvider(payload: Partial<CustomProviderConfig>) {
      this.customProvider = { ...this.customProvider, ...payload }
    },
    setDraftPrompt(prompt: string) {
      this.draftRequest.prompt = prompt
    },
    setDraftMode(mode: AigcMode) {
      this.draftRequest.mode = mode
    },
    setDraftInput(input?: MediaInput) {
      this.draftRequest.input = input
    },
    setLoading(value: boolean) {
      this.isLoading = value
    },
    pushHistory(task: AigcTask) {
      this.history.unshift(task)
    },
  },
  persist: {
    storage: persistedStorage,
    paths: ['activeProviderId', 'customProvider', 'history'],
  },
})
