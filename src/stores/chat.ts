import { defineStore } from 'pinia'
import type { AigcTask } from '@/types/aigc'
import type { Conversation } from '@/types/chat'
import { persistedStorage } from '@/utils/storage'

interface ChatState {
  conversations: Conversation[]
  activeConversationId: string
}

interface RoomSeed {
  contactId: string
  contactName: string
  contactTitle: string
  avatarText: string
}

function createRoom(seed: RoomSeed): Conversation {
  const timestamp = new Date().toISOString()
  return {
    id: `room-${seed.contactId}`,
    contactId: seed.contactId,
    contactName: seed.contactName,
    contactTitle: seed.contactTitle,
    avatarText: seed.avatarText,
    lastMessage: 'Room created. Start with a text prompt or an AIGC action.',
    lastTimestamp: timestamp,
    unreadCount: 0,
    isOnline: true,
    messages: [
      {
        id: `system-${seed.contactId}`,
        direction: 'system',
        kind: 'text',
        body: 'Room created. Start with a text prompt or an AIGC action.',
        createdAt: timestamp,
      },
    ],
  }
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    conversations: [],
    activeConversationId: '',
  }),
  getters: {
    activeConversation(state) {
      return state.conversations.find((item) => item.id === state.activeConversationId) ?? state.conversations[0]
    },
    totalUnread(state) {
      return state.conversations.reduce((sum, item) => sum + item.unreadCount, 0)
    },
  },
  actions: {
    openConversation(conversationId: string) {
      this.activeConversationId = conversationId
      const conversation = this.conversations.find((item) => item.id === conversationId)
      if (conversation) {
        conversation.unreadCount = 0
      }
    },
    ensureConversation(seed: RoomSeed) {
      const existing = this.conversations.find((item) => item.contactId === seed.contactId)
      if (existing) {
        this.openConversation(existing.id)
        return existing
      }

      const room = createRoom(seed)
      this.conversations.unshift(room)
      this.activeConversationId = room.id
      return room
    },
    ensureStudioRoom() {
      return this.ensureConversation({
        contactId: 'studio-agent',
        contactName: 'Studio Agent',
        contactTitle: 'Creative Workshop Bridge',
        avatarText: 'SA',
      })
    },
    appendOutgoingMessage(body: string) {
      const conversation = this.activeConversation ?? this.ensureStudioRoom()
      const timestamp = new Date().toISOString()
      conversation.messages.push({
        id: `out-${timestamp}`,
        direction: 'outgoing',
        kind: 'text',
        body,
        createdAt: timestamp,
      })
      conversation.lastMessage = body
      conversation.lastTimestamp = timestamp
    },
    appendAigcResult(task: AigcTask) {
      const conversation = this.activeConversation ?? this.ensureStudioRoom()
      const message = `Generated ${task.mode} output with ${task.providerId}.`
      conversation.messages.push({
        id: `aigc-${task.id}`,
        direction: 'system',
        kind: 'aigc',
        body: message,
        createdAt: task.createdAt,
        attachment: {
          type: task.mode === 'video-to-video' ? 'video' : 'image',
          preview: task.outputPreview,
          prompt: task.prompt,
          generationId: task.id,
        },
      })
      conversation.lastMessage = message
      conversation.lastTimestamp = task.createdAt
    },
  },
  persist: {
    storage: persistedStorage,
    paths: ['conversations', 'activeConversationId'],
  },
})
