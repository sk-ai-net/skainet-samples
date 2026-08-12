#!/usr/bin/env python3
"""
SKaiNET Samples Page Generator

Generates a beautifully styled HTML landing page for SKaiNET sample applications.
Scans webapp.json files and creates a static HTML page with embedded CSS and JS.

Usage:
    # Local generation (preview only - no file copying)
    python scripts/generate-samples-page.py

    # Build mode (copies dist files and generates index)
    python scripts/generate-samples-page.py --build

    # Custom output directory
    python scripts/generate-samples-page.py --build --output ./dist

Environment variables:
    GITHUB_REF_NAME - Release tag name (optional, for display)
"""

import json
import os
import re
import shutil
import html
import argparse
import subprocess
from pathlib import Path
from datetime import datetime


def get_git_repo_url(root_dir: Path) -> str | None:
    """Get the GitHub browsable URL from git remote."""
    try:
        result = subprocess.run(
            ["git", "remote", "get-url", "origin"],
            cwd=root_dir,
            capture_output=True,
            text=True,
            check=True
        )
        remote_url = result.stdout.strip()

        # Convert SSH URL to HTTPS URL
        # git@github.com:user/repo.git -> https://github.com/user/repo
        if remote_url.startswith("git@"):
            match = re.match(r"git@([^:]+):(.+?)(?:\.git)?$", remote_url)
            if match:
                host, path = match.groups()
                return f"https://{host}/{path.rstrip('.git')}"

        # Already HTTPS URL
        # https://github.com/user/repo.git -> https://github.com/user/repo
        if remote_url.startswith("https://"):
            return remote_url.rstrip(".git").rstrip("/")

        return None
    except subprocess.CalledProcessError:
        return None


def get_git_default_branch(root_dir: Path) -> str:
    """Get the default branch name (main or master)."""
    try:
        # Try to get the default branch from remote
        result = subprocess.run(
            ["git", "remote", "show", "origin"],
            cwd=root_dir,
            capture_output=True,
            text=True,
            check=True
        )
        for line in result.stdout.splitlines():
            if "HEAD branch:" in line:
                return line.split(":")[-1].strip()
    except subprocess.CalledProcessError:
        pass

    # Fallback: check if main or master exists
    try:
        result = subprocess.run(
            ["git", "branch", "-r"],
            cwd=root_dir,
            capture_output=True,
            text=True,
            check=True
        )
        branches = result.stdout
        if "origin/main" in branches:
            return "main"
        if "origin/master" in branches:
            return "master"
    except subprocess.CalledProcessError:
        pass

    return "main"  # Default fallback


def find_webapp_configs(root_dir: Path, repo_url: str | None = None, branch: str = "main") -> list:
    """Find all webapp.json files and parse them."""
    apps = []

    for webapp_json in root_dir.rglob("webapp.json"):
        # Skip node_modules and other common directories
        if any(part in webapp_json.parts for part in ["node_modules", ".git", "build"]):
            continue

        try:
            with open(webapp_json, "r", encoding="utf-8") as f:
                meta = json.load(f)

            app_id = meta.get("id")
            if not app_id:
                print(f"[WARN] {webapp_json} missing 'id', skipping.")
                continue

            project_root = webapp_json.parent

            # Compute source URL from git repo URL + relative path
            source_url = meta.get("sourceUrl")  # Allow override from webapp.json
            if not source_url and repo_url:
                try:
                    rel_path = project_root.relative_to(root_dir)
                    source_url = f"{repo_url}/tree/{branch}/{rel_path}"
                    print(f"[OK] Source URL: {source_url}")
                except ValueError:
                    pass  # project_root is not relative to root_dir

            apps.append({
                "id": app_id,
                "name": meta.get("name", app_id),
                "description": meta.get("description", ""),
                "screenshot": meta.get("screenshot"),
                "distDirs": meta.get("distDirs", []),
                "platforms": meta.get("platforms", ["web"]),
                "sourceUrl": source_url,
                "project_root": project_root
            })
            print(f"[OK] Found app: {app_id} in {project_root}")

        except Exception as e:
            print(f"[WARN] Cannot parse {webapp_json}: {e}")

    return sorted(apps, key=lambda a: a["id"])


