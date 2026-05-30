---
name: mcp2cli
description: Turn any MCP server, OpenAPI spec, or GraphQL endpoint into a CLI. Use this skill when the user wants to interact with an MCP server, OpenAPI/REST API, or GraphQL API via command line, discover available tools/endpoints, call API operations, or generate a new skill from an API. Triggers include "mcp2cli", "call this MCP server", "use this API", "list tools from", "create a skill for this API", "graphql", or any task involving MCP tool invocation, OpenAPI endpoint calls, or GraphQL queries without writing code.
---

# mcp2cli

Turn any MCP server, OpenAPI spec, or GraphQL endpoint into a CLI at runtime. No codegen.

## Install

```bash
# Run directly (no install needed)
uvx mcp2cli --help

# Or install
pip install mcp2cli
```

## Core Workflow

1. **Connect** to a source (MCP server, OpenAPI spec, or GraphQL endpoint)
2. **Discover** available commands with `--list` (or filter with `--search`)
3. **Inspect** a specific command with `<command> --help`
4. **Execute** the command with flags

```bash
# MCP over HTTP
uvx mcp2cli --mcp https://mcp.example.com/sse --list
uvx mcp2cli --mcp https://mcp.example.com/sse create-task --help
uvx mcp2cli --mcp https://mcp.example.com/sse create-task --title "Fix bug"

# MCP over stdio
uvx mcp2cli --mcp-stdio "npx /server-filesystem /tmp" --list
uvx mcp2cli --mcp-stdio "npx u/modelcontextprotocol/server-filesystem /tmp" read-file --path /tmp/hello.txt

# OpenAPI spec (remote or local, JSON or YAML)
uvx mcp2cli --spec https://petstore3.swagger.io/api/v3/openapi.json --list
uvx mcp2cli --spec ./openapi.json --base-url https://api.example.com list-pets --status available

# GraphQL endpoint
uvx mcp2cli --graphql https://api.example.com/graphql --list
uvx mcp2cli --graphql https://api.example.com/graphql users --limit 10
uvx mcp2cli --graphql https://api.example.com/graphql create-user --name "Alice"
```

## CLI Reference

```
mcp2cli [global options] <subcommand> [command options]

Source (mutually exclusive, one required):
  --spec URL|FILE       OpenAPI spec (JSON or YAML, local or remote)
  --mcp URL             MCP server URL (HTTP/SSE)
  --mcp-stdio CMD       MCP server command (stdio transport)
  --graphql URL         GraphQL endpoint URL

Options:
  --auth-header K:V       HTTP header (repeatable, value supports env:/file: prefixes)
  --base-url URL          Override base URL from spec
  --transport TYPE        MCP HTTP transport: auto|sse|streamable (default: auto)
  --env KEY=VALUE         Env var for stdio server process (repeatable)
  --oauth                 Enable OAuth (authorization code + PKCE flow)
  --oauth-client-id ID    OAuth client ID (supports env:/file: prefixes)
  --oauth-client-secret S OAuth client secret (supports env:/file: prefixes)
  --oauth-scope SCOPE     OAuth scope(s) to request
  --cache-key KEY         Custom cache key
  --cache-ttl SECONDS     Cache TTL (default: 3600)
  --refresh               Bypass cache
  --list                  List available subcommands
  --search PATTERN        Search tools by name or description (implies --list)
  --fields FIELDS         Override GraphQL selection set (e.g. "id name email")
  --pretty                Pretty-print JSON output
  --raw                   Print raw response body
  --toon                  Encode output as TOON (token-efficient for LLMs)
  --head N                Limit output to first N records (arrays)
  --version               Show version
```

Subcommands and flags are generated dynamically from the source.

## Patterns

### Authentication

**Always use `env:` or `file:` prefixes for secrets** — never pass credentials as literal values in CLI flags.

```bash
# Secret from environment variable
uvx mcp2cli --spec ./spec.json --auth-header "Authorization:env:API_TOKEN" list-items
```

### Tool search

```bash
uvx mcp2cli --mcp https://mcp.example.com/sse --search "task"
```

## Generating a Skill from an API

When the user asks to create a skill from an MCP server, OpenAPI spec, or GraphQL endpoint, follow this workflow:

1. **Discover** all available commands:
   ```bash
   uvx mcp2cli --mcp https://target.example.com/sse --list
   ```

2. **Inspect** each command to understand parameters:
   ```bash
   uvx mcp2cli --mcp https://target.example.com/sse <command> --help
   ```

3. **Test** key commands and probe for edge cases.

4. **Create a SKILL.MD** in `./.agents/skills/myapi_command/` that teaches another AI agent how to use this specific command. **Each command should have its own separate skill.** Note: The SKILL.md must go beyond `--help` output — focus on knowledge that can only be learned through testing and reading documentation.

   **Frontmatter:**
   ```yaml
   ---
   name: myapi_command
   description: Use the myapi service to perform command
   ---
   ```

   **Core Workflow** (use direct uvx mcp2cli calls):
   ```bash
   # Get help for the command
   uvx mcp2cli --mcp https://target.example.com/sse command --help
   # Run the command
   uvx mcp2cli --mcp https://target.example.com/sse command --param value --pretty
   ```

   **Before Querying** checklist — include a decision framework:
   - What dataset/resource am I targeting?
   - Do I need pagination (`--offset`, `--limit`)?
   - Are there fields that produce large output I should exclude or truncate (`--head`)?

   **Anti-Patterns & Gotchas** — document every surprise found during testing.

   **Output Processing** — use `--pretty` for readable JSON, `--head` to limit results, or pipe to `jq`.

   **Knowledge Delta Principle:** Do not duplicate parameter listings from `--help`. Instead, document which parameters actually matter for common tasks, default behaviors that are surprising, combinations that don't work, and rate limits or response size limits.

Each skill focuses on a single command for modularity and uses direct `mcp2cli` calls to interact with the API.