<script setup lang="ts">
import type { WorkshopPrompt } from '@/types/social'

const props = defineProps<{
  prompt: WorkshopPrompt
}>()

const emit = defineEmits<{
  (event: 'apply', id: string): void
}>()
</script>

<template>
  <view class="card-shell px-5 py-5 section-stack gap-4">
    <view class="section-stack gap-2">
      <text class="text-[20rpx] uppercase tracking-[0.22em] text-tertiary">{{ props.prompt.category }}</text>
      <text class="text-[34rpx] leading-[1.1] font-headline text-on-surface">{{ props.prompt.title }}</text>
      <text class="text-[24rpx] leading-[1.6] text-on-surface-variant">{{ props.prompt.summary }}</text>
    </view>
    <text class="text-[22rpx] leading-[1.7] text-on-surface">{{ props.prompt.prompt }}</text>
    <view class="flex items-center justify-between gap-3">
      <text class="text-[20rpx] text-on-surface-variant">By {{ props.prompt.author }} · {{ props.prompt.uses }} uses</text>
      <button class="rounded-lg bg-gradient-to-br from-primary to-primary-container px-4 py-3 text-[22rpx] font-semibold text-surface" @tap="emit('apply', props.prompt.id)">
        Apply
      </button>
    </view>
  </view>
</template>
