---
description: "Messages the Lumen debug server sends to a connected client."
---

# Server to Client Messages

The server sends messages in response to client requests and also pushes snapshot events via `poll`.

---

## Authentication

### authorized

Sent after a successful `hello` (either auto-trusted or after pairing approval).

```json
{
  "type": "authorized",
  "sessionToken": "...",
  "clientId": "...",
  "expiresAt": 1234567890000,
  "sessionMinutes": 300
}
```

### pairing_required

Sent when the connecting client is not trusted and requires operator approval. The operator must run
`/lumendebug pair <code>` in console. After approval the server sends `authorized`. If the code expires the connection
is closed.

```json
{
  "type": "pairing_required",
  "pairingId": "...",
  "ttlSeconds": 60
}
```

### auth_failed

Sent when authentication is rejected outright (rate limited, remote access disabled, etc.) or when a non-`hello` message
arrives before authentication.

```json
{
  "type": "auth_failed",
  "reason": "..."
}
```

---

## Script instrumentation

### enabled

Sent after `enableDebug` succeeds.

```json
{
  "type": "enabled",
  "script": "myscript.luma",
  "expressionCount": 5,
  "breakpointCount": 2
}
```

---

## Expressions

### expressions

Sent after `enableDebug`, `getExpressions`, or any operation that recompiles the script. Lists all tracked expressions.

```json
{
  "type": "expressions",
  "script": "myscript.luma",
  "list": [
    {
      "id": "myscript.luma:10:x",
      "expression": "set x to 10",
      "type": "int",
      "line": 10,
      "overridden": false
    },
    {
      "id": "myscript.luma:11:y",
      "expression": "set y to x * 2",
      "type": "int",
      "line": 11,
      "overridden": true,
      "overrideValue": "999"
    }
  ]
}
```

---

## Conditions

### conditions

Sent after `enableDebug`, `getConditions`, or any operation that recompiles the script. Lists all tracked conditions.

```json
{
  "type": "conditions",
  "script": "myscript.luma",
  "list": [
    {
      "id": "myscript.luma:15:cond",
      "source": "if x > 5",
      "line": 15,
      "overridden": false
    },
    {
      "id": "myscript.luma:20:cond",
      "source": "if player has permission \"admin\"",
      "line": 20,
      "overridden": true,
      "overrideMode": "true"
    }
  ]
}
```

---

## Polling

### poll

Response to `poll`. Contains all snapshot events buffered since the last poll.

```json
{
  "type": "poll",
  "events": [
    ...
  ]
}
```

Each entry in `events` is one of the event types below.

---

### snapshot event

Emitted when execution crosses a snapshot point (set via `setBreakpoints`). `vars` is an array of all in-scope variables
at that line.

These are called `breakpoint` events in the protocol for historical reasons, but they do not pause execution.

```json
{
    "type": "breakpoint",
    "script": "myscript.luma",
    "line": 10,
    "vars": [
        {
            "name": "x",
            "value": "10",
            "type": "..."
        },
        {
            "name": "player",
            "value": "CraftPlayer{name=user}",
            "type": "...",
            "fields": [
                { "name": "health", "value": "20.0", "type": "..." }
            ]
        }
    ],
    "conditionTrace": [
        {
            "condId": "myscript.luma:8:cond",
            "source": "if x > 5",
            "line": 8,
            "result": true
        }
    ]
}
```

`fields` is only present when `dumpFields` is enabled via `configure`. `conditionTrace` lists every condition evaluated
between the previous snapshot and this one.

---

## Snippets

### snippetResult

Response to `executeSnippet`.

```json
{
  "type": "snippetResult",
  "success": true,
  "stdout": "hello from snippet",
  "stderr": "",
  "generatedSource": "...",
  "conditionTrace": [
    ...
  ]
}
```

`error`, `stdout`, `stderr`, and `generatedSource` are omitted when empty or not applicable. `conditionTrace` follows
the same format as in snapshot events.

---

## Errors

### error

Sent in response to a bad request, a compile failure, or any unexpected server-side error.

```json
{
  "type": "error",
  "message": "..."
}
```
