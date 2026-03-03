import os
import subprocess
import requests
import re
import sys
import logging

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
        sys.exit(1)

def escape_markdown_v2(text):
    """Escapes special characters for Telegram MarkdownV2."""
    escape_chars = r'_*[]()~`>#+-=|{}.!'
    return re.sub(r'([%s])' % re.escape(escape_chars), r'\\\1', text)

def main():
    # Required environment variables
    bot_token = os.environ.get('BOT_TOKEN')
    chat_id = os.environ.get('CHAT_ID')

    if not bot_token or not chat_id:
        logger.error("BOT_TOKEN and CHAT_ID environment variables are required.")
        sys.exit(1)

    topic_id = os.environ.get('TOPIC_ID')
    github_actor = os.environ.get('GITHUB_ACTOR', '')
    repo_name = os.environ.get('GITHUB_REPOSITORY', 'StudioAsInc/synapse-android')

    commit_author, commit_message, commit_hash, commit_hash_short = get_git_commit_info()

    # Escape dynamic content
    safe_author = escape_markdown_v2(commit_author)
    safe_message = escape_markdown_v2(commit_message)

    # Constructing parts with correct escaping
    if github_actor:
        author_link = f"[{safe_author}](https://github.com/{github_actor})"
    else:
        author_link = f"*{safe_author}*"

    commit_link = f"[commit](https://github.com/{repo_name}/commit/{commit_hash})"

    # Use raw f-strings to avoid SyntaxWarning with backslashes
    final_message = (
        fr"A new {commit_link} has been merged to the repository by {author_link}\.\n\n"
        fr"*What has changed:*\n"
        fr">{safe_message}\n\n"
        fr"I'm currently building it and will send you the APKs here within ~5 mins if the build is successful\.\n\n"
        fr"\#{commit_hash_short}"
    )

    url = f"https://api.telegram.org/bot{bot_token}/sendMessage"
    payload = {
        "chat_id": chat_id,
        "text": final_message,
        "parse_mode": "markdownv2",
        "disable_web_page_preview": True
    }
    if topic_id:
        payload["message_thread_id"] = topic_id

    try:
        response = requests.post(url, json=payload, timeout=10)
        response.raise_for_status()
        logger.info("Message sent successfully.")
    except requests.exceptions.RequestException as e:
        logger.error(f"Failed to send message: {e}")
        if hasattr(e, 'response') and e.response is not None:
            logger.error(f"Response: {e.response.text}")
        sys.exit(1)

if __name__ == "__main__":
    main()
