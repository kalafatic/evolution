from playwright.sync_api import sync_playwright
import os
import json

def run_verification(page):
    # Construct the path to chat.html
    chat_html_path = os.path.abspath("eu.kalafatic.evolution.view/chat.html")
    page.goto(f"file://{chat_html_path}")
    page.wait_for_timeout(1000)

    # Mock the Java bridge
    page.evaluate("""
        window.JavaHandler = function(action, index, payload) {
            console.log('JavaHandler called:', action, index, payload);
        };
        window.JavaLog = function(msg) {
            console.log('JavaLog:', msg);
        };
    """)

    # Mock messages to test the new rendering logic
    messages = [
        {
            "index": 0,
            "sender": "Evo",
            "text": '{"explanation": "This is a simple explanation that should be rendered as plain text."}',
            "timestamp": "12:00:00",
            "agentType": "ai"
        },
        {
            "index": 1,
            "sender": "Evo",
            "text": '{"clarificationQuestion": "What is the project root?", "id": "123", "confidence": 0.99}',
            "timestamp": "12:01:00",
            "agentType": "ai waiting"
        },
        {
            "index": 2,
            "sender": "Evo",
            "text": '{"thought": "I should check the pom.xml file.", "summary": "Checking pom.xml", "risk": "low"}',
            "timestamp": "12:02:00",
            "agentType": "ai"
        }
    ]

    # Trigger updateMessages
    page.evaluate(f"window.updateMessages({json.dumps(messages)})")
    page.wait_for_timeout(1000)

    # Take screenshot
    page.screenshot(path="verification/screenshots/renderer_verification.png")
    page.wait_for_timeout(1000)

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(
            record_video_dir="verification/videos"
        )
        page = context.new_page()
        try:
            run_verification(page)
        finally:
            context.close()
            browser.close()
