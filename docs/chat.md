# 💬 Synapse Chat Implementation Roadmap

This document outlines the current status and future roadmap for the chat messaging feature within the Synapse application.

---

## 🚀 Recently Implemented
- [x] Basic text messaging interface
- [x] Real-time message synchronization via Supabase
- [x] Message grouping (bubble styling based on sender and time)
- [x] Clickable links within messages
- [x] Delivery status indicators (Sent / Delivered / Read)
- [x] User presence display (Online/Offline/Last Seen)

---

## 🛠️ High Priority (Up Next)

### 📂 Media & Rich Content
- [ ] **Image Sending:** Support for selecting and sending images from gallery/camera.
- [ ] **Video Support:** Video selection, compression, and in-chat playback.
- [ ] **Voice Messages:** Record and send audio snippets with waveform visualization.
- [ ] **File Sharing:** Support for PDF, DOCX, and other common file formats.

### 🤫 Privacy & Control
- [ ] **Disappearing Messages:** Messages that automatically delete after a set time (e.g., 24h, 1 week, or after being read).
- [ ] **Message Editing:** Allow users to correct typos in sent messages.
- [ ] **Unsend/Delete:** Ability to delete messages for everyone or just for yourself.
- [ ] **Chat Lock:** Protect specific conversations with Biometric/PIN authentication.

---

## ⚡ Real-time Experience

### ⌨️ Interactive Features
- [ ] **Typing Indicators:** Show "User is typing..." in real-time.
- [ ] **Read Receipts:** Update message status to 'Read' when the recipient views the chat.
- [ ] **Emoji Reactions:** Quick reactions to messages (heart, thumbs up, etc.).
- [ ] **Reply Threading:** Swipe to reply or long-press to quote a previous message.

---

## 👥 Advanced Collaboration

### 🏢 Group Chats
- [ ] **Group Creation:** Multi-user conversation setup.
- [ ] **Admin Controls:** Promote/demote admins, remove members.
- [ ] **Group Settings:** Custom icons, descriptions, and invitation links.

### 📢 Channels (Broadcasting)
- [ ] **One-to-Many Broadcasting:** Admins post updates to an unlimited audience.
- [ ] **Subscriber Management:** Public and Private channel options.
- [ ] **Analytics:** View counts for broadcasted messages.
- [ ] **Channel Discovery:** Searchable directory for public channels.

### 🤖 AI-Powered Features
- [ ] **Smart Replies:** Context-aware quick reply suggestions.
- [ ] **Chat Summarization:** AI-generated summaries of long conversations or missed group chats.
- [ ] **Language Translation:** Real-time translation of incoming/outgoing messages.
- [ ] **AI Assistant:** Integration of a chat bot for tasks (reminders, search, generation).

### 📞 Voice & Video
- [ ] **1-on-1 Calls:** High-quality P2P voice and video calling.
- [ ] **Group Calls:** Voice and video conferencing for groups.
- [ ] **Screen Sharing:** Ability to share screen during video calls.
- [ ] **Floating Player:** Watch shared videos (YouTube/Local) in PIP mode while chatting.

### 💰 Monetization & Economy
- [ ] **Digital Gifts:** Send virtual gifts or "tips" within chats.
- [ ] **Premium Stickers:** Market for exclusive sticker packs and animated emojis.
- [ ] **Subscription Channels:** Paid access to exclusive channel content.

---

## ⚙️ Technical Enhancements

### 📦 Infrastructure & Performance
- [ ] **Pagination:** Efficiently load older messages as the user scrolls up.
- [ ] **Local Persistence:** Store messages locally for offline viewing and faster app launch.
- [ ] **Push Notifications:** Deep-linking to specific chats when a message is received.
- [ ] **E2E Encryption:** End-to-end encryption for maximum user privacy.

---

## 🎨 UI/UX Polish
- [ ] **Stickers & GIFs:** Integration with Giphy or custom sticker packs.
- [ ] **Message Search:** Search within a specific conversation or across all chats.
- [ ] **Custom Chat Wallpapers:** Allow users to set personalized backgrounds for chats.
- [ ] **Forwarding:** Easily share messages across different conversations.
- [ ] **Scheduled Messages:** Type now, send automatically at a specific time.
- [ ] **Reminders:** Set reminders on specific messages (long-press -> remind me).
- [ ] **Chat Folders:** Organize chats into categories (Personal, Work, Channels).

---

> [!TIP]
> Prioritize **Media Sending** and **Typing Indicators** next to enhance the core communication loop.
