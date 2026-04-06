import type { ContactSortMode, Contact } from '@/types/social'

export function sortContacts(contacts: Contact[], mode: ContactSortMode) {
  const cloned = [...contacts]

  if (mode === 'added-asc') {
    return cloned.sort((left, right) => new Date(left.addedAt).getTime() - new Date(right.addedAt).getTime())
  }

  if (mode === 'added-desc') {
    return cloned.sort((left, right) => new Date(right.addedAt).getTime() - new Date(left.addedAt).getTime())
  }

  return cloned.sort((left, right) => left.nickname.localeCompare(right.nickname, 'zh-Hans-CN'))
}
