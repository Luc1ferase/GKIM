package com.gkim.im.android.data.repository

import com.gkim.im.android.core.model.AccentTone
import com.gkim.im.android.core.model.AigcMode
import com.gkim.im.android.core.model.AigcProvider
import com.gkim.im.android.core.model.ChatMessage
import com.gkim.im.android.core.model.Contact
import com.gkim.im.android.core.model.Conversation
import com.gkim.im.android.core.model.FeedPost
import com.gkim.im.android.core.model.MessageDirection
import com.gkim.im.android.core.model.MessageKind
import com.gkim.im.android.core.model.PromptCategory
import com.gkim.im.android.core.model.WorkshopPrompt

val seedContacts = listOf(
    Contact("leo-vance", "Leo Vance", "Realtime Systems", "LV", "2026-04-02T09:00:00Z", true),
    Contact("aria-thorne", "Aria Thorne", "Prompt Architect", "AT", "2026-04-01T09:00:00Z", true),
    Contact("clara-wu", "Clara Wu", "Visual Systems", "CW", "2026-04-03T09:00:00Z", false),
)

val seedConversations = listOf(
    Conversation(
        id = "room-leo",
        contactId = "leo-vance",
        contactName = "Leo Vance",
        contactTitle = "Realtime Systems",
        avatarText = "LV",
        lastMessage = "The orbital thread is stable. Ready for review.",
        lastTimestamp = "2026-04-06T13:42:00Z",
        unreadCount = 2,
        isOnline = true,
        messages = listOf(
            ChatMessage("m-1", MessageDirection.Incoming, MessageKind.Text, "The orbital thread is stable. Ready for review.", "2026-04-06T13:42:00Z"),
            ChatMessage("m-2", MessageDirection.Outgoing, MessageKind.Text, "Push the AIGC moodboard into the workshop and I will sync the feed.", "2026-04-06T13:44:00Z"),
        ),
    ),
    Conversation(
        id = "room-aria",
        contactId = "aria-thorne",
        contactName = "Aria Thorne",
        contactTitle = "Prompt Architect",
        avatarText = "AT",
        lastMessage = "I added two cinematic presets that keep the subject locked on axis.",
        lastTimestamp = "2026-04-05T08:12:00Z",
        unreadCount = 0,
        isOnline = true,
        messages = listOf(
            ChatMessage("m-3", MessageDirection.Incoming, MessageKind.Text, "I added two cinematic presets that keep the subject locked on axis.", "2026-04-05T08:12:00Z"),
        ),
    ),
    Conversation(
        id = "room-clara",
        contactId = "clara-wu",
        contactName = "Clara Wu",
        contactTitle = "Visual Systems",
        avatarText = "CW",
        lastMessage = "The feed cards now use tonal depth instead of divider lines.",
        lastTimestamp = "2026-04-04T11:05:00Z",
        unreadCount = 1,
        isOnline = false,
        messages = listOf(
            ChatMessage("m-4", MessageDirection.Incoming, MessageKind.Text, "The feed cards now use tonal depth instead of divider lines.", "2026-04-04T11:05:00Z"),
        ),
    ),
)

val seedPosts = listOf(
    FeedPost(
        id = "post-1",
        author = "Neural Architect",
        role = "Community Builder",
        title = "Prompting image diffusion with structured motion notes",
        summary = "A short guide for turning rough product intent into an image-to-image prompt that preserves posture and camera rhythm.",
        body = "# Structured Prompting\n\n> blueprint note\n\nUse **camera verbs**, then add the materials layer.\n\n- shot: half-body portrait\n- materials: chrome headset, matte coat\n- lighting: electric indigo rim",
        tags = listOf("prompting", "image-to-image", "workflow"),
        createdAt = "2026-04-06T09:10:00Z",
        accent = AccentTone.Primary,
    ),
    FeedPost(
        id = "post-2",
        author = "Clara Wu",
        role = "Visual Systems",
        title = "Designing feed cards without divider lines",
        summary = "How tonal depth, offset typography, and asymmetry create stronger structure than 1px rules.",
        body = "## Tonal layers\n\nShift the surface instead of drawing a separator.\n\n```css\n.card { background: surface-container-low; }\n```",
        tags = listOf("design", "feed", "aether-mono"),
        createdAt = "2026-04-05T07:24:00Z",
        accent = AccentTone.Tertiary,
    ),
)

val seedPrompts = listOf(
    WorkshopPrompt(
        id = "prompt-1",
        title = "Editorial Indigo Portrait",
        summary = "Keeps the face stable while pushing reflective materials and neon indigo atmosphere.",
        prompt = "Create an editorial portrait with glass reflections, indigo rim light, and precise geometric framing.",
        category = PromptCategory.Portrait,
        author = "Aria Thorne",
        uses = 124,
        mdxReady = true,
    ),
    WorkshopPrompt(
        id = "prompt-2",
        title = "Kinetic Motion Reel",
        summary = "A video-to-video prompt starter for device footage that needs a sleek cyber-minimal finish.",
        prompt = "Transform the source clip into a kinetic architectural reel with stabilized camera rhythm and metallic light passes.",
        category = PromptCategory.Video,
        author = "Leo Vance",
        uses = 87,
        mdxReady = false,
    ),
    WorkshopPrompt(
        id = "prompt-3",
        title = "Code Artifact Hero Image",
        summary = "Turns terminal fragments and code metaphors into launch art.",
        prompt = "Generate a hero still that merges terminal glyphs, floating UI panes, and angular chrome surfaces in a premium editorial layout.",
        category = PromptCategory.CodeArt,
        author = "Neural Architect",
        uses = 63,
        mdxReady = true,
    ),
)

val presetProviders = listOf(
    AigcProvider("hunyuan", "Tencent Hunyuan", "Tencent", "Tencent image generation lane for local provider validation.", "hy-image-v3.0", AccentTone.Primary, true, setOf(AigcMode.TextToImage)),
    AigcProvider("tongyi", "Alibaba Tongyi", "Alibaba", "Alibaba image generation lane for iterative prompt experiments.", "wan2.7-image", AccentTone.Secondary, true, setOf(AigcMode.TextToImage, AigcMode.ImageToImage)),
    AigcProvider("custom", "Custom Endpoint", "OpenAI-Compatible", "Bring your own gateway, auth key, and model identifier.", "gpt-image-1", AccentTone.Tertiary, false, setOf(AigcMode.TextToImage, AigcMode.ImageToImage, AigcMode.VideoToVideo)),
)