def copy_dist_files(apps: list, output_dir: Path):
    """Copy distribution files for each app."""
    for app in apps:
        app_id = app["id"]
        project_root = app["project_root"]
        target_dir = output_dir / app_id

        # Clean and create target directory
        if target_dir.exists():
            shutil.rmtree(target_dir)
        target_dir.mkdir(parents=True)

        # Copy dist directories
        for rel_path in app["distDirs"]:
            src_dir = project_root / rel_path
            if not src_dir.exists():
                print(f"[INFO] distDir does not exist (skipped): {src_dir}")
                continue

            print(f"[OK] Copying {src_dir} -> {target_dir}")
            for item in src_dir.rglob("*"):
                if item.is_file():
                    rel = item.relative_to(src_dir)
                    dest = target_dir / rel
                    dest.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(item, dest)

        # Copy screenshot
        if app["screenshot"]:
            screenshot_src = project_root / app["screenshot"]
            if screenshot_src.exists():
                screenshot_name = screenshot_src.name
                shutil.copy2(screenshot_src, target_dir / screenshot_name)
                app["screenshot_url"] = f"{app_id}/{screenshot_name}"
            else:
                print(f"[WARN] Screenshot not found: {screenshot_src}")
                app["screenshot_url"] = None
        else:
            app["screenshot_url"] = None


def copy_assets(output_dir: Path, root_dir: Path):
    """Copy shared assets like logo to the output directory."""
    assets_dir = output_dir / "assets"
    assets_dir.mkdir(parents=True, exist_ok=True)

    # Copy logo
    logo_src = root_dir / "templates" / "SKaiNET-simple.png"
    if logo_src.exists():
        shutil.copy2(logo_src, assets_dir / "logo.png")
        print(f"[OK] Copied logo to {assets_dir / 'logo.png'}")
    else:
        print(f"[WARN] Logo not found: {logo_src}")


