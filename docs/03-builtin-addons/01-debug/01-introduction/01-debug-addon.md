---
description: "Live script inspection, expression overrides, condition tracing, and snippet execution for Lumen."
---

# Lumen Debug Addon

The debug addon is built into Lumen and disabled by default. When enabled, it opens a WebSocket port and lets connected
clients inspect running scripts, override expressions and conditions at compile time, capture variable snapshots, and
execute code snippets live on the server.

It is designed for AI-assisted development via the Lumen MCP integration, but any client that speaks the WebSocket
protocol can use it.

## Configuration

All settings live under `debug.service` in `config.yml`.

```yaml
debug:
  service:
    enabled: false
    bind-host: "127.0.0.1"
    port: 7400
    session-timeout-minutes: 300

    same-os:
      allow-same-os-access: true
      require-pairing: false

    remote:
      allow-remote-access: false
      require-pairing: true
```

`enabled` is the master switch. When false, no port is bound and the service never starts. Set it to true and restart to
activate.

`bind-host` controls which network interface the port binds to. The default `127.0.0.1` means only the same machine can
reach the port. Change to `0.0.0.0` only if you also enable `allow-remote-access` and have a firewall in place.

`port` is the WebSocket port clients connect to. Default is 7400.

`session-timeout-minutes` controls how long a session token stays valid. After expiry the client re-handshakes;
previously paired clients skip the pairing prompt and get a fresh token automatically.

The `same-os` block controls connections from the same machine (loopback address). With `require-pairing: false`, a
local client is trusted on first connect. This is the typical development setup.

The `remote` block controls non-loopback connections. Remote access is off by default.

## Security

The debug service is a development tool. Same-OS access with auto-trust is fine for local development: only someone
already on the machine can connect.

Remote access is a different matter. The use case is developing against a test server you do not have physical access
to. Enable remote access only on a dedicated test server, never on anything carrying real player data or a live economy.

A paired client gets far more than a server panel or admin commands. They can execute arbitrary code in the JVM, read
any memory the server process can touch, and rewrite any running script. Only pair with someone who already has full,
trusted access to the server by other means, not because you are granting it to them through this. A pairing code is not
a permission level you can revoke cleanly once someone knows what they are doing.

## Pairing

When pairing is required, the server logs a banner with a six-digit code when a client connects. Approve or manage
connections:

```
/lumendebug pair <code>
/lumendebug list
/lumendebug pending
/lumendebug revoke <clientId>
```

Approved clients are saved to `debug-trust.json` in the plugin data folder and auto-approved on future connects.

## What it supports

### Variable snapshots

Set snapshot points on specific script lines. When code execution passes those lines, the variable state at that moment
is buffered. Poll the server afterward to retrieve snapshots, including all in-scope variables with their values and
types.

### Expression overrides

Every `set x to <expr>` in a script becomes a tracked expression. Override it before the next recompile with either a
literal value (`5`, `"hello"`, `true`) or a full Lumen expression replacing the RHS. Overrides persist across recompiles
until removed.

### Condition overrides

Every `if <condition>:` block is tracked. Pin the outcome to `true` or `false`, skip an `else` branch with `skip`, or
replace the condition with a different Lumen condition.

### Snippet execution

Send a Lumen or Java snippet to be compiled and run immediately on the main thread. Output and errors are captured and
returned.

Setting `useVars: true` injects variables from the most recent snapshot into the snippet scope so you can reference them
by name.

### Condition tracing

Every `if`/`else if`/`else` evaluated during instrumented execution is recorded, showing which branches passed or failed
and at which line.
