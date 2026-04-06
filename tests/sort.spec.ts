import { describe, expect, it } from 'vitest'
import { sortContacts } from '@/utils/sort'
import type { Contact } from '@/types/social'

const contacts: Contact[] = [
  { id: 'b', nickname: 'Bravo', title: 'Engineer', avatarText: 'BR', addedAt: '2026-04-02T09:00:00Z', isOnline: false },
  { id: 'a', nickname: 'Alpha', title: 'Designer', avatarText: 'AL', addedAt: '2026-04-01T09:00:00Z', isOnline: true },
  { id: 'c', nickname: 'Cipher', title: 'Researcher', avatarText: 'CP', addedAt: '2026-04-03T09:00:00Z', isOnline: true }
]

describe('sortContacts', () => {
  it('sorts contacts by nickname initial', () => {
    expect(sortContacts(contacts, 'nickname').map((item) => item.nickname)).toEqual(['Alpha', 'Bravo', 'Cipher'])
  })

  it('sorts contacts by oldest added time first', () => {
    expect(sortContacts(contacts, 'added-asc').map((item) => item.id)).toEqual(['a', 'b', 'c'])
  })

  it('sorts contacts by newest added time first', () => {
    expect(sortContacts(contacts, 'added-desc').map((item) => item.id)).toEqual(['c', 'b', 'a'])
  })
})
