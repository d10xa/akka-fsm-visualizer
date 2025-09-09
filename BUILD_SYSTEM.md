# Build System Architecture

## Template-Based HTML Generation

The build system now uses a template-based approach to ensure both `index.html` and Jekyll files are always correct and synchronized.

### Architecture Overview

```
src/
├── templates/
│   ├── base.html              # Single source of truth HTML template
│   └── jekyll-frontmatter.yaml # Jekyll front matter template
└── web/
    └── style.css              # Shared CSS file
```

### Template System

**`src/templates/base.html`** - Contains the complete HTML structure with placeholders:
- `{{CSS_LINK}}` - Replaced with appropriate CSS link
- `{{JS_SCRIPT}}` - Replaced with appropriate JavaScript script tag

**`src/templates/jekyll-frontmatter.yaml`** - Jekyll front matter configuration

### Build Targets

#### Development Build (`sbt dev`)
- Generates: `dist/dev/index.html`
- CSS Link: `<link rel="stylesheet" href="style.css">`
- JS Script: `<script src="akka-fsm-visualizer-fastopt.js"></script>`
- File Structure:
  ```
  dist/dev/
  ├── index.html
  ├── style.css
  └── akka-fsm-visualizer-fastopt.js
  ```

#### Jekyll Build (`sbt jekyllBuild`)
- Generates: `dist/jekyll/apps/akka-fsm-visualizer.html`
- CSS Link: `<link rel="stylesheet" href="{{ '/apps/assets/css/akka-fsm-visualizer.css' | relative_url }}">`
- JS Script: `<script src="{{ '/apps/assets/js/akka-fsm-visualizer.js' | relative_url }}"></script>`
- File Structure:
  ```
  dist/jekyll/
  └── apps/
      ├── akka-fsm-visualizer.html
      └── assets/
          ├── css/
          │   └── akka-fsm-visualizer.css
          └── js/
              └── akka-fsm-visualizer.js
  ```

### Benefits

✅ **Single Source of Truth** - All HTML comes from one template
✅ **Automatic Synchronization** - Changes to UI are reflected in both builds
✅ **Correct Asset Paths** - Each build gets appropriate paths for its deployment target
✅ **Jekyll Liquid Support** - Jekyll build uses proper Liquid templating syntax
✅ **No Manual Maintenance** - No need to manually sync `index.html` and `jekyll.html`

### Usage

```bash
# Development build
sbt dev

# Jekyll build for deployment
sbt jekyllBuild

# Build both targets
sbt dev jekyllBuild
```

The generated Jekyll files can be copied directly to your Jekyll site and will work with the `/akka-fsm-visualizer/` permalink.