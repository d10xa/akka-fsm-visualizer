# Akka FSM Visualizer - Playwright Tests

Comprehensive end-to-end tests for the Akka FSM Visualizer web application using Playwright.

## Test Coverage

### Core Functionality (`akka-fsm-visualizer.spec.js`)
- ✅ Page loading and UI elements visibility
- ✅ Example code loading and diagram generation
- ✅ Code input and real-time diagram updates
- ✅ Toggle between diagram and code view
- ✅ Copy mermaid code to clipboard
- ✅ Fullscreen modal functionality
- ✅ File upload handling
- ✅ Recovery function support
- ✅ Error handling for empty/invalid code
- ✅ SVG/PNG export functionality
- ✅ Multiple state objects support
- ✅ Complex nested functions
- ✅ State persistence after reload

### Edge Cases (`edge-cases.spec.js`)
- ✅ Large FSM handling (50+ states)
- ✅ Special characters in state names
- ✅ FSM with no transitions
- ✅ Malformed Scala code graceful handling
- ✅ Rapid code changes stability
- ✅ Circular state references
- ✅ Mixed case state names
- ✅ stopSuccess-only transitions
- ✅ Browser refresh during analysis
- ✅ Very long state/event names

### Performance (`performance.spec.js`)
- ✅ Initial page load time (< 5 seconds)
- ✅ Example code analysis speed (< 3 seconds)
- ✅ Code change response time (< 2 seconds)
- ✅ Large FSM memory efficiency (< 8 seconds)
- ✅ Rapid successive changes handling (< 5 seconds)
- ✅ UI responsiveness during analysis
- ✅ Smooth scrolling with large diagrams

## Setup

1. Install dependencies:
\`\`\`bash
cd playwright-tests
npm install
npx playwright install
\`\`\`

2. Start the local server (in another terminal):
\`\`\`bash
cd ../docs
python3 -m http.server 8000
\`\`\`

## Running Tests

### Run all tests
\`\`\`bash
npm test
\`\`\`

### Run tests with UI mode
\`\`\`bash
npm run test:ui
\`\`\`

### Run tests in headed mode (visible browser)
\`\`\`bash
npm run test:headed
\`\`\`

### Debug tests
\`\`\`bash
npm run test:debug
\`\`\`

### Run specific test file
\`\`\`bash
npx playwright test tests/akka-fsm-visualizer.spec.js
\`\`\`

### Run specific test
\`\`\`bash
npx playwright test --grep "should load the main page"
\`\`\`

## Test Categories

### Functional Tests
- Basic UI functionality
- Diagram generation and updates
- User interactions (buttons, modals, etc.)
- File handling and exports

### Integration Tests
- Scala code parsing
- Mermaid diagram generation
- State management
- Error handling

### Performance Tests
- Load times and response times
- Memory usage with large inputs
- UI responsiveness
- Scrolling performance

### Robustness Tests
- Edge cases and error conditions
- Malformed input handling
- Browser compatibility
- Rapid user interactions

## Browser Support

Tests run on:
- ✅ Chromium (Chrome/Edge)
- ✅ Firefox
- ✅ WebKit (Safari)

## Key Test Features

### Automatic Server Setup
The test configuration automatically starts a local server on port 8000 serving the application from the \`docs/\` directory.

### Cross-Browser Testing
All tests run across multiple browsers to ensure compatibility.

### Visual Testing
Tests verify that SVG diagrams are generated and displayed correctly.

### User Interaction Testing
Comprehensive testing of all UI elements including buttons, modals, file uploads, and keyboard interactions.

### Performance Monitoring
Tests include timing assertions to ensure the application performs within acceptable limits.

### Error Handling Verification
Tests ensure graceful handling of invalid inputs and error conditions.

## Test Data

Tests use various sample FSM codes:
- Simple state machines for basic functionality
- Complex nested function examples
- Recovery state handling
- Large-scale FSMs for performance testing
- Malformed code for error handling

## CI/CD Integration

Tests are configured for continuous integration with:
- Retry logic for flaky tests
- Parallel execution support
- HTML reporting
- Trace collection on failures

## Debugging

When tests fail:
1. Check the HTML report: \`npx playwright show-report\`
2. Use debug mode: \`npm run test:debug\`
3. Run with headed browser: \`npm run test:headed\`
4. Check browser console logs in the test output

## Contributing

When adding new features to the application:
1. Add corresponding test cases
2. Update existing tests if functionality changes
3. Ensure all tests pass before committing
4. Consider performance implications for new features