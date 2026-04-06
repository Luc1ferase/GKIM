<script setup lang="ts">
import type { AigcProvider } from '@/types/aigc'

const props = defineProps<{
  provider: AigcProvider
  active: boolean
}>()

const emit = defineEmits<{
  (event: 'select', id: string): void
}>()
</script>

<template>
  <button
    class="w-full rounded-xl px-5 py-5 text-left"
    :class="active ? 'bg-surface-container-highest shadow-glow' : 'bg-surface-container-low'"
    @tap="emit('select', props.provider.id)"
  >
    <view class="section-stack gap-2">
      <text class="text-[20rpx] uppercase tracking-[0.22em]" :class="props.active ? 'text-primary' : 'text-on-surface-variant'">
        {{ props.provider.vendor }}
      </text>
      <text class="text-[34rpx] font-headline text-on-surface">{{ props.provider.label }}</text>
      <text class="text-[24rpx] leading-[1.6] text-on-surface-variant">{{ props.provider.description }}</text>
      <text class="text-[20rpx] text-on-surface-variant">Model · {{ props.provider.model }}</text>
    </view>
  </button>
</template>
