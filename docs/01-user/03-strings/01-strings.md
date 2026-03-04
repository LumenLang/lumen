---
description: "Text manipulation including placeholders, case conversion, comparisons, and more."
---

# Strings

Strings are pieces of text. In Lumen, strings are enclosed in double quotes and support placeholders for embedding dynamic values.

## Creating Strings

```luma
var greeting = "Hello, world!"
var name = "Steve"
```

## Placeholders

Embed variable values inside strings using curly braces `{}`:

```luma
command greet:
    var name = "World"
    message player "&aHello, {name}!"
```

Placeholders work with any variable, including built-in ones:

```luma
on join:
    broadcast "&e{player_name} has joined the server!"
```

## String Length

Get the character count with `length of`:

```luma
var text = "Hello"
var len = length of text
message player "&7Length: {len}"
```

## Case Conversion

Convert to uppercase or lowercase:

```luma
var text = "Hello World"
var upper = text to uppercase
var lower = text to lowercase
```

`upper` is `"HELLO WORLD"` and `lower` is `"hello world"`.

## Joining Arguments

When a command receives multiple words as arguments, join them into a single string:

```luma
command shout:
    var text = args joined with " "
    var upper = text to uppercase
    broadcast "&c&l{player_name}: &f{upper}"
```

If a player types `/shout Hello there friend`, `text` becomes `"Hello there friend"`.

## String Comparison

Use `is` and `is not` to compare strings:

```luma
var action = get args at index 0

if action is "add":
    message player "&aAdding!"

if tag is not "none":
    broadcast "&7{player_name} has a tag!"
```

String comparison with `is` is case-insensitive by default. For an exact (case-sensitive) comparison, use `is exactly`:

```luma
if name is exactly "Steve":
    message player "&aExact match!"
```

## Contains, Starts With, Ends With

Check whether a string contains, starts with, or ends with another string:

```luma
if text contains "hello":
    message player "&aFound it!"

if text starts with "http":
    message player "&7That looks like a URL."

if text ends with "!":
    message player "&eExciting!"
```

## Checking for Empty Strings

```luma
if text is empty:
    message player "&7No text provided."
```

## Trimming

Remove leading and trailing whitespace:

```luma
var cleaned = text trimmed
```

## Substring

Extract a portion of a string:

```luma
var part = substring of text from 0 to 5
```

## Replace

Replace occurrences of one string with another:

```luma
var result = "hello world" replaced "world" with "everyone" # Result: "hello everyone"
```
