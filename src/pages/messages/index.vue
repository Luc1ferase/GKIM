<script setup lang="ts">
import { computed } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import ConversationCard from '@/components/ConversationCard.vue'
import { useChatStore } from '@/stores/chat'

const chatStore = useChatStore()
const conversations = computed(() => chatStore.conversations)

function openConversation(id: string) {
  chatStore.openConversation(id)
  uni.navigateTo({ url: `/pages/chat/index?id=${id}` })
}

function openSettings() {
  uni.navigateTo({ url: '/pages/settings/index' })
}

function openWorkshop() {
  uni.navigateTo({ url: '/pages/workshop/index' })
}
</script>

<template>
  <view class="page-shell safe-bottom section-stack gap-6">
    <AppHeader
      eyebrow="Signal Lattice"
      title="Messages"
      description="Recent conversations, unread momentum, and a direct path into AIGC-assisted chats."
      action-label="Settings"
      @action="openSettings"
    />

    <view class="glass-card rounded-xl px-5 py-4 flex items-center justify-between gap-3">
      <view class="section-stack gap-1">
        <text class="text-[20rpx] uppercase tracking-[0.2em] text-primary">Unread Pulse</text>
        <text class="text-[26rpx] text-on-surface">{{ chatStore.totalUnread }} unread signals</text>
      </view>
      <button class="rounded-lg bg-surface-container-high px-4 py-3 text-[22rpx] text-on-surface" @tap="openWorkshop">
        Workshop
      </button>
    </view>

    <view v-if="conversations.length" class="section-stack gap-4">
      <ConversationCard v-for="conversation in conversations" :key="conversation.id" :conversation="conversation" @select="openConversation" />
    </view>

    <view v-else class="card-shell px-5 py-10 section-stack gap-3 items-start">
      <text class="text-[40rpx] font-headline text-on-surface">No active rooms yet</text>
      <text class="text-[24rpx] leading-[1.7] text-on-surface-variant">Seed data, websocket sync, or imported contacts will populate this lane once connected.</text>
    </view>
  </view>
</template>
