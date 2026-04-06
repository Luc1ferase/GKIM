<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import PromptCard from '@/components/PromptCard.vue'
import { useCreativeWorkshop } from '@/composables/useCreativeWorkshop'
import { useFeedStore } from '@/stores/feed'
import { useAigcStore } from '@/stores/aigc'

const feedStore = useFeedStore()
const aigcStore = useAigcStore()
const { filteredPrompts, applyPromptTemplate } = useCreativeWorkshop()
const categories = ['all', 'portrait', 'video', 'cyberpunk', 'code-art'] as const

type UniInputLikeEvent = Event & {
  detail?: {
    value?: string | number
  }
}

function applyPrompt(promptId: string) {
  const prompt = feedStore.prompts.find((item) => item.id === promptId)
  if (!prompt) {
    return
  }

  aigcStore.setDraftPrompt(applyPromptTemplate(prompt))
  uni.navigateTo({ url: '/pages/chat/index' })
}

function handleInput(event: Event) {
  const value = (event as UniInputLikeEvent).detail?.value
  feedStore.setPromptQuery(typeof value === 'string' ? value : value == null ? '' : String(value))
}

function goBack() {
  uni.navigateBack()
}

function openSettings() {
  uni.navigateTo({ url: '/pages/settings/index' })
}
</script>

<template>
  <view class="page-shell safe-bottom section-stack gap-6">
    <AppHeader
      eyebrow="Community Prompt Hub"
      title="Workshop"
      description="Prompt templates for users who want a strong starting point before they write their own creative direction."
      show-back
      action-label="Settings"
      @back="goBack"
      @action="openSettings"
    />

    <input class="glass-card rounded-xl px-5 py-4 text-[24rpx] text-on-surface" placeholder="Search prompts" @input="handleInput" />

    <scroll-view scroll-x class="w-full whitespace-nowrap">
      <view class="flex gap-3 pr-4">
        <button
          v-for="category in categories"
          :key="category"
          class="rounded-pill px-4 py-3"
          :class="feedStore.promptCategory === category ? 'bg-primary text-surface' : 'bg-surface-container-high text-on-surface-variant'"
          @tap="feedStore.setPromptCategory(category)"
        >
          {{ category }}
        </button>
      </view>
    </scroll-view>

    <view class="section-stack gap-4">
      <PromptCard v-for="prompt in filteredPrompts" :key="prompt.id" :prompt="prompt" @apply="applyPrompt" />
    </view>
  </view>
</template>
