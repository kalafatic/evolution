import asyncio
from playwright.async_api import async_playwright
import json

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

        # 2. Check initial workflow (GENERAL)
        await page.reload()
        workflow = await page.inner_text("#displayWorkflow")
        print(f"Initial workflow: {workflow}")

        # 3. Navigate to AI Chat
        print("Navigating to AI Chat...")
        await page.click("text=AI Chat")
        await page.wait_for_load_state("networkidle")
        print(f"URL after navigation: {page.url}")

        # 4. Go back to Portal and check workflow
        await page.goto("http://localhost:48080/dashboard.html")
        await page.wait_for_load_state("networkidle")
        workflow = await page.inner_text("#displayWorkflow")
        print(f"Workflow after Chat visit: {workflow}")

        # 5. Navigate to Forge
        print("Navigating to Forge...")
        await page.click("text=Forge")
        await page.wait_for_load_state("networkidle")

        # 6. Go back to Portal and check workflow
        await page.goto("http://localhost:48080/dashboard.html")
        await page.wait_for_load_state("networkidle")
        workflow = await page.inner_text("#displayWorkflow")
        print(f"Workflow after Forge visit: {workflow}")

        await browser.close()

if __name__ == "__main__":
    pass
