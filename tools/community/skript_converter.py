#!/usr/bin/env python3
"""
Skript to Lumen Converter (Best Effort)
By: alm7airbi
Date: April 2026

Helps port .sk files to Lumen's .luma format. Handles command definitions, events,
messages, variables, and basic control flow. Complex addon syntax needs manual porting.

Usage:
  python3 skript_converter.py my_script.sk
"""

import re
import sys
import os

# Valid Lumen events (from DefaultEvents.java source)
LUMEN_EVENTS = {
    'block_break', 'block_place', 'entity_damage', 'entity_damage_by_entity',
    'entity_death', 'entity_interact', 'entity_spawn', 'fish', 'interact',
    'inventory_click', 'inventory_close', 'inventory_drag', 'inventory_open',
    'join', 'move', 'player_death', 'projectile_hit', 'projectile_launch',
    'quit', 'respawn', 'teleport', 'toggle_flight', 'toggle_sneak', 'toggle_sprint'
}

# Map Skript event names -> Lumen event names
EVENT_MAP = {
    'join': 'join',
    'quit': 'quit',
    'death of player': 'player_death',
    'player death': 'player_death',
    'death': 'player_death',
    'break': 'block_break',
    'block break': 'block_break',
    'mine': 'block_break',
    'place': 'block_place',
    'block place': 'block_place',
    'right click': 'interact',
    'left click': 'interact',
    'click': 'interact',
    'damage of player': 'entity_damage_by_entity',
    'damage': 'entity_damage_by_entity',
    'shoot': 'projectile_launch',
    'projectile hit': 'projectile_hit',
    'respawn': 'respawn',
    'move': 'move',
    'teleport': 'teleport',
    'toggle flight': 'toggle_flight',
    'toggle sneak': 'toggle_sneak',
    'toggle sprint': 'toggle_sprint',
    'fish': 'fish',
    'inventory click': 'inventory_click',
    'inventory close': 'inventory_close',
    'inventory open': 'inventory_open',
    'entity death': 'entity_death',
    'entity spawn': 'entity_spawn',
}

# No-equivalent events
NO_EQUIVALENT = {'script load', 'load', 'enable', 'disable', 'script unload'}


