import { defineStore } from 'pinia'
import { seedContacts } from '@/stores/seeds'
import type { ContactSortMode } from '@/types/social'
import { sortContacts } from '@/utils/sort'
import { persistedStorage } from '@/utils/storage'

export const useContactsStore = defineStore('contacts', {
  state: () => ({
    contacts: seedContacts,
    sortMode: 'nickname' as ContactSortMode,
  }),
  getters: {
    sortedContacts(state) {
      return sortContacts(state.contacts, state.sortMode)
    },
  },
  actions: {
    setSortMode(mode: ContactSortMode) {
      this.sortMode = mode
    },
  },
  persist: {
    storage: persistedStorage,
    paths: ['sortMode'],
  },
})
