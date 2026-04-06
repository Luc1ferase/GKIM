import { describe, expect, it } from 'vitest'
import { useCreativeWorkshop } from '@/composables/useCreativeWorkshop'
import { useFeedStore } from '@/stores/feed'

describe('useCreativeWorkshop', () => {
  it('returns prompt templates filtered by active category', () => {
    const store = useFeedStore()
    const workshop = useCreativeWorkshop()

    store.setPromptCategory('portrait')

    expect(workshop.filteredPrompts.value.every((item) => item.category === 'portrait')).toBe(true)
  })
})