def generate_app_wrapper(app: dict) -> str:
    """Generate a wrapper HTML page for an individual app with header/footer."""
    app_name = html.escape(app["name"])
    app_id = html.escape(app["id"])
    source_url = app.get("sourceUrl", "")

    source_btn = ""
    if source_url:
        source_btn = f'''<a href="{html.escape(source_url)}" class="btn btn-outline" target="_blank" rel="noopener noreferrer">
          <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4"></path>
            <path d="M9 18c-4.51 2-5-2-7-2"></path>
          </svg>
          Source
        </a>'''

    return f'''<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>{app_name} - SKaiNET Examples</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Orbitron:wght@400;500;600;700&family=Inter:wght@300;400;500&display=swap" rel="stylesheet">
  <style>
    :root {{
      --background: 220 20% 4%;
      --foreground: 0 0% 95%;
      --primary: 0 72% 51%;
      --primary-foreground: 0 0% 100%;
      --muted: 220 15% 12%;
      --muted-foreground: 220 10% 50%;
      --border: 220 15% 18%;
      --radius: 0.5rem;
    }}
    .light {{
      --background: 0 0% 98%;
      --foreground: 220 20% 10%;
      --muted: 220 15% 95%;
      --muted-foreground: 220 10% 45%;
      --border: 220 15% 88%;
    }}
    *, *::before, *::after {{ box-sizing: border-box; margin: 0; padding: 0; }}
    html, body {{ height: 100%; }}
    body {{
      font-family: 'Inter', sans-serif;
      background-color: hsl(var(--background));
      color: hsl(var(--foreground));
      display: flex;
      flex-direction: column;
    }}
    .font-orbitron {{ font-family: 'Orbitron', sans-serif; }}
    .header {{
      border-bottom: 1px solid hsl(var(--border) / 0.5);
      background-color: hsl(var(--background));
      flex-shrink: 0;
    }}
    .header-content {{
      max-width: 1400px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0.75rem 1rem;
      gap: 1rem;
      flex-wrap: wrap;
    }}
    .header-left {{
      display: flex;
      align-items: center;
      gap: 1rem;
    }}
    .header-right {{
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;
    }}
    .logo-link {{
      display: flex;
      align-items: center;
      gap: 0.5rem;
      text-decoration: none;
    }}
    .logo-link:hover {{ opacity: 0.8; }}
    .logo-img {{ height: 2rem; width: auto; }}
    .logo-text {{ font-size: 1.1rem; font-weight: 700; color: hsl(var(--foreground)); }}
    .logo-text .highlight {{ color: hsl(var(--primary)); }}
    .divider {{
      width: 1px;
      height: 1.5rem;
      background-color: hsl(var(--border));
      margin: 0 0.25rem;
    }}
    .app-title {{
      font-size: 1rem;
      font-weight: 500;
      color: hsl(var(--muted-foreground));
    }}
    .btn {{
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.375rem;
      padding: 0.5rem 0.875rem;
      min-height: 36px;
      font-size: 0.8125rem;
      font-weight: 500;
      border-radius: var(--radius);
      cursor: pointer;
      transition: all 0.2s;
      text-decoration: none;
      border: none;
      white-space: nowrap;
    }}
    .btn-ghost {{
      background: transparent;
      color: hsl(var(--foreground));
    }}
    .btn-ghost:hover {{ background-color: hsl(var(--muted)); }}
    .btn-outline {{
      background: transparent;
      border: 1px solid hsl(var(--border));
      color: hsl(var(--foreground));
    }}
    .btn-outline:hover {{ background-color: hsl(var(--muted)); }}
    .icon {{ width: 0.875rem; height: 0.875rem; }}
    .app-frame {{
      flex: 1;
      border: none;
      width: 100%;
      background: white;
    }}
    .footer {{
      border-top: 1px solid hsl(var(--border) / 0.5);
      padding: 0.75rem 1rem;
      text-align: center;
      font-size: 0.75rem;
      color: hsl(var(--muted-foreground));
      flex-shrink: 0;
    }}
    .theme-toggle {{
      position: fixed;
      bottom: 1rem;
      right: 1rem;
      z-index: 50;
    }}
    .theme-toggle-btn {{
      display: flex;
      align-items: center;
      justify-content: center;
      width: 2.25rem;
      height: 2.25rem;
      border-radius: 50%;
      border: 1px solid hsl(var(--border));
      background-color: hsl(var(--background));
      color: hsl(var(--foreground));
      cursor: pointer;
      transition: all 0.2s;
    }}
    .theme-toggle-btn:hover {{ background-color: hsl(var(--muted)); }}
    @media (max-width: 600px) {{
      .header-content {{ padding: 0.5rem 0.75rem; }}
      .divider {{ display: none; }}
      .app-title {{ display: none; }}
      .btn {{ padding: 0.4rem 0.6rem; font-size: 0.75rem; min-height: 32px; }}
    }}
  </style>
</head>
<body>
  <div class="theme-toggle">
    <button class="theme-toggle-btn" onclick="toggleTheme()" aria-label="Toggle theme">
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="4"></circle>
        <path d="M12 2v2"></path><path d="M12 20v2"></path>
        <path d="m4.93 4.93 1.41 1.41"></path><path d="m17.66 17.66 1.41 1.41"></path>
        <path d="M2 12h2"></path><path d="M20 12h2"></path>
        <path d="m6.34 17.66-1.41 1.41"></path><path d="m19.07 4.93-1.41 1.41"></path>
      </svg>
    </button>
  </div>

  <header class="header">
    <div class="header-content">
      <div class="header-left">
        <a href="https://skainet.sk" class="logo-link">
          <img src="../assets/logo.png" alt="SKaiNET" class="logo-img">
          <span class="font-orbitron logo-text">SK<span class="highlight">ai</span>NET</span>
        </a>
        <div class="divider"></div>
        <span class="app-title">{app_name}</span>
      </div>
      <div class="header-right">
        {source_btn}
        <a href="../" class="btn btn-ghost">
          <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="m12 19-7-7 7-7"></path>
            <path d="M19 12H5"></path>
          </svg>
          All Examples
        </a>
      </div>
    </div>
  </header>

  <iframe src="app.html" class="app-frame" title="{app_name}"></iframe>

  <footer class="footer">
    &copy; {datetime.now().year} SKaiNET. All rights reserved.
  </footer>

  <script>
    function toggleTheme() {{
      document.documentElement.classList.toggle('light');
      localStorage.setItem('theme', document.documentElement.classList.contains('light') ? 'light' : 'dark');
    }}
    (function() {{
      if (localStorage.getItem('theme') === 'light') {{
        document.documentElement.classList.add('light');
      }}
    }})();
  </script>
</body>
</html>
'''


