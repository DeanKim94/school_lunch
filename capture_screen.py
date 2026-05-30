import pygetwindow as gw
import pyautogui
import time
import os

def capture_window(title, filename):
    windows = gw.getWindowsWithTitle(title)
    if not windows:
        print(f"Window '{title}' not found.")
        return False
    
    win = windows[0]
    
    # Restore if minimized
    if win.isMinimized:
        win.restore()
        
    win.activate()
    time.sleep(1) # Wait for window to come to foreground
    
    # Capture the window area
    # left, top, width, height
    screenshot = pyautogui.screenshot(region=(win.left, win.top, win.width, win.height))
    screenshot.save(filename)
    print(f"Saved {filename}")
    return True

if __name__ == "__main__":
    title = "🍱 급식지도 자동 배치 시스템"
    os.makedirs('screenshots', exist_ok=True)
    capture_window(title, "screenshots/main_app.png")
