<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  description?: string
  showBack?: boolean
  actionLabel?: string
}>()

const emit = defineEmits<{
  (event: 'back'): void
  (event: 'action'): void
}>()
</script>

<template>
  <view class="section-stack">
    <view class="glass-card rounded-xl px-5 py-4 flex items-start justify-between gap-4">
      <view class="flex-1 section-stack gap-2">
        <text class="text-[20rpx] uppercase tracking-[0.28em] text-primary font-mono">{{ eyebrow }}</text>
        <view class="section-stack gap-1">
          <text class="text-[56rpx] leading-[1.05] font-headline font-semibold text-on-surface">{{ title }}</text>
          <text v-if="description" class="text-[24rpx] leading-[1.6] text-on-surface-variant">{{ description }}</text>
        </view>
      </view>
      <view class="flex items-center gap-3">
        <button
          v-if="showBack"
          class="h-18 w-18 rounded-lg bg-surface-container-lowest/70 flex items-center justify-center text-[28rpx] text-on-surface"
          @tap="emit('back')"
        >
          <text>&lt;</text>
        </button>
        <button
          v-if="actionLabel"
          class="rounded-lg bg-gradient-to-br from-primary to-primary-container px-4 py-3 text-[22rpx] font-semibold text-surface"
          @tap="emit('action')"
        >
          {{ actionLabel }}
        </button>
      </view>
    </view>
  </view>
</template>
