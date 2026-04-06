import { computed } from 'vue'
import { useAigcStore } from '@/stores/aigc'
import { useChatStore } from '@/stores/chat'
import { providerRegistry } from '@/api/aigc/providers'
import type { AigcMode, MediaInput } from '@/types/aigc'

async function fallbackMedia(type: 'image' | 'video'): Promise<MediaInput> {
  return {
    type,
    path: type === 'image'
      ? 'https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=900&q=80'
      : 'https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?auto=format&fit=crop&w=900&q=80',
  }
}

export function useAIGC() {
  const aigcStore = useAigcStore()
  const chatStore = useChatStore()

  const history = computed(() => aigcStore.history)

  async function run(mode: AigcMode, prompt: string, input?: MediaInput) {
    const provider = aigcStore.activeProvider
    if (!provider) {
      throw new Error('No active provider configured.')
    }

    aigcStore.setLoading(true)
    aigcStore.setDraftMode(mode)
    aigcStore.setDraftPrompt(prompt)
    aigcStore.setDraftInput(input)

    try {
      const task = await providerRegistry[provider.id].generate({
        mode,
        prompt,
        provider,
        input,
      })
      aigcStore.pushHistory(task)
      chatStore.appendAigcResult(task)
      return task
    } finally {
      aigcStore.setLoading(false)
    }
  }

  async function generateTextToImage(prompt: string) {
    return run('text-to-image', prompt)
  }

  async function generateImageToImage(prompt: string) {
    const input = await fallbackMedia('image')
    return run('image-to-image', prompt, input)
  }

  async function generateVideoToVideo(prompt: string) {
    const input = await fallbackMedia('video')
    return run('video-to-video', prompt, input)
  }

  return {
    history,
    generateTextToImage,
    generateImageToImage,
    generateVideoToVideo,
  }
}
