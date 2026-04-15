#!/usr/bin/env python3
"""
Skript to Lumen Converter (Best Effort)
By: alm7airbi
Date: April 2026

This is a simple script to help you port your existing .sk files over to Lumen's .luma format.
It won't catch everything perfectly since Lumen is compiled and typed differently, but it handles 
the boring boilerplate stuff like command definitions, events, messages, and basic variable setting.

Usage:
  python3 skript_converter.py my_script.sk
"""

import re
import sys
import os

def convert_line(line, indent):
    stripped = line.strip()
    
    # Empty lines and comments
    if not stripped: return ""
    if stripped.startswith('#'): return line
        
    # Commands
    if stripped.startswith('command /'):
        cmd_name = stripped[9:].split(':')[0].strip()
        return f"{indent}command {cmd_name}:"
        
    # Skip triggers (Lumen doesn't use them)
    if stripped == 'trigger:':
        return f"{indent}# TODO: Lumen doesn't use 'trigger:'. Check indentation below."
        
    # Events
    if stripped.startswith('on '):
        event_name = stripped[3:].split(':')[0].strip()
        
        # Map common Skript events to Lumen events
        event_map = {
            'join': 'join',
            'quit': 'quit',
            'death of player': 'player_death',
            'break': 'block_break',
            'place': 'block_place',
            'right click': 'interact',
            'left click': 'interact'
        }
        
        mapped = event_map.get(event_name, event_name)
        return f"{indent}on {mapped}:"
        
    # Messaging
    if stripped.startswith('send ') or stripped.startswith('message '):
        # send "Hello" to player -> message player "Hello"
        match = re.search(r'(send|message)\s+"([^"]+)"\s+to\s+(.+)', stripped)
        if match:
            msg = match.group(2).replace('%player%', '{player_name}')
            target = match.group(3)
            return f"{indent}message {target} \"{msg}\""
        
        # broadcast "Hello" -> broadcast "Hello"
        match_bc = re.search(r'(broadcast|send)\s+"([^"]+)"', stripped)
        if match_bc:
            msg = match_bc.group(2).replace('%player%', '{player_name}')
            return f"{indent}broadcast \"{msg}\""
            
    # Variables (set {_var} to 5 -> set var to 5)
    if stripped.startswith('set '):
        match = re.search(r'set\s+\{_?([^}]+)\}\s+to\s+(.+)', stripped)
        if match:
            var_name = match.group(1).replace('::', '_').replace('%', '')
            val = match.group(2)
            return f"{indent}set {var_name} to {val}"
            
    # Math (add 5 to {_var} -> add 5 to var)
    if stripped.startswith('add '):
        match = re.search(r'add\s+(.+)\s+to\s+\{_?([^}]+)\}', stripped)
        if match:
            val = match.group(1)
            var_name = match.group(2).replace('::', '_').replace('%', '')
            return f"{indent}add {val} to {var_name}"
            
    # Items (give 1 diamond to player -> give player diamond 1)
    if stripped.startswith('give '):
        match = re.search(r'give\s+([0-9]+)\s+([a-zA-Z_]+)\s+to\s+(.+)', stripped)
        if match:
            amount = match.group(1)
            item = match.group(2)
            target = match.group(3)
            return f"{indent}give {target} {item} {amount}"
            
    # Fallback for anything else
    return f"{indent}# MANUAL CONVERT: {stripped}"

def convert_file(filepath):
    with open(filepath, 'r') as f:
        lines = f.readlines()
        
    output = []
    for line in lines:
        stripped = line.strip()
        indent = line[:len(line) - len(stripped)]
        output.append(convert_line(line, indent))
        
    return '\n'.join(output)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 skript_converter.py <file.sk>")
        sys.exit(1)
        
    input_file = sys.argv[1]
    if not os.path.exists(input_file):
        print(f"Error: {input_file} not found.")
        sys.exit(1)
        
    print(f"Converting {input_file} to Lumen format...")
    lumen_code = convert_file(input_file)
    
    output_file = input_file.replace('.sk', '.luma')
    with open(output_file, 'w') as f:
        f.write(lumen_code)
        
    print(f"Done! Saved as {output_file}.")
    print("Make sure to review the file and fix any '# MANUAL CONVERT' comments.")
