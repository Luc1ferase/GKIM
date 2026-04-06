<script setup lang="ts">
import type { Contact } from '@/types/social'

const props = defineProps<{
  contact: Contact
}>()

const emit = defineEmits<{
  (event: 'select', id: string): void
}>()
</script>

<template>
  <button class="card-shell w-full px-5 py-4 text-left flex items-center gap-4" @tap="emit('select', props.contact.id)">
    <view class="h-14 w-14 rounded-pill bg-secondary/12 flex items-center justify-center text-[24rpx] font-headline text-secondary">
      {{ props.contact.avatarText }}
    </view>
    <view class="flex-1 section-stack gap-1">
      <text class="text-[28rpx] font-headline text-on-surface">{{ props.contact.nickname }}</text>
      <text class="text-[22rpx] text-on-surface-variant">{{ props.contact.title }}</text>
    </view>
    <view class="h-3 w-3 rounded-pill" :class="props.contact.isOnline ? 'bg-success' : 'bg-outline-variant'" />
  </button>
</template>
