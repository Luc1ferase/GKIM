<script setup lang="ts">
import { computed } from 'vue'
import type { FeedPost } from '@/types/social'
import { renderPostBody } from '@/utils/markdown'
import { formatRelativeLabel } from '@/utils/time'

const props = defineProps<{
  post: FeedPost
}>()

const html = computed(() => renderPostBody(props.post.body))
</script>

<template>
  <view class="card-shell px-5 py-5 section-stack gap-4">
    <view class="flex items-start justify-between gap-3">
      <view class="section-stack gap-1">
        <text class="text-[20rpx] uppercase tracking-[0.2em]" :class="post.accent === 'primary' ? 'text-primary' : post.accent === 'tertiary' ? 'text-tertiary' : 'text-secondary'">
          {{ post.author }}
        </text>
        <text class="text-[42rpx] leading-[1.08] font-headline text-on-surface">{{ post.title }}</text>
      </view>
      <text class="text-[20rpx] text-on-surface-variant">{{ formatRelativeLabel(post.createdAt) }}</text>
    </view>
    <text class="text-[24rpx] leading-[1.7] text-on-surface-variant">{{ post.summary }}</text>
    <view class="rich-content">
      <rich-text :nodes="html" />
    </view>
    <view class="flex flex-wrap gap-2">
      <view v-for="tag in post.tags" :key="tag" class="rounded-pill bg-surface-container-high px-3 py-2">
        <text class="text-[18rpx] uppercase tracking-[0.16em] text-on-surface-variant">{{ tag }}</text>
      </view>
    </view>
  </view>
</template>
