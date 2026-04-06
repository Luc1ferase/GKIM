<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import ContactCard from '@/components/ContactCard.vue'
import { useContactsStore } from '@/stores/contacts'
import { useChatStore } from '@/stores/chat'
import type { ContactSortMode } from '@/types/social'

const contactsStore = useContactsStore()
const chatStore = useChatStore()
const sortOptions = [
  { label: 'Nickname A-Z', value: 'nickname' },
  { label: 'Added earliest', value: 'added-asc' },
  { label: 'Added latest', value: 'added-desc' },
] as const

function handleSortChange(event: { detail: { value: string } }) {
  const option = sortOptions[Number(event.detail.value)]
  if (option) {
    contactsStore.setSortMode(option.value as ContactSortMode)
  }
}

function openContact(contactId: string) {
  const contact = contactsStore.contacts.find((item) => item.id === contactId)
  if (!contact) {
    return
  }

  const room = chatStore.ensureConversation({
    contactId: contact.id,
    contactName: contact.nickname,
    contactTitle: contact.title,
    avatarText: contact.avatarText,
  })
  uni.navigateTo({ url: `/pages/chat/index?id=${room.id}` })
}

function openSettings() {
  uni.navigateTo({ url: '/pages/settings/index' })
}
</script>

<template>
  <view class="page-shell safe-bottom section-stack gap-6">
    <AppHeader
      eyebrow="Address Mesh"
      title="Contacts"
      description="Sort by name or onboarding time, then jump straight into the corresponding message room."
      action-label="Settings"
      @action="openSettings"
    />

    <view class="glass-card rounded-xl px-5 py-4 flex items-center justify-between gap-4">
      <view class="section-stack gap-1">
        <text class="text-[20rpx] uppercase tracking-[0.2em] text-primary">Sort order</text>
        <text class="text-[24rpx] text-on-surface">{{ sortOptions.find((item) => item.value === contactsStore.sortMode)?.label }}</text>
      </view>
      <picker :range="sortOptions" range-key="label" @change="handleSortChange">
        <view class="rounded-lg bg-surface-container-high px-4 py-3 text-[22rpx] text-on-surface">Change</view>
      </picker>
    </view>

    <view class="section-stack gap-4">
      <ContactCard v-for="contact in contactsStore.sortedContacts" :key="contact.id" :contact="contact" @select="openContact" />
    </view>
  </view>
</template>
