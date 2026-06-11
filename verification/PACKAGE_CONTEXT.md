# PACKAGE CONTEXT

## Directory: git/evolution/verification/

## Domain: general

## Components
* `verify_renderer.py`: from playwright.sync_api import sync_playwright import os def run_cuj(page): path = os.path.abspath("verification/test_renderer.html") page.goto(f"file://{path}") page.wait_for_timeout(1000) page.screenshot(path="verification/screenshots/renderer_fix.png") links = page.query_selector_all("a") print(f"Found {len(links)} links") for link in links: print(f"Link text: {link.inner_text()}") if __name__ == "__main__": with sync_playwright() as p: browser = p.chromium.launch(headless=True) context = browser.new_context(record_video_dir="verification/videos") page = context.new_page() try: run_cuj(page) finally: context.close()
