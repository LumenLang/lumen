---
description: "Messages a debug client sends to the Lumen debug server."
---

# Client to Server Messages

All messages are JSON objects sent over the WebSocket connection. Every message must include a `type` field.

Before any other message is accepted, the client must authenticate. Sending any other message type without an active
session returns `auth_failed`.

---

## Authentication

### hello

Sent immediately after connecting. `clientId` must be a stable identifier that persists across reconnects. Store a UUID
on disk so reconnects are recognized as the same client. The server uses it to look up existing trust grants.

```json
{
  "type": "hello",
  "clientId": "stable-uuid",
  "clientName": "My Tool",
  "version": "1.0"
}
```

---

## Script instrumentation

### enableDebug

Enables instrumentation for a script and recompiles it. Responds with `expressions`, `conditions`, and `enabled` events.

`source` is optional. When omitted, the server loads the script from its own scripts directory by name. `lines` sets
initial snapshot points.

```json
{
  "type": "enableDebug",
  "script": "myscript.luma",
  "lines": [
    10,
    25
  ]
}
```

### disableDebug

Disables instrumentation and recompiles the script back to its original form.

```json
{
  "type": "disableDebug",
  "script": "myscript.luma"
}
```

---

## Expressions

### getExpressions

Request the current expression list for a script.

```json
{
  "type": "getExpressions",
  "script": "myscript.luma"
}
```

### override

Override a tracked expression. `mode` is either `value` (replace with a literal) or `replace` (substitute a new Lumen
expression for the RHS).

```json
{
  "type": "override",
  "exprId": "myscript.luma:10:x",
  "mode": "value",
  "value": "42"
}
```

```json
{
  "type": "override",
  "exprId": "myscript.luma:10:x",
  "mode": "replace",
  "expression": "x + 10"
}
```

### removeOverride

```json
{
  "type": "removeOverride",
  "exprId": "myscript.luma:10:x"
}
```

### removeAllOverrides

```json
{
  "type": "removeAllOverrides",
  "script": "myscript.luma"
}
```

---

## Conditions

### getConditions

```json
{
  "type": "getConditions",
  "script": "myscript.luma"
}
```

### overrideCondition

Override a tracked condition. `mode` is `true`, `false`, `skip` (else blocks only), or `replace`.

```json
{
  "type": "overrideCondition",
  "condId": "myscript.luma:15:cond",
  "mode": "true"
}
```

For `replace`, include `condition` with the new Lumen condition expression.

```json
{
  "type": "overrideCondition",
  "condId": "myscript.luma:15:cond",
  "mode": "replace",
  "condition": "player has permission \"admin\""
}
```

### removeConditionOverride

```json
{
  "type": "removeConditionOverride",
  "condId": "myscript.luma:15:cond"
}
```

### removeAllConditionOverrides

```json
{
  "type": "removeAllConditionOverrides",
  "script": "myscript.luma"
}
```

---

## Snapshots

### setBreakpoints

Set snapshot points for a script. Requires debug to be enabled for the script first. When execution crosses one of these
lines, the variable state is buffered for the next `poll`.

These are called breakpoints internally, but they do not pause execution. They are snapshot points.

```json
{
  "type": "setBreakpoints",
  "script": "myscript.luma",
  "lines": [
    10,
    25
  ]
}
```

---

## Polling

### poll

Drains all buffered snapshot events since the last poll.

```json
{
  "type": "poll"
}
```

---

## Snippets

### executeSnippet

Compile and run a Lumen or Java snippet immediately on the main thread.

`useVars: true` injects variables from the most recent snapshot into the snippet scope. Set snapshot points and trigger
the code path first, then call this to reference those variables by name.

```json
{
  "type": "executeSnippet",
  "snippet": "broadcast \"hello\"",
  "useVars": false
}
```

---

## Configuration

### configure

`dumpFields: true` enables field-level inspection on object variables in snapshots. Each variable will also include a
`fields` array with the declared field values on the object.

```json
{
  "type": "configure",
  "dumpFields": true
}
```
