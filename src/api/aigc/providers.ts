import type { AigcProvider } from '@/types/aigc'
import type { GenerationRequest, ProviderAdapter } from '@/api/aigc/types'

function createPreview(mode: GenerationRequest['mode']) {
  if (mode === 'video-to-video') {
    return 'https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?auto=format&fit=crop&w=900&q=80'
  }

  return 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=900&q=80'
}

function createAdapter(id: string, label: string): ProviderAdapter {
  return {
    id,
    label,
    async generate(request) {
      return Promise.resolve({
        id: `${id}-${Date.now()}`,
        providerId: id,
        mode: request.mode,
        prompt: request.prompt,
        createdAt: new Date().toISOString(),
        status: 'succeeded',
        input: request.input,
        outputPreview: createPreview(request.mode),
      })
    },
  }
}

export const providerRegistry: Record<string, ProviderAdapter> = {
  hunyuan: createAdapter('hunyuan', 'Tencent Hunyuan'),
  tongyi: createAdapter('tongyi', 'Alibaba Tongyi'),
  custom: createAdapter('custom', 'Custom Endpoint'),
}

export const presetProviders: AigcProvider[] = [
  {
    id: 'hunyuan',
    label: 'Tencent Hunyuan',
    vendor: 'Tencent',
    description: 'Default image and video experimentation lane.',
    model: 'hunyuan-image',
    accent: 'primary',
    preset: true,
    capabilities: ['text-to-image', 'image-to-image', 'video-to-video'],
  },
  {
    id: 'tongyi',
    label: 'Alibaba Tongyi',
    vendor: 'Alibaba',
    description: 'Useful for prompt expansion and guided iteration.',
    model: 'wanx2.1',
    accent: 'secondary',
    preset: true,
    capabilities: ['text-to-image', 'image-to-image'],
  },
  {
    id: 'custom',
    label: 'Custom Endpoint',
    vendor: 'OpenAI Compatible',
    description: 'Connect your own gateway or third-party model service.',
    model: 'gpt-image-1',
    accent: 'tertiary',
    preset: false,
    capabilities: ['text-to-image', 'image-to-image', 'video-to-video'],
  },
]