def wrap_app_pages(apps: list, output_dir: Path):
    """Wrap each app's index.html with a template containing header/footer."""
    for app in apps:
        app_id = app["id"]
        app_dir = output_dir / app_id
        index_path = app_dir / "index.html"

        if not index_path.exists():
            print(f"[WARN] No index.html found for {app_id}, skipping wrapper")
            continue

        # Rename original index.html to app.html
        app_path = app_dir / "app.html"
        shutil.move(index_path, app_path)
        print(f"[OK] Renamed {app_id}/index.html -> app.html")

        # Generate wrapper index.html
        wrapper_html = generate_app_wrapper(app)
        with open(index_path, "w", encoding="utf-8") as f:
            f.write(wrapper_html)
        print(f"[OK] Created wrapper for {app_id}")


def generate_html(apps: list, release_tag: str = "", base_url: str = "https://examples.skainet.sk") -> str:
    """Generate the complete HTML page with embedded CSS and JS."""

    # Generate project cards data for JavaScript
    projects_json = json.dumps([
        {
            "id": app["id"],
            "name": app["name"],
            "description": app["description"],
            "screenshot": app.get("screenshot_url") or app.get("screenshot"),
            "sourceUrl": app.get("sourceUrl"),
            "platforms": app.get("platforms", ["web"]),
            # Only samples that actually ship a web dist get a live demo link.
            "demoUrl": f"./{app['id']}/" if app.get("distDirs") else None
        }
        for app in apps
    ], indent=2)

    release_info = f'<span class="release-tag">Release: {html.escape(release_tag)}</span>' if release_tag else ""

    return f'''<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Example Projects - SKaiNET</title>
  <meta name="description" content="Explore sample applications built with SKaiNET ML framework for Kotlin Multiplatform">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Orbitron:wght@400;500;600;700&family=Inter:wght@300;400;500&display=swap" rel="stylesheet">
  <style>
    /* CSS Variables - Dark theme (default) */
    :root {{
      --background: 220 20% 4%;
      --foreground: 0 0% 95%;
      --card: 220 18% 8%;
      --card-foreground: 0 0% 95%;
      --primary: 0 72% 51%;
      --primary-foreground: 0 0% 100%;
      --secondary: 220 15% 15%;
      --secondary-foreground: 0 0% 85%;
      --muted: 220 15% 12%;
      --muted-foreground: 220 10% 50%;
      --border: 220 15% 18%;
      --radius: 0.5rem;
    }}

    /* Light theme */
    .light {{
      --background: 0 0% 98%;
      --foreground: 220 20% 10%;
      --card: 0 0% 100%;
      --card-foreground: 220 20% 10%;
      --primary: 0 72% 51%;
      --primary-foreground: 0 0% 100%;
      --secondary: 220 15% 95%;
      --secondary-foreground: 220 20% 20%;
      --muted: 220 15% 95%;
      --muted-foreground: 220 10% 45%;
      --border: 220 15% 88%;
    }}

    /* Reset & Base */
    *, *::before, *::after {{
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }}

    body {{
      font-family: 'Inter', sans-serif;
      background-color: hsl(var(--background));
      color: hsl(var(--foreground));
      min-height: 100vh;
      -webkit-font-smoothing: antialiased;
      -moz-osx-font-smoothing: grayscale;
    }}

    .font-orbitron {{
      font-family: 'Orbitron', sans-serif;
    }}

    /* Layout */
    .container {{
      max-width: 1400px;
      margin: 0 auto;
      padding: 0 1rem;
    }}

    /* Header */
    .header {{
      border-bottom: 1px solid hsl(var(--border) / 0.5);
      background-color: hsl(var(--background) / 0.8);
      backdrop-filter: blur(8px);
      position: sticky;
      top: 0;
      z-index: 40;
    }}

    .header-content {{
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1rem;
    }}

    .logo-link {{
      display: flex;
      align-items: center;
      gap: 0.75rem;
      text-decoration: none;
      transition: opacity 0.2s;
    }}

    .logo-link:hover {{
      opacity: 0.8;
    }}

    .logo-img {{
      height: 2.5rem;
      width: auto;
    }}

    .logo-text {{
      font-size: 1.25rem;
      font-weight: 700;
      color: hsl(var(--foreground));
    }}

    .logo-text .highlight {{
      color: hsl(var(--primary));
    }}

    /* Theme Toggle */
    .theme-toggle {{
      position: fixed;
      bottom: 1rem;
      right: 1rem;
      z-index: 50;
    }}

    .theme-toggle-btn {{
      display: flex;
      align-items: center;
      justify-content: center;
      width: 2.5rem;
      height: 2.5rem;
      border-radius: 50%;
      border: 1px solid hsl(var(--border));
      background-color: hsl(var(--card));
      color: hsl(var(--foreground));
      cursor: pointer;
      transition: all 0.2s;
    }}

    .theme-toggle-btn:hover {{
      background-color: hsl(var(--muted));
    }}

    /* Buttons - Mobile-friendly touch targets */
    .btn {{
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      padding: 0.75rem 1.25rem;
      min-height: 44px; /* iOS recommended touch target */
      font-size: 0.9rem;
      font-weight: 500;
      border-radius: var(--radius);
      cursor: pointer;
      transition: all 0.2s;
      text-decoration: none;
      border: none;
    }}

    .btn-ghost {{
      background: transparent;
      color: hsl(var(--foreground));
    }}

    .btn-ghost:hover {{
      background-color: hsl(var(--muted));
    }}

    .btn-outline {{
      background: transparent;
      border: 1px solid hsl(var(--border));
      color: hsl(var(--foreground));
    }}

    .btn-outline:hover {{
      background-color: hsl(var(--muted));
    }}

    .btn-primary {{
      background-color: hsl(var(--primary));
      color: hsl(var(--primary-foreground));
      border: none;
    }}

    .btn-primary:hover {{
      background-color: hsl(var(--primary) / 0.9);
    }}

    .btn:disabled {{
      opacity: 0.5;
      cursor: not-allowed;
    }}

    .icon {{
      width: 1rem;
      height: 1rem;
    }}

    /* Content Section - Mobile optimized */
    .content {{
      padding: 2rem 1rem;
    }}

    @media (min-width: 640px) {{
      .content {{
        padding: 3rem 1.5rem;
      }}
    }}

    .content-header {{
      text-align: center;
      margin-bottom: 2rem;
    }}

    @media (min-width: 640px) {{
      .content-header {{
        margin-bottom: 3rem;
      }}
    }}

    .content-title {{
      font-size: 1.75rem;
      font-weight: 700;
      color: hsl(var(--foreground));
    }}

    @media (min-width: 640px) {{
      .content-title {{
        font-size: 2.25rem;
      }}
    }}

    @media (min-width: 768px) {{
      .content-title {{
        font-size: 3rem;
      }}
    }}

    .content-subtitle {{
      margin-top: 1rem;
      font-size: 1.125rem;
      color: hsl(var(--muted-foreground));
    }}

    .release-tag {{
      display: inline-block;
      margin-top: 0.75rem;
      padding: 0.25rem 0.75rem;
      font-size: 0.75rem;
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      background-color: hsl(var(--primary) / 0.1);
      color: hsl(var(--primary));
      border-radius: 999px;
      border: 1px solid hsl(var(--primary) / 0.3);
    }}

    /* Cards Grid - Mobile first, scalable layout */
    .cards-grid {{
      display: grid;
      gap: 1.5rem;
      grid-template-columns: 1fr;
      max-width: 500px;
      margin: 0 auto;
    }}

    /* Tablet: 2 cards side by side, centered */
    @media (min-width: 640px) {{
      .cards-grid {{
        grid-template-columns: repeat(auto-fit, minmax(min(100%, 320px), 1fr));
        max-width: 720px;
      }}
    }}

    /* Desktop: allow up to 3 cards, centered */
    @media (min-width: 1024px) {{
      .cards-grid {{
        grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
        max-width: 1100px;
      }}
    }}

    /* Large desktop: cap at 3 columns for readability */
    @media (min-width: 1200px) {{
      .cards-grid {{
        grid-template-columns: repeat(3, 1fr);
        max-width: 1200px;
      }}
    }}

    /* Project Card */
    .project-card {{
      position: relative;
      display: flex;
      flex-direction: column;
      height: 100%;
      overflow: hidden;
      border-radius: var(--radius);
      border: 1px solid hsl(var(--border) / 0.5);
      background-color: hsl(var(--card) / 0.5);
      backdrop-filter: blur(8px);
      transition: all 0.3s;
    }}

    .project-card:hover {{
      border-color: hsl(var(--primary) / 0.5);
      box-shadow: 0 10px 40px hsl(var(--primary) / 0.1);
      transform: translateY(-4px);
    }}

    .project-card::before {{
      content: '';
      position: absolute;
      inset: 0;
      background: linear-gradient(to bottom right, hsl(var(--primary) / 0.05), transparent);
      opacity: 0;
      transition: opacity 0.3s;
      pointer-events: none;
    }}

    .project-card:hover::before {{
      opacity: 1;
    }}

    /* Card Screenshot */
    .card-screenshot {{
      position: relative;
      aspect-ratio: 16 / 9;
      width: 100%;
      overflow: hidden;
      background-color: hsl(var(--muted) / 0.3);
    }}

    .card-screenshot img {{
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s;
    }}

    .project-card:hover .card-screenshot img {{
      transform: scale(1.05);
    }}

    .card-screenshot-placeholder {{
      display: flex;
      height: 100%;
      width: 100%;
      align-items: center;
      justify-content: center;
    }}

    .card-screenshot-placeholder-content {{
      text-align: center;
      color: hsl(var(--muted-foreground) / 0.5);
    }}

    .card-screenshot-placeholder-icon {{
      margin: 0 auto 0.5rem;
      height: 3rem;
      width: 3rem;
      border-radius: var(--radius);
      background-color: hsl(var(--muted) / 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
    }}

    .card-screenshot-placeholder-text {{
      font-size: 0.75rem;
    }}

    .card-screenshot-overlay {{
      position: absolute;
      inset: 0;
      background: linear-gradient(to top, hsl(var(--card) / 0.8), transparent);
    }}

    /* Card Header */
    .card-header {{
      position: relative;
      flex-grow: 1;
      padding: 1.5rem;
      padding-bottom: 0.5rem;
    }}

    .card-header-row {{
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 0.5rem;
    }}

    .card-title {{
      font-size: 1.125rem;
      font-weight: 600;
      color: hsl(var(--foreground));
    }}

    .card-badge {{
      display: inline-flex;
      align-items: center;
      padding: 0.25rem 0.5rem;
      font-size: 0.625rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: hsl(var(--primary));
      background-color: hsl(var(--primary) / 0.1);
      border-radius: 999px;
      white-space: nowrap;
    }}

    .card-description {{
      margin-top: 0.5rem;
      font-size: 0.875rem;
      color: hsl(var(--muted-foreground));
      line-height: 1.5;
    }}

    .platform-chips {{
      display: flex;
      flex-wrap: wrap;
      gap: 0.375rem;
      margin-top: 0.75rem;
    }}

    .platform-chip {{
      display: inline-flex;
      align-items: center;
      padding: 0.2rem 0.55rem;
      font-size: 0.625rem;
      font-weight: 500;
      text-transform: capitalize;
      color: hsl(var(--muted-foreground));
      background-color: hsl(var(--muted) / 0.6);
      border: 1px solid hsl(var(--border));
      border-radius: 999px;
      white-space: nowrap;
    }}

    /* Card Footer - Stack on mobile, side-by-side on larger */
    .card-footer {{
      position: relative;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      padding: 1rem 1.5rem 1.5rem;
    }}

    .card-footer .btn {{
      flex: 1;
      width: 100%;
    }}

    @media (min-width: 400px) {{
      .card-footer {{
        flex-direction: row;
      }}
      .card-footer .btn {{
        width: auto;
      }}
    }}

    /* Footer */
    .footer {{
      border-top: 1px solid hsl(var(--border) / 0.5);
      padding: 2rem 1rem;
      text-align: center;
    }}

    .footer-content {{
      font-size: 0.875rem;
      color: hsl(var(--muted-foreground));
    }}

    /* Empty State */
    .empty-state {{
      text-align: center;
      padding: 4rem 2rem;
      color: hsl(var(--muted-foreground));
    }}

    .empty-state-icon {{
      font-size: 3rem;
      margin-bottom: 1rem;
    }}
  </style>
</head>
<body>
  <!-- Theme Toggle -->
  <div class="theme-toggle">
    <button class="theme-toggle-btn" onclick="toggleTheme()" aria-label="Toggle theme">
      <svg class="icon-sun" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="4"></circle>
        <path d="M12 2v2"></path>
        <path d="M12 20v2"></path>
        <path d="m4.93 4.93 1.41 1.41"></path>
        <path d="m17.66 17.66 1.41 1.41"></path>
        <path d="M2 12h2"></path>
        <path d="M20 12h2"></path>
        <path d="m6.34 17.66-1.41 1.41"></path>
        <path d="m19.07 4.93-1.41 1.41"></path>
      </svg>
    </button>
  </div>

  <!-- Header -->
  <header class="header">
    <div class="container header-content">
      <a href="https://skainet.sk" class="logo-link">
        <img src="assets/logo.png" alt="SKaiNET" class="logo-img">
        <span class="font-orbitron logo-text">SK<span class="highlight">ai</span>NET</span>
      </a>

      <a href="https://skainet.sk" class="btn btn-ghost">
        <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="m12 19-7-7 7-7"></path>
          <path d="M19 12H5"></path>
        </svg>
        Back to Home
      </a>
    </div>
  </header>

  <!-- Content -->
  <main class="content">
    <div class="container">
      <div class="content-header">
        <h1 class="font-orbitron content-title">Example Projects</h1>
        <p class="content-subtitle">Explore sample applications built with SKaiNET ML framework</p>
        {release_info}
      </div>

      <div id="cards-container" class="cards-grid">
        <!-- Cards will be rendered here dynamically -->
      </div>
    </div>
  </main>

  <!-- Footer -->
  <footer class="footer">
    <div class="container footer-content">
      &copy; {datetime.now().year} SKaiNET. All rights reserved.
    </div>
  </footer>

  <!-- SVG Icon Templates -->
  <template id="github-icon">
    <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4"></path>
      <path d="M9 18c-4.51 2-5-2-7-2"></path>
    </svg>
  </template>

  <template id="play-icon">
    <svg class="icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <polygon points="6 3 20 12 6 21 6 3"></polygon>
    </svg>
  </template>

  <script>
    // Theme management
    function toggleTheme() {{
      document.documentElement.classList.toggle('light');
      localStorage.setItem('theme', document.documentElement.classList.contains('light') ? 'light' : 'dark');
    }}

    // Initialize theme from localStorage
    (function() {{
      const savedTheme = localStorage.getItem('theme');
      if (savedTheme === 'light') {{
        document.documentElement.classList.add('light');
      }}
    }})();

    // Escape HTML to prevent XSS
    function escapeHtml(str) {{
      if (!str) return '';
      const div = document.createElement('div');
      div.textContent = str;
      return div.innerHTML;
    }}

    // Render project cards
    function renderProjectCards(projects) {{
      const container = document.getElementById('cards-container');
      if (!container) return;

      if (projects.length === 0) {{
        container.innerHTML = `
          <div class="empty-state" style="grid-column: 1 / -1;">
            <div class="empty-state-icon">📦</div>
            <p>No sample applications found.</p>
          </div>
        `;
        return;
      }}

      const githubIcon = document.getElementById('github-icon').innerHTML;
      const playIcon = document.getElementById('play-icon').innerHTML;

      container.innerHTML = projects.map(project => {{
        const screenshotHtml = project.screenshot
          ? `<img src="${{escapeHtml(project.screenshot)}}" alt="${{escapeHtml(project.name)}} screenshot">`
          : `<div class="card-screenshot-placeholder">
              <div class="card-screenshot-placeholder-content">
                <div class="card-screenshot-placeholder-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <rect width="18" height="18" x="3" y="3" rx="2" ry="2"></rect>
                    <circle cx="9" cy="9" r="2"></circle>
                    <path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"></path>
                  </svg>
                </div>
                <p class="card-screenshot-placeholder-text">No preview</p>
              </div>
            </div>`;

        const sourceBtn = project.sourceUrl
          ? `<a href="${{escapeHtml(project.sourceUrl)}}" class="btn btn-outline" target="_blank" rel="noopener noreferrer">
              ${{githubIcon}}
              Source
            </a>`
          : `<button class="btn btn-outline" disabled>
              ${{githubIcon}}
              Source
            </button>`;

        const demoBtn = project.demoUrl
          ? `<a href="${{escapeHtml(project.demoUrl)}}" class="btn btn-primary">
              ${{playIcon}}
              Try Demo
            </a>`
          : (project.sourceUrl
              ? `<a href="${{escapeHtml(project.sourceUrl)}}" class="btn btn-outline" target="_blank" rel="noopener noreferrer">
                  How to run
                </a>`
              : `<button class="btn btn-outline" disabled>
                  How to run
                </button>`);

        const platformChips = (project.platforms || ['web']).map(p =>
          `<span class="platform-chip">${{escapeHtml(p)}}</span>`
        ).join('');

        return `
          <article class="project-card" data-project-id="${{escapeHtml(project.id)}}">
            <div class="card-screenshot">
              ${{screenshotHtml}}
              <div class="card-screenshot-overlay"></div>
            </div>
            <div class="card-header">
              <div class="card-header-row">
                <h2 class="card-title">${{escapeHtml(project.name)}}</h2>
                <span class="card-badge">${{escapeHtml(project.id)}}</span>
              </div>
              <p class="card-description">${{escapeHtml(project.description)}}</p>
              <div class="platform-chips">${{platformChips}}</div>
            </div>
            <div class="card-footer">
              ${{sourceBtn}}
              ${{demoBtn}}
            </div>
          </article>
        `;
      }}).join('');
    }}

    // Projects data (generated by build script)
    const projects = {projects_json};

    // Render on load
    document.addEventListener('DOMContentLoaded', function() {{
      renderProjectCards(projects);
    }});
  </script>
</body>
</html>
'''