class SkriptConverter:
    def __init__(self):
        self.options = {}
        self.in_options = False
        self.warnings = []

    def warn(self, line_num, msg):
        self.warnings.append(f"  Line {line_num}: {msg}")

    def fix_options_refs(self, text):
        """Replace {@option} with {option}"""
        return re.sub(r'\{@(\w+)\}', r'{\1}', text)

    def fix_var_refs(self, text):
        """Convert Skript {var} references to Lumen style"""
        def replace_var(m):
            inner = m.group(1)
            # Local var
            if inner.startswith('_'):
                inner = inner[1:]
            # Clean up :: separators
            inner = inner.replace('::', '_')
            # Replace %expr% inside var names
            inner = re.sub(r'%([^%]+)%', r'_\1_', inner)
            inner = inner.replace("uuid of player", "player_name")
            inner = inner.replace("player's uuid", "player_name")
            inner = inner.replace("uuid of ", "")
            inner = inner.replace("'s uuid", "")
            # Clean double underscores
            inner = re.sub(r'_+', '_', inner).strip('_')
            return inner
        return re.sub(r'\{([^}]+)\}', lambda m: replace_var(m), text)

    def fix_string_vars(self, text):
        """Replace %var% in strings with {var}"""
        text = text.replace('%player%', '{player_name}')
        text = text.replace("%player's name%", '{player_name}')
        # %{var}% -> {var_cleaned}
        text = re.sub(r'%\{([^}]+)\}%', lambda m: '{' + self.fix_var_refs('{' + m.group(1) + '}') + '}', text)
        # remaining %expr%
        text = re.sub(r'%([^%]+)%', r'{\1}', text)
        return text

    def convert_line(self, line, line_num):
        stripped = line.strip()
        indent = line[:len(line) - len(line.lstrip())]

        # Empty lines and comments
        if not stripped:
            return ""
        if stripped.startswith('#'):
            return line.rstrip()

        # Options block -> config block
        if stripped == 'options:':
            self.in_options = True
            return f"{indent}config:"

        if self.in_options:
            if not line[0].isspace() and stripped != '':
                self.in_options = False
            else:
                if ':' in stripped and not stripped.startswith('#'):
                    key, val = stripped.split(':', 1)
                    key = key.strip()
                    val = val.strip()
                    self.options[key] = val
                    # Quote string values that aren't already quoted
                    if val and not val[0].isdigit() and val not in ('true', 'false') and not val.startswith('"'):
                        val = f'"{val}"'
                    return f"{indent}{key}: {val}"
                return line.rstrip()

        # Apply option refs everywhere
        working = self.fix_options_refs(stripped)

        # Skip trigger: lines
        if working == 'trigger:':
            return None

        # Commands: command /name ... : -> command name:
        cmd_match = re.match(r'command\s+/?(\S+?)(?:\s+.*)?:', working)
        if cmd_match and not working.startswith('execute'):
            cmd_name = cmd_match.group(1).rstrip(':')
            # Strip angle bracket args from name
            cmd_name = re.sub(r'\s*[<\[].*', '', cmd_name)
            return f"{indent}command {cmd_name}:"

        # Permission
        perm_match = re.match(r'permission:\s*(.+)', working)
        if perm_match:
            perm = perm_match.group(1).strip()
            if not perm.startswith('"'):
                perm = f'"{perm}"'
            return f"{indent}permission {perm}"

        # Permission message — no Lumen equivalent
        if working.startswith('permission message:'):
            return f"{indent}# Lumen doesn't support permission messages: {working}"

        # Events
        event_match = re.match(r'on\s+(.+):', working)
        if event_match:
            event_raw = event_match.group(1).strip().lower()

            if event_raw in NO_EQUIVALENT:
                self.warn(line_num, f"'on {event_raw}:' has no Lumen equivalent. Use top-level code or config: block.")
                return f"{indent}# NO LUMEN EQUIVALENT — on {event_raw}: (use top-level code instead)"

            # Direct map
            if event_raw in EVENT_MAP:
                return f"{indent}on {EVENT_MAP[event_raw]}:"

            # Partial match
            for sk_ev, lumen_ev in EVENT_MAP.items():
                if sk_ev in event_raw:
                    self.warn(line_num, f"Mapped 'on {event_raw}:' -> 'on {lumen_ev}:' (partial match, verify)")
                    return f"{indent}on {lumen_ev}:"

            # Already valid?
            clean = event_raw.replace(' ', '_')
            if clean in LUMEN_EVENTS:
                return f"{indent}on {clean}:"

            self.warn(line_num, f"Unknown event: on {event_raw}:")
            return f"{indent}# UNKNOWN EVENT — on {event_raw}:"

        # Send title with subtitle
        title_match = re.match(r'send\s+title\s+"([^"]+)"\s+with\s+subtitle\s+"([^"]+)"\s+to\s+(\S+).*', working)
        if title_match:
            title = self.fix_string_vars(title_match.group(1))
            subtitle = self.fix_string_vars(title_match.group(2))
            target = title_match.group(3)
            return f'{indent}send title "{title}" with subtitle "{subtitle}" to {target}'

        # Send "text" to target
        send_match = re.match(r'send\s+"([^"]+)"\s+to\s+(\S+)', working)
        if send_match:
            msg = self.fix_string_vars(send_match.group(1))
            target = send_match.group(2)
            return f'{indent}message {target} "{msg}"'

        # Send "text" (implicit player)
        send_impl = re.match(r'send\s+"([^"]+)"$', working)
        if send_impl:
            msg = self.fix_string_vars(send_impl.group(1))
            return f'{indent}message player "{msg}"'

        # Broadcast
        bc_match = re.match(r'broadcast\s+"([^"]+)"', working)
        if bc_match:
            msg = self.fix_string_vars(bc_match.group(1))
            return f'{indent}broadcast "{msg}"'

        # Wait/delay -> block syntax with colon
        wait_match = re.match(r'wait\s+(\d+)\s+(second|tick|minute|hour|day)s?', working)
        if wait_match:
            amount = wait_match.group(1)
            unit = wait_match.group(2)
            self.warn(line_num, f"'wait' is a BLOCK in Lumen — code after it must be indented inside the wait block.")
            return f"{indent}wait {amount} {unit}s:"

        # Set variable
        set_match = re.match(r'set\s+(\{[^}]+\})\s+to\s+(.+)', working)
        if set_match:
            var = self.fix_var_refs(set_match.group(1))
            val = self.fix_options_refs(set_match.group(2))
            val = self.fix_string_vars(val)
            val = self.fix_var_refs(val)
            return f"{indent}set {var} to {val}"

        # Delete/clear variable
        del_match = re.match(r'(delete|clear)\s+\{([^}]+)\}', working)
        if del_match:
            var = self.fix_var_refs('{' + del_match.group(2) + '}')
            return f"{indent}set {var} to 0"

        # Add to variable
        add_match = re.match(r'add\s+(.+)\s+to\s+(\{[^}]+\})', working)
        if add_match:
            val = add_match.group(1)
            var = self.fix_var_refs(add_match.group(2))
            return f"{indent}add {val} to {var}"

        # Add to balance (Skript economy)
        bal_match = re.match(r"add\s+(\d+)\s+to\s+(\S+?)(?:'s)?\s+balance", working)
        if bal_match:
            amount = bal_match.group(1)
            target = bal_match.group(2)
            return f'{indent}execute console command "eco give {target} {amount}"'

        # Subtract from variable
        sub_match = re.match(r'subtract\s+(.+)\s+from\s+(\{[^}]+\})', working)
        if sub_match:
            val = sub_match.group(1)
            var = self.fix_var_refs(sub_match.group(2))
            return f"{indent}subtract {val} from {var}"

        # Give items: give N item to target -> give target item N
        give_match = re.match(r'give\s+(\d+)\s+(\S+)\s+to\s+(\S+)', working)
        if give_match:
            amount = give_match.group(1)
            item = give_match.group(2).replace(' ', '_').lower()
            target = give_match.group(3)
            return f"{indent}give {target} {item} {amount}"

        # Execute console command
        exec_match = re.match(r'execute\s+console\s+command\s+"([^"]+)"', working)
        if exec_match:
            cmd = self.fix_string_vars(exec_match.group(1))
            return f'{indent}execute console command "{cmd}"'

        # Cancel event
        if working == 'cancel event':
            return f"{indent}cancel event"

        # Stop
        if working == 'stop':
            return f"{indent}stop"

        # Play sound
        if working.startswith('play sound'):
            return f"{indent}{self.fix_string_vars(working)}"

        # If/else if conditions
        if working.startswith('if ') or working.startswith('else if '):
            result = self.fix_var_refs(working)
            result = self.fix_string_vars(result)
            return f"{indent}{result}"

        # Else
        if working == 'else:':
            return f"{indent}else:"

        # Loops — flag for manual conversion
        if working.startswith('loop '):
            self.warn(line_num, "Loops need manual conversion. Lumen uses 'for item in collection:'")
            return f"{indent}# MANUAL CONVERT (Lumen loops: 'for item in list:'): {working}"

        # Loop vars
        if 'loop-value' in working or 'loop-index' in working:
            return f"{indent}# MANUAL CONVERT (loop vars): {working}"

        # Skript-specific expressions that need manual work
        manual_keywords = ['parsed as', 'subtext of', 'length of', 'difference between',
                          'is in the future', 'is in the past', 'now']
        if any(kw in working.lower() for kw in manual_keywords):
            self.warn(line_num, f"Skript time/string expression needs manual conversion")
            return f"{indent}# MANUAL CONVERT (Skript expression): {working}"

        # Inline conditions without if (e.g., "attacker is a player")
        if re.match(r'\S+\s+is\s+(a\s+)?player', working):
            self.warn(line_num, "Inline condition — wrap in 'if' block in Lumen")
            return f"{indent}# MANUAL CONVERT (inline condition, use 'if' in Lumen): {working}"

        # Fallback — clean up vars and pass through
        result = self.fix_var_refs(working)
        result = self.fix_string_vars(result)
        result = self.fix_options_refs(result)
        return f"{indent}{result}"

    def convert_file(self, filepath):
        self.options = {}
        self.in_options = False
        self.warnings = []

        with open(filepath, 'r') as f:
            lines = f.readlines()

        output = []
        for i, line in enumerate(lines, 1):
            result = self.convert_line(line, i)
            if result is not None:
                output.append(result)

        return '\n'.join(output)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 skript_converter.py <file.sk>")
        sys.exit(1)

    input_file = sys.argv[1]
    if not os.path.exists(input_file):
        print(f"Error: {input_file} not found.")
        sys.exit(1)

    converter = SkriptConverter()
    print(f"Converting {input_file}...")
    lumen_code = converter.convert_file(input_file)

    output_file = input_file.replace('.sk', '.luma')
    with open(output_file, 'w') as f:
        f.write(lumen_code)

    print(f"Saved as {output_file}")
    print()

    if converter.warnings:
        print(f"Warnings ({len(converter.warnings)}):")
        for w in converter.warnings:
            print(w)
        print()

    print("Reminders:")
    print("  - Add 'global stored' variable declarations at the top of the file")
    print("  - 'wait X seconds:' is a BLOCK — indent the body underneath it")
    print("  - No 'on load:' event — use top-level code or config: block")
    print("  - Event vars: victim -> entity, attacker -> damagerPlayer, shooter -> shooter")
    print("  - on death: -> on player_death: (vars: player, killer)")
    print("  - on shoot: -> on projectile_launch: (vars: entity, shooter)")
    print("  - Loops: 'for item in collection:' instead of 'loop X:'")
    print("  - Lines marked '# MANUAL CONVERT' need your attention")
