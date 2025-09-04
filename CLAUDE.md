# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Akka FSM Visualizer is a web-based tool that converts Akka Finite State Machine (FSM) code written in Scala into Mermaid state diagrams. The tool provides an interactive web interface where developers can input their Akka FSM code and get a visual representation of state transitions.

## Development Commands

```bash
# Compile and generate JavaScript (development mode)
sbt fastOptJS

# Compile optimized JavaScript (production mode) 
sbt fullOptJS

# Start SBT shell for interactive development
sbt

# Clean build artifacts
sbt clean

# Serve the web app locally
cd docs && python3 -m http.server 8000
```

After running `sbt fastOptJS`, open `docs/index.html` in a browser to test changes.

## Architecture

### Technology Stack
- **Scala 2.13.16** with **Scala.js** for browser compilation
- **Scalameta** for parsing and analyzing Scala AST
- HTML/CSS/JavaScript frontend in `docs/`

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