def main():
    parser = argparse.ArgumentParser(description="Generate SKaiNET samples landing page")
    parser.add_argument("--build", action="store_true", help="Build mode: copy dist files and generate index")
    parser.add_argument("--output", "-o", type=str, default="site", help="Output directory (default: site)")
    parser.add_argument("--root", "-r", type=str, default=".", help="Root directory to scan for webapp.json")
    parser.add_argument("--branch", "-b", type=str, default=None, help="Git branch for source links (auto-detected if not set)")
    args = parser.parse_args()

    root_dir = Path(args.root).resolve()
    output_dir = Path(args.output).resolve()
    release_tag = os.environ.get("GITHUB_REF_NAME", "")

    # Get git repository info for source links
    repo_url = get_git_repo_url(root_dir)
    if repo_url:
        print(f"[OK] Git repo URL: {repo_url}")
    else:
        print("[WARN] Could not detect git remote URL. Source links will be disabled.")

    branch = args.branch or get_git_default_branch(root_dir)
    print(f"[OK] Using branch: {branch}")

    print(f"[INFO] Scanning for webapp.json in: {root_dir}")
    apps = find_webapp_configs(root_dir, repo_url, branch)

    if not apps:
        print("[WARN] No valid webapp.json files found.")

    if args.build:
        print(f"[INFO] Build mode: copying dist files to {output_dir}")
        output_dir.mkdir(parents=True, exist_ok=True)
        copy_dist_files(apps, output_dir)
        copy_assets(output_dir, root_dir)
        # Note: App wrappers are now in source (wasmJsMain/resources/index.html)
        # No post-build wrapping needed
    else:
        # Preview mode: set screenshot URLs for local testing
        for app in apps:
            if app["screenshot"]:
                app["screenshot_url"] = str(app["project_root"] / app["screenshot"])
            else:
                app["screenshot_url"] = None

    # Generate HTML
    html_content = generate_html(apps, release_tag)

    # Write to output
    output_dir.mkdir(parents=True, exist_ok=True)
    index_path = output_dir / "index.html"

    with open(index_path, "w", encoding="utf-8") as f:
        f.write(html_content)

    print(f"[OK] Generated: {index_path}")
    print(f"[OK] Total apps: {len(apps)}")

    if not args.build:
        print(f"\n[TIP] Open {index_path} in a browser to preview.")
        print("[TIP] Use --build flag to copy dist files for deployment.")


if __name__ == "__main__":
    main()
