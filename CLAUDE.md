# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Akka FSM Visualizer is a web-based tool that converts Akka Finite State Machine (FSM) code written in Scala into Mermaid state diagrams. The tool provides an interactive web interface where developers can input their Akka FSM code and get a visual representation of state transitions.

## Development Commands

```bash
# Development build (fast compilation + copy static assets)
sbt dev

# Individual tasks
sbt fastOptJS      # Just compile JS to dist/dev/
sbt copyAssetsDev  # Just copy static assets to dist/dev/

# Production build (optimized compilation)
sbt fullOptJS

# Build for Jekyll site deployment
sbt jekyllBuild

# Start SBT shell for interactive development
sbt

# Clean build artifacts
sbt clean

# Serve the web app locally for development
cd dist/dev && python3 -m http.server 8000
```

After running `sbt dev`, the complete development build is available in `dist/dev/`. Open `dist/dev/index.html` in a browser or use the local server.

## Project Structure

```
├── src/
│   ├── main/scala/        # Scala.js source code
│   └── web/              # Static web assets (HTML, CSS)
│       ├── index.html    # Development HTML template
│       ├── style.css     # Styles
│       └── jekyll.html   # Jekyll site template
├── dist/                 # Build outputs (ignored by git)
│   ├── dev/             # Development build
│   ├── prod/            # Production build
│   └── jekyll/          # Jekyll site files
└── docs/                 # Legacy directory (only index.html, style.css)
```

## Architecture

### Technology Stack
- **Scala 2.13.16** with **Scala.js** for browser compilation
- **Scalameta** for parsing and analyzing Scala AST
- Static web assets in `src/web/`, build outputs in `dist/`

### Core Components

**`src/main/scala/AkkaFsmAnalyzer.scala`** - Core parser that uses Scalameta to analyze Scala FSM code, extracts state transitions from `when()` blocks and `goto()` statements, and converts them to Mermaid diagram syntax.

**`src/main/scala/FsmVisualizerApp.scala`** - Main web application entry point that sets up DOM event handlers and integrates with the analyzer.

**`docs/index.html`** - Web interface with dual-panel layout (left: Scala code input, right: Mermaid diagram output).

### Key Parsing Logic

The analyzer looks for:
- `when()` blocks to identify states
- `goto()` statements for state transitions
- Special state handling for recovery, stop, and failed states
- State coloring: recovery states (teal), failed states (red), regular states (blue)

## Build Configuration

- **SBT 1.9.6** with Scala.js plugin 1.19.0
- JavaScript output goes to `/docs` directory for easy web serving
- Source maps enabled for debugging
- Module initializer auto-starts the application

## Testing and Examples

Use the example FSM files in `example/` directory:
- `MyFsm.scala` - Simple FSM example
- `Processing.scala` - Complex FSM with timeouts and recovery

The tool generates Mermaid stateDiagram-v2 syntax that can be visualized in GitHub, GitLab, or Mermaid Live Editor.