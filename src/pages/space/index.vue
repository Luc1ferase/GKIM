<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import FeedPostCard from '@/components/FeedPostCard.vue'
import { useFeedStore } from '@/stores/feed'

const feedStore = useFeedStore()

function openWorkshop() {
  uni.navigateTo({ url: '/pages/workshop/index' })
}
</script>

<template>
  <view class="page-shell safe-bottom section-stack gap-6">
    <AppHeader
      eyebrow="Builder Feed"
      title="Space"
      description="Developer-first posts, prompt breakdowns, and community knowledge rendered through the shared Markdown pipeline."
      action-label="Workshop"
      @action="openWorkshop"
    />

    <scroll-view scroll-x class="w-full whitespace-nowrap">
      <view class="flex gap-3 pr-4">
        <view class="rounded-pill bg-primary px-4 py-3"><text class="text-[20rpx] uppercase tracking-[0.18em] text-surface">For You</text></view>
        <view class="rounded-pill bg-surface-container-high px-4 py-3"><text class="text-[20rpx] uppercase tracking-[0.18em] text-on-surface-variant">Prompting</text></view>
        <view class="rounded-pill bg-surface-container-high px-4 py-3"><text class="text-[20rpx] uppercase tracking-[0.18em] text-on-surface-variant">AI Tools</text></view>
        <view class="rounded-pill bg-surface-container-high px-4 py-3"><text class="text-[20rpx] uppercase tracking-[0.18em] text-on-surface-variant">Motion</text></view>
      </view>
    </scroll-view>

    <view class="section-stack gap-5">
      <FeedPostCard v-for="post in feedStore.posts" :key="post.id" :post="post" />
    </view>
  </view>
</template>
