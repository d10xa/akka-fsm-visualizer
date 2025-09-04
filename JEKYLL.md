# Jekyll Integration

This document explains how to integrate the Akka FSM Visualizer into your Jekyll blog.

## Building for Jekyll

Run the custom Jekyll build task:

```bash
sbt jekyllBuild
```

This will:
1. Compile and optimize the Scala.js code
2. Create a `jekyll/` directory with Jekyll-compatible files
3. Copy assets to proper Jekyll paths:
   - `jekyll/assets/js/akka-fsm-visualizer.js`
   - `jekyll/assets/css/akka-fsm-visualizer.css`
   - `jekyll/akka-fsm-visualizer.html`

## Integration Steps

1. **Copy assets to your Jekyll blog:**
   ```bash
   cp -r jekyll/assets/* your-blog/assets/
   ```

2. **Copy the page file:**
   ```bash
   cp jekyll/akka-fsm-visualizer.html your-blog/_pages/
   # or to your posts directory if you prefer
   ```

3. **Update the layout (if needed):**
   The HTML file uses `layout: default`. Make sure your Jekyll theme has a `default` layout, or change it to match your theme.

## Customization

### Front Matter
The Jekyll template includes the following front matter:
```yaml
---
layout: default
title: "Akka FSM Visualizer"
description: "Convert Akka FSM code to Mermaid state diagrams with live preview"
permalink: /akka-fsm-visualizer/
---
```

You can modify these values in `jekyll-template.html` before building.

### Styling
The CSS is self-contained and shouldn't conflict with your Jekyll theme. If you need to customize styling:
1. Edit `docs/style.css`
2. Run `sbt jekyllBuild` to regenerate

### JavaScript
The JavaScript is optimized and minified. No additional dependencies are needed besides Mermaid.js which is loaded from CDN.

## File Structure

After running `sbt jekyllBuild`, you'll get:
```
jekyll/
├── assets/
│   ├── css/
│   │   └── akka-fsm-visualizer.css
│   └── js/
│       └── akka-fsm-visualizer.js
└── akka-fsm-visualizer.html
```

## Notes

- The tool works entirely client-side, no server-side processing needed
- Mermaid.js is loaded from CDN (jsdelivr)
- The visualizer supports all modern browsers
- No additional Jekyll plugins required