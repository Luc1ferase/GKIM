import { beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const storage = new Map<string, string>()

const uniMock = {
  getStorageSync(key: string) {
    return storage.get(key) ?? ''
  },
  setStorageSync(key: string, value: string) {
    storage.set(key, value)
  },
  removeStorageSync(key: string) {
    storage.delete(key)
  },
  navigateTo() {},
  switchTab() {},
  showToast() {},
  chooseImage() {},
  chooseVideo() {},
}

Object.assign(globalThis, { uni: uniMock })

beforeEach(() => {
  storage.clear()
  setActivePinia(createPinia())
})
