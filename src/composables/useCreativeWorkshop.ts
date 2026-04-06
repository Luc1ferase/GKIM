import { storeToRefs } from 'pinia'
import { useFeedStore } from '@/stores/feed'
import type { WorkshopPrompt } from '@/types/social'

export function useCreativeWorkshop() {
  const feedStore = useFeedStore()
  const { filteredPrompts } = storeToRefs(feedStore)

  function applyPromptTemplate(prompt: WorkshopPrompt) {
    return prompt.prompt
  }

  return {
    filteredPrompts,
    applyPromptTemplate,
  }
}
