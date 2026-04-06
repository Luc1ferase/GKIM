import { presetProviders } from '@/api/aigc/providers'
import type { Conversation } from '@/types/chat'
import type { Contact, FeedPost, WorkshopPrompt } from '@/types/social'

export const seedContacts: Contact[] = [
  {
    id: 'leo-vance',
    nickname: 'Leo Vance',
    title: 'Realtime Systems',
    avatarText: 'LV',
    addedAt: '2026-04-02T09:00:00Z',
    isOnline: true,
  },
  {
    id: 'aria-thorne',
    nickname: 'Aria Thorne',
    title: 'Prompt Architect',
    avatarText: 'AT',
    addedAt: '2026-04-01T09:00:00Z',
    isOnline: true,
  },
  {
    id: 'clara-wu',
    nickname: 'Clara Wu',
    title: 'Visual Systems',
    avatarText: 'CW',
    addedAt: '2026-04-03T09:00:00Z',
    isOnline: false,
  },
]

export const seedConversations: Conversation[] = [
  {
    id: 'room-leo',
    contactId: 'leo-vance',
    contactName: 'Leo Vance',
    contactTitle: 'Realtime Systems',
    avatarText: 'LV',
    lastMessage: 'The orbital thread is stable. Ready for review.',
    lastTimestamp: '2026-04-06T13:42:00Z',
    unreadCount: 2,
    isOnline: true,
    messages: [
      {
        id: 'm-1',
        direction: 'incoming',
        kind: 'text',
        body: 'The orbital thread is stable. Ready for review.',
        createdAt: '2026-04-06T13:42:00Z',
      },
      {
        id: 'm-2',
        direction: 'outgoing',
        kind: 'text',
        body: 'Push the AIGC moodboard into the workshop and I will sync the feed.',
        createdAt: '2026-04-06T13:44:00Z',
      },
    ],
  },
  {
    id: 'room-aria',
    contactId: 'aria-thorne',
    contactName: 'Aria Thorne',
    contactTitle: 'Prompt Architect',
    avatarText: 'AT',
    lastMessage: 'Uploaded a portrait remix prompt set for the new creator lane.',
    lastTimestamp: '2026-04-05T20:12:00Z',
    unreadCount: 0,
    isOnline: true,
    messages: [
      {
        id: 'm-3',
        direction: 'incoming',
        kind: 'text',
        body: 'Uploaded a portrait remix prompt set for the new creator lane.',
        createdAt: '2026-04-05T20:12:00Z',
        chips: ['portrait', 'workshop'],
      },
    ],
  },
  {
    id: 'room-clara',
    contactId: 'clara-wu',
    contactName: 'Clara Wu',
    contactTitle: 'Visual Systems',
    avatarText: 'CW',
    lastMessage: 'The feed cards now use tonal depth instead of divider lines.',
    lastTimestamp: '2026-04-04T11:05:00Z',
    unreadCount: 1,
    isOnline: false,
    messages: [
      {
        id: 'm-4',
        direction: 'incoming',
        kind: 'text',
        body: 'The feed cards now use tonal depth instead of divider lines.',
        createdAt: '2026-04-04T11:05:00Z',
      },
    ],
  },
]

export const seedPosts: FeedPost[] = [
  {
    id: 'post-1',
    author: 'Neural Architect',
    role: 'Community Builder',
    title: 'Prompting image diffusion with structured motion notes',
    summary: 'A short guide for turning rough product intent into an image-to-image prompt that preserves posture and camera rhythm.',
    body: '# Structured Prompting\n\n> blueprint note\n\nUse **camera verbs**, then add the materials layer.\n\n```md\nshot: half-body portrait\nmaterials: chrome headset, matte coat\nlighting: electric indigo rim\n```',
    tags: ['prompting', 'image-to-image', 'workflow'],
    createdAt: '2026-04-06T09:10:00Z',
    accent: 'primary',
  },
  {
    id: 'post-2',
    author: 'Signal Garden',
    role: 'Frontend Lead',
    title: 'No-line layouts for mobile IM surfaces',
    summary: 'How to separate dense content with surface tiers instead of hard borders.',
    body: '## Surface Tiers\n\nTreat each card as a physical slab. Shift the background tone before you ever add a border.',
    tags: ['design-system', 'surface', 'mobile'],
    createdAt: '2026-04-05T18:20:00Z',
    accent: 'secondary',
  },
]

export const seedPrompts: WorkshopPrompt[] = [
  {
    id: 'prompt-1',
    title: 'Portrait Remix Starter',
    summary: 'Transforms a selfie into a premium cyber-minimal portrait.',
    prompt: 'Create a polished portrait remix with geometric shadows, indigo rim light, clean skin texture, and editorial negative space.',
    category: 'portrait',
    author: 'Aria Thorne',
    uses: 1420,
    mdxReady: true,
  },
  {
    id: 'prompt-2',
    title: 'Video Echo Motion',
    summary: 'Wraps a short clip with futuristic lighting and motion trails.',
    prompt: 'Turn this clip into a sleek concept reel with controlled motion blur, neon edge lighting, and studio-grade contrast.',
    category: 'video',
    author: 'Leo Vance',
    uses: 860,
    mdxReady: false,
  },
  {
    id: 'prompt-3',
    title: 'Code Artifact Poster',
    summary: 'Converts code fragments into graphical poster layouts.',
    prompt: 'Translate the code sample into a clean visual poster with monospaced hierarchy, layered cards, and subtle glitch accents.',
    category: 'code-art',
    author: 'Clara Wu',
    uses: 640,
    mdxReady: true,
  },
]

export const seedProviders = presetProviders
