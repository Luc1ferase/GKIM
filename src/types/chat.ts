export type MessageDirection = 'incoming' | 'outgoing' | 'system'
export type MessageKind = 'text' | 'aigc'
export type AttachmentType = 'image' | 'video'

export interface MessageAttachment {
  type: AttachmentType
  preview: string
  prompt?: string
  generationId?: string
}

export interface ChatMessage {
  id: string
  direction: MessageDirection
  kind: MessageKind
  body: string
  createdAt: string
  chips?: string[]
  attachment?: MessageAttachment
}

export interface Conversation {
  id: string
  contactId: string
  contactName: string
  contactTitle: string
  avatarText: string
  lastMessage: string
  lastTimestamp: string
  unreadCount: number
  isOnline: boolean
  messages: ChatMessage[]
}
