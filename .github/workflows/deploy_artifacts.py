import os
import subprocess
import asyncio
import logging
import sys
import time
from telethon import TelegramClient
from telethon.errors import FloodWaitError

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(levelname)s: %(message)s')
logger = logging.getLogger(__name__)

def get_git_commit_info():
    """Retrieves the latest git commit information."""
    try:
        commit_author = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%an']).decode('utf-8').strip()
        commit_message = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%s']).decode('utf-8').strip()
        commit_hash = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%H']).decode('utf-8').strip()
        commit_hash_short = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%h']).decode('utf-8').strip()
        return commit_author, commit_message, commit_hash, commit_hash_short
    except subprocess.CalledProcessError as e:
        logger.error(f"Failed to get git commit info: {e}")
        return "Unknown", "No message", "0000000000000000000000000000000000000000", "0000000"

def human_readable_size(size, decimal_places=2):
    """Converts bytes to a human-readable string."""
    for unit in ['B', 'KB', 'MB', 'GB', 'TB']:
        if size < 1024.0:
            break
        size /= 1024.0
    return f"{size:.{decimal_places}f} {unit}"

async def progress_callback(current, total):
    """Callback to log upload progress."""
    progress_percentage = (current / total) * 100
    uploaded_size = human_readable_size(current)
    total_size = human_readable_size(total)
    sys.stdout.write(f"\r{progress_percentage:.2f}% uploaded - {uploaded_size}/{total_size}")
    sys.stdout.flush()

async def send_file(client, group_id, topic_id, file_path, commit_info, github_actor):
    """Sends the file to Telegram with the formatted caption."""
    if not os.path.exists(file_path):
        logger.error(f"File not found: {file_path}")
        return

    logger.info(f"Sending file: {file_path} to group {group_id} (topic {topic_id})")

    author_name, commit_message, _, commit_hash_short = commit_info

    # Hyperlink the author name to their GitHub profile
    if github_actor:
        author_display = f"[{author_name}](https://github.com/{github_actor})"
    else:
        author_display = author_name

    message = (
        f"**Commit by:** {author_display}\n"
        f"**Commit message:** {commit_message}\n"
        f"**Commit hash:** #{commit_hash_short}"
    )

    try:
        await client.send_file(
            entity=group_id,
            file=file_path,
            caption=message,
            parse_mode='markdown',
            progress_callback=progress_callback,
            reply_to=topic_id
        )
        logger.info("\nFile sent successfully.")
    except FloodWaitError as e:
        wait_time = e.seconds
        logger.warning(f"Rate limited by Telegram. Waiting {wait_time} seconds...")
        time.sleep(wait_time)
        # Retry once after waiting
        await client.send_file(
            entity=group_id,
            file=file_path,
            caption=message,
            parse_mode='markdown',
            progress_callback=progress_callback,
            reply_to=topic_id
        )
        logger.info("\nFile sent successfully after rate limit wait.")
    except Exception as e:
        logger.error(f"\nFailed to send file: {e}")

async def main():
    # Load and validate environment variables
    api_id_str = os.getenv("API_ID")
    api_hash = os.getenv("API_HASH")
    bot_token = os.getenv("BOT_TOKEN")
    group_id_str = os.getenv("CHAT_ID")
    topic_id_str = os.getenv("TOPIC_ID")
    apk_path = os.getenv("APK_PATH")
    github_actor = os.getenv("GITHUB_ACTOR")

    if not all([api_id_str, api_hash, bot_token, group_id_str, apk_path]):
        logger.error("Missing required environment variables.")
        sys.exit(1)

    try:
        api_id = int(api_id_str)
        group_id = int(group_id_str)
        topic_id = int(topic_id_str) if topic_id_str else None
    except ValueError as e:
        logger.error(f"Invalid numeric environment variable: {e}")
        sys.exit(1)

    # Cleanup last session if it exists
    session_file = "bot_session.session"
    if os.path.exists(session_file):
        try:
            os.remove(session_file)
        except OSError as e:
            logger.warning(f"Could not remove session file: {e}")

    commit_info = get_git_commit_info()

    client = TelegramClient('bot_session', api_id, api_hash)
    try:
        await client.start(bot_token=bot_token)
        await send_file(client, group_id, topic_id, apk_path, commit_info, github_actor)
    except FloodWaitError as e:
        wait_time = e.seconds
        logger.warning(f"Rate limited during client start. Waiting {wait_time} seconds...")
        time.sleep(wait_time)
        # Retry client start after waiting
        await client.start(bot_token=bot_token)
        await send_file(client, group_id, topic_id, apk_path, commit_info, github_actor)
    finally:
        await client.disconnect()

if __name__ == '__main__':
    asyncio.run(main())
