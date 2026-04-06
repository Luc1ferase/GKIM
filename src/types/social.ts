export type ContactSortMode = 'nickname' | 'added-asc' | 'added-desc'
export type PromptCategory = 'all' | 'portrait' | 'video' | 'cyberpunk' | 'code-art'

export interface Contact {
  id: string
  nickname: string
  title: string
  avatarText: string
  addedAt: string
  isOnline: boolean
}

export interface FeedPost {
  id: string
  author: string
  role: string
  title: string
  summary: string
  body: string
  tags: string[]
  createdAt: string
  accent: 'primary' | 'tertiary' | 'secondary'
}

export interface WorkshopPrompt {
  id: string
  title: string
  summary: string
  prompt: string
  category: PromptCategory
  author: string
  uses: number
  mdxReady: boolean
}
