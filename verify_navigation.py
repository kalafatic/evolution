import asyncio
from playwright.async_api import async_playwright

async def run():
    async with async_playwright() as p:
        browser = await p.chromium.launch()
        context = await browser.new_context()
        page = await context.new_page()

        # 1. Login
        print("Logging in...")
        await page.goto("http://localhost:48080/login.html")
        await page.fill("#username", "admin")
        await page.fill("#password", "admin")
        await page.click("#loginBtn")

        await page.wait_for_url("**/dashboard.html")
        print("Logged in successfully.")

        # Check cookies
        cookies = await context.cookies()
        session_cookie = next((c for c in cookies if c['name'] == 'sessionId'), None)
        if session_cookie:
            print(f"Session cookie found: {session_cookie['value']}")
        else:
            print("Session cookie NOT found!")

        # 2. Navigate to AI Chat via link
        print("Navigating to AI Chat...")
        await page.click("text=AI Chat")

        await page.wait_for_load_state("networkidle")
        print(f"Current URL: {page.url}")

        if "login.html" in page.url:
            print("FAILED: Redirected to login page!")
        else:
            print("SUCCESS: Stayed logged in.")

        await browser.close()

if __name__ == "__main__":
    # We need a running server for this.
    # For now I'll just write it and I'll try to run it if I can start the server.
    pass
