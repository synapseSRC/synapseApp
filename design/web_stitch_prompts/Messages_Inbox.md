# Synapse Social: Messages Inbox (Web)

These prompts are designed for **Stitch** to prototype the desktop-optimized chat experience for Synapse Social.

## 🎨 Web Messaging Layout
- **Split Pane:** Fixed left column for the conversation list; right column for the active chat window.
- **Top Bar:** Sticky bar with the contact's name, avatar, and "Online" status.

---

## 1. Inbox Split-Pane Layout
**Prompt:**
> Design a 2-column desktop messaging interface for "Synapse Social".
> - **Left Column (1/3 width):** Scrollable conversation list. A "Messages" header at the top with a "New Message" (pencil icon) button. Search bar for "Search direct messages".
> - **Conversation Card:** User avatar, name (bold), timestamp (e.g., "Oct 12"), and a one-line preview of the last message.
> - **Active Conversation:** The currently selected chat is highlighted with a violet vertical line on the left.
> - **Right Column (2/3 width):** The active chat window (currently empty state for "Select a message to start chatting").

---

## 2. Active Chat Interface
**Prompt:**
> Design a detailed chat window for Synapse Social.
> - **Top Bar:** Contact avatar (circular), name, and a green "Online" dot. Icons for "Audio Call", "Video Call", and "Info".
> - **Message Bubbles:** Alternating left (incoming) and right (outgoing) bubbles.
>   - **Outgoing (User):** Violet background (#6750A4) with white text. Rounded corners except for the bottom-right.
>   - **Incoming:** Light gray background with black text. Rounded corners except for the bottom-left.
> - **Status:** Small "Delivered" or "Read" (double-check icons) at the bottom of the outgoing bubble.
> - **Input Bar:** Fixed at the bottom with a plus icon (attachment), a large text field, and a "Send" (arrow) button.

---

## 3. Attachment Menu & Media Sharing
**Prompt:**
> Create a popup attachment menu for a social chat.
> - **Context:** Triggered by a plus (+) icon.
> - **Options:** Grid of 4 large, colorful icons for "Photos", "Videos", "Files", and "Location".
> - **Visuals:** Material 3 styled, rounded corners, subtle shadows, and a "Close" (X) button at the top-right.

---

## 4. Message Search & Filtering
**Prompt:**
> Design a "Search in Conversation" feature for a chat screen.
> - **Layout:** An overlay search bar at the top of the chat window.
> - **Filtering:** Options to filter messages by "Media", "Links", or "Docs".
> - **Results:** Highlighting the searched text within the chat bubbles as the user scrolls.
