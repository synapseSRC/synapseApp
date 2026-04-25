import os
import subprocess
import requests
import sys
import logging
import html

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(levelname)s: %(message)s')
logger = logging.getLogger(__name__)

def get_git_commit_info():
    """Retrieves the latest git commit information."""
    try:
        commit_author = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%an']).decode('utf-8').strip()
        commit_message = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%s']).decode('utf-8').strip()
        commit_hash_short = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%h']).decode('utf-8').strip()
        return commit_author, commit_message, commit_hash_short
    except Exception as e:
        logger.error(f"Failed to get git commit info: {e}")
        return "Unknown", "No message", "0000000"

def main():
    # Load environment variables
    bot_token = os.environ.get('BOT_TOKEN')
    chat_id = os.environ.get('CHAT_ID')
    topic_id = os.environ.get('TOPIC_ID')
    github_actor = os.environ.get('GITHUB_ACTOR', '')
    build_status = os.environ.get('BUILD_STATUS', 'unknown').lower()
    repo_name = os.environ.get('GITHUB_REPOSITORY', 'Synapse')

    if not bot_token or not chat_id:
        logger.error("BOT_TOKEN and CHAT_ID are required.")
        sys.exit(1)

    # Get Git details
    raw_author, raw_message, commit_hash = get_git_commit_info()

    # ESCAPE strings for HTML safety
    author = html.escape(raw_author)
    msg_body = html.escape(raw_message)
    status_text = build_status.upper()

    # Determine Emoji based on status
    if build_status == "success":
        status_icon = "✅"
    elif build_status == "failure":
        status_icon = "❌"
    else:
        status_icon = "⚠️"

    # Construct the HTML Message
    # HTML mode is robust: no more backslash escaping needed for dots or dashes!
    author_link = f'<a href="https://github.com/{github_actor}">{author}</a>' if github_actor else f'<b>{author}</b>'
    
    final_message = (
        f"{status_icon} <b>{repo_name} Build {status_text}</b>\n\n"
        f"<b>Author:</b> {author_link}\n"
        f"<b>Commit:</b> <code>#{commit_hash}</code>\n"
        f"<b>What changed:</b>\n"
        f"<blockquote>{msg_body}</blockquote>\n\n"
        f'<a href="https://github.com/{repo_name}/actions">View Build Logs</a>'
    )

    # Send to Telegram
    url = f"https://api.telegram.org/bot{bot_token}/sendMessage"
    payload = {
        "chat_id": chat_id,
        "text": final_message,
        "parse_mode": "HTML",
        "disable_web_page_preview": True
    }
    if topic_id:
        payload["message_thread_id"] = topic_id

    try:
        response = requests.post(url, json=payload, timeout=10)
        response.raise_for_status()
        logger.info("Notification sent successfully.")
    except Exception as e:
        logger.error(f"Failed to send message: {e}")
        if hasattr(e, 'response') and e.response is not None:
            logger.error(f"Response: {e.response.text}")
        sys.exit(1)

if __name__ == "__main__":
    main()