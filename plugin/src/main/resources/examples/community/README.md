# Lumen Script Library & Tools

A collection of ready-to-use `.luma` scripts for common Minecraft server tasks, plus a migration guide and converter tool for Skript users.

## Scripts

| Script | Description |
|--------|-------------|
| `welcome.luma` | First-join detection, welcome kit, join/quit messages |
| `homes.luma` | Per-player home system with set/delete/list/teleport |
| `warps.luma` | Server-wide warp points with admin management |
| `kits.luma` | Configurable kits with per-player cooldowns |
| `staffchat.luma` | Toggleable staff chat with permission checks |
| `announcements.luma` | Rotating auto-broadcast system with admin commands |
| `combattag.luma` | Combat logging prevention with PvP tagging |
| `bounties.luma` | Player bounty system with kill rewards |
| `killstats.luma` | Kill/death tracking, killstreaks, and stat lookup |
| `chatformat.luma` | Permission-based chat formatting with rank prefixes |
| `voterewards.luma` | Vote tracking with streak bonuses and milestone rewards |
| `spawnprotection.luma` | PvP and build protection near spawn |

## Tools

| File | Description |
|------|-------------|
| `skript_converter.py` | Best-effort `.sk` to `.luma` converter (Python 3) |
| `migration_guide.md` | Side-by-side Skript vs Lumen syntax reference |
| `bug_report.md` | Bugs and suggestions found during testing |

## How to Use

1. Drop any `.luma` file into your server's `plugins/Lumen/scripts/` folder
2. That's it — Lumen automatically detects new files and compiles them on the fly

## Notes

These scripts were written for Lumen v1.1.0-BETA. Some features (like `wait` in events or certain expressions) may change as Lumen develops. Scripts that need functionality beyond what Lumen currently offers use `java:` blocks as a workaround.

If you find any issues or want to improve a script, PRs are welcome.
