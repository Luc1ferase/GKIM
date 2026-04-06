import { defineStore } from 'pinia'
import { seedPosts, seedPrompts } from '@/stores/seeds'
import type { PromptCategory } from '@/types/social'

export const useFeedStore = defineStore('feed', {
  state: () => ({
    posts: seedPosts,
    prompts: seedPrompts,
    promptCategory: 'all' as PromptCategory,
    promptQuery: '',
  }),
  getters: {
    filteredPrompts(state) {
      return state.prompts.filter((item) => {
        const matchesCategory = state.promptCategory === 'all' || item.category === state.promptCategory
        const matchesQuery = !state.promptQuery || `${item.title} ${item.summary}`.toLowerCase().includes(state.promptQuery.toLowerCase())
        return matchesCategory && matchesQuery
      })
    },
  },
  actions: {
    setPromptCategory(category: PromptCategory) {
      this.promptCategory = category
    },
    setPromptQuery(query: string) {
      this.promptQuery = query
    },
  },
})
