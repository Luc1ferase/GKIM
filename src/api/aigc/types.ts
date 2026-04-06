import type { AigcMode, AigcProvider, AigcTask, MediaInput } from '@/types/aigc'

export interface GenerationRequest {
  mode: AigcMode
  prompt: string
  provider: AigcProvider
  input?: MediaInput
}

export interface ProviderAdapter {
  id: string
  label: string
  generate(request: GenerationRequest): Promise<AigcTask>
}
