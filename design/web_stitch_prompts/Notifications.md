# Synapse Social: Notifications (Web)

These prompts are designed for **Stitch** to prototype the activity and notification center for Synapse Social on web.

## 🎨 Notification Strategy
- **Tabs:** "All", "Verified", "Mentions".
- **Visuals:** Icons for specific activity (heart for Like, arrow for Repost).
- **Grouped Notifications:** Combine multiple likes/reposts from different users into a single notification.

---

## 1. Notification Feed (Unified)
**Prompt:**
> Design a "Notifications" dashboard for a modern social media web app.
> - **Header:** "Notifications" headline with a "Settings" (gear) icon.
> - **Tabs:** "All", "Verified", "Mentions".
> - **Notification Card (Grouped):** A single card showing "User1, User2 and 5 others liked your post" with multiple small avatars overlapping.
> - **Individual Notification:** "UserA followed you" with a large "Seven Sided Cookie" avatar and a "Follow" button on the right.
> - **Aesthetics:** Minimalist layout, white background, violet icons (#6750A4), 14px secondary text.

---

## 2. Mentions & Replies
**Prompt:**
> Design a "Mentions" tab for the Synapse Social notification center.
> - **Visuals:** A list of full post cards (X/Twitter style) where the user was mentioned.
> - **Highlight:** The user's handle (@currentUser) is highlighted in violet in each post.
> - **Action:** "Reply" and "Share" buttons are prominent on each notification.
> - **Thread Preview:** Show a tiny vertical line connecting the mention to its parent post (if any).

---

## 3. Verified Activity
**Prompt:**
> Create a "Verified" notifications view for a social app.
> - **Visuals:** Similar to the "All" tab but exclusively for accounts with a blue/violet verified badge.
> - **Badge:** A special "Verified" icon (checkmark in a violet star/shield) next to usernames.
> - **Empty State:** Design a clean "Nothing to see here—yet" message with a decorative verified badge illustration.

---

## 4. Notification Settings (Desktop)
**Prompt:**
> Design a "Notification Settings" modal for "Synapse Social".
> - **Layout:** Two-column modal. Left column: "Push Notifications", "Email Notifications", "SMS Notifications".
> - **Content:** Toggle switches (Material 3 style) for "Likes", "Comments", "New Followers", "Direct Messages".
> - **Aesthetics:** High-precision, clean Material 3 design with violet accents.
