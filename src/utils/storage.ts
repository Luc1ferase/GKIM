const memoryStorage = new Map<string, string>()

type GlobalUniShape = typeof globalThis & {
  uni?: UniLike
}

function getUniStorage(): UniLike | undefined {
  return (globalThis as GlobalUniShape).uni
}

export const persistedStorage = {
  getItem(key: string) {
    const uniStorage = getUniStorage()
    if (uniStorage?.getStorageSync) {
      const value = uniStorage.getStorageSync(key)
      return typeof value === 'string' ? value : value ? JSON.stringify(value) : null
    }

    return memoryStorage.get(key) ?? null
  },
  setItem(key: string, value: string) {
    const uniStorage = getUniStorage()
    if (uniStorage?.setStorageSync) {
      uniStorage.setStorageSync(key, value)
      return
    }

    memoryStorage.set(key, value)
  },
  removeItem(key: string) {
    const uniStorage = getUniStorage()
    if (uniStorage?.removeStorageSync) {
      uniStorage.removeStorageSync(key)
      return
    }

    memoryStorage.delete(key)
  },
}
