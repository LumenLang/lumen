/**
 * Composable terminal UI toolkit for styled multi-line output, live progress displays, and
 * rich diagnostic rendering.
 *
 * <p>This module exists because plain {@code System.out.println} with manual ANSI escapes does
 * not scale. Column alignment breaks the moment styled and unstyled text mix, multi-line
 * layouts become tedious string concatenation, and any progress UI that updates in place needs
 * consistent cursor management nobody wants to write twice. The toolkit solves all three by
 * treating every visible piece of console output as an element that knows its own preferred
 * size and renders into a fixed box assigned by its parent. Parents compose children without
 * managing ANSI escapes by hand; rendering is fully separated from styling.
 *
 * <p>Platform-agnostic; no Bukkit, log4j, or Lumen-specific dependencies.
 */
package dev.lumenlang.console;
