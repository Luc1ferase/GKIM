export type AigcMode = 'text-to-image' | 'image-to-image' | 'video-to-video'

export interface MediaInput {
  type: 'image' | 'video'
  path: string
}

export interface CustomProviderConfig {
  baseUrl: string
  apiKey: string
  model: string
}

export interface AigcProvider {
  id: string
  label: string
  vendor: string
  description: string
  model: string
  accent: 'primary' | 'secondary' | 'tertiary'
  preset: boolean
  capabilities: AigcMode[]
}

export interface AigcTask {
  id: string
  providerId: string
  mode: AigcMode
  prompt: string
  createdAt: string
  status: 'queued' | 'succeeded'
  input?: MediaInput
  outputPreview: string
}
