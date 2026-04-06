import { describe, expect, it } from 'vitest'
import { useAIGC } from '@/composables/useAIGC'
import { useAigcStore } from '@/stores/aigc'

describe('useAIGC', () => {
  it('creates a text-to-image task with the active provider', async () => {
    const store = useAigcStore()
    const aigc = useAIGC()

    await aigc.generateTextToImage('Neon skyline with indigo haze')

    expect(store.history).toHaveLength(1)
    expect(store.history[0]?.mode).toBe('text-to-image')
    expect(store.history[0]?.providerId).toBe(store.activeProviderId)
  })
})
