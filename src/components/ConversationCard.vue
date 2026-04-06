<script setup lang="ts">
import type { Conversation } from '@/types/chat'
import { formatChatTimestamp } from '@/utils/time'

const props = defineProps<{
  conversation: Conversation
}>()

const emit = defineEmits<{
  (event: 'select', id: string): void
}>()
</script>

<template>
  <button
    class="card-shell w-full px-5 py-4 text-left flex items-center gap-4"
    @tap="emit('select', props.conversation.id)"
  >
    <view class="h-14 w-14 rounded-pill bg-primary/12 flex items-center justify-center text-[24rpx] font-headline text-primary">
      {{ props.conversation.avatarText }}
    </view>
    <view class="flex-1 min-w-0 section-stack gap-1">
      <view class="flex items-center justify-between gap-3">
        <text class="truncate text-[28rpx] font-headline text-on-surface">{{ props.conversation.contactName }}</text>
        <text class="text-[20rpx] text-on-surface-variant">{{ formatChatTimestamp(props.conversation.lastTimestamp) }}</text>
      </view>
      <view class="flex items-center justify-between gap-3">
        <text class="truncate text-[22rpx] text-on-surface-variant">{{ props.conversation.lastMessage }}</text>
        <view
          v-if="props.conversation.unreadCount"
          class="min-w-8 rounded-pill bg-primary px-2 py-1 text-center text-[18rpx] font-semibold text-surface"
        >
          {{ props.conversation.unreadCount }}
        </view>
      </view>
    </view>
  </button>
</template>
