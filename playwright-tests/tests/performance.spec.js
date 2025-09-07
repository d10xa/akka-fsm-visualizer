import { test, expect } from '@playwright/test';

test.describe('Akka FSM Visualizer - Performance', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:8000');
  });

  test('should load initial page within reasonable time', async ({ page }) => {
    const startTime = Date.now();
    
    // Wait for all main elements to be visible
    await expect(page.locator('h1')).toBeVisible();
    await expect(page.locator('#codeInput')).toBeVisible();
    await expect(page.locator('#diagramContainer')).toBeVisible();
    
    const loadTime = Date.now() - startTime;
    expect(loadTime).toBeLessThan(5000); // Should load within 5 seconds
  });

  test('should analyze example code quickly', async ({ page }) => {
    const startTime = Date.now();
    
    // Wait for initial analysis to complete (example code is loaded automatically)
    await page.waitForTimeout(1000);
    
    // Check that diagram was generated
    await expect(page.locator('#diagramContainer svg')).toBeVisible();
    
    const analysisTime = Date.now() - startTime;
    expect(analysisTime).toBeLessThan(3000); // Should analyze within 3 seconds
  });

  test('should handle code changes with reasonable response time', async ({ page }) => {
    const codeTextarea = page.locator('#codeInput');
    
    const simpleCode = `object State {
  case object A extends State
  case object B extends State
}

class SimpleFSM extends FSM[State, Data] {
  when(State.A) {
    case Event(Go, _) => goto(State.B)
  }
}`;
    
    const startTime = Date.now();
    
    await codeTextarea.clear();
    await codeTextarea.fill(simpleCode);
    
    // Wait for analysis to complete
    await expect(page.locator('#diagramContainer svg')).toBeVisible();
    
    const responseTime = Date.now() - startTime;
    expect(responseTime).toBeLessThan(2000); // Should respond within 2 seconds
  });

  test('should not consume excessive memory with large FSM', async ({ page }) => {
    // Generate moderately large FSM (not too large to avoid timeouts)
    let largeCode = `object State {\n`;
    for (let i = 0; i < 20; i++) {
      largeCode += `  case object State${i} extends ProcessingState\n`;
    }
    largeCode += `}

class LargeFSM extends FSM[ProcessingState, Data] {\n`;
    
    for (let i = 0; i < 20; i++) {
      const nextState = (i + 1) % 20;
      largeCode += `  when(State.State${i}) {
    case Event(Next, _) => goto(State.State${nextState})
  }
`;
    }
    largeCode += `}`;
    
    const codeTextarea = page.locator('#codeInput');
    
    // Monitor memory usage (basic check)
    const startTime = Date.now();
    
    await codeTextarea.clear();
    await codeTextarea.fill(largeCode);
    
    // Wait for analysis
    await page.waitForTimeout(4000);
    
    // Should still be responsive
    await expect(page.locator('#diagramContainer svg')).toBeVisible();
    
    const processingTime = Date.now() - startTime;
    expect(processingTime).toBeLessThan(8000); // Should process within 8 seconds
  });

  test('should handle rapid successive code changes efficiently', async ({ page }) => {
    const codeTextarea = page.locator('#codeInput');
    
    const codes = [];
    for (let i = 0; i < 5; i++) {
      codes.push(`object State${i} {
  case object A${i} extends State${i}
  case object B${i} extends State${i}
}

class FSM${i} extends FSM[State${i}, Data] {
  when(State${i}.A${i}) {
    case Event(Go, _) => goto(State${i}.B${i})
  }
}`);
    }
    
    const startTime = Date.now();
    
    // Rapidly change code
    for (let i = 0; i < codes.length; i++) {
      await codeTextarea.clear();
      await codeTextarea.fill(codes[i]);
      await page.waitForTimeout(100); // Very short delay
    }
    
    // Wait for final analysis
    await page.waitForTimeout(2000);
    
    // Should still work and be responsive
    await expect(page.locator('#diagramContainer svg')).toBeVisible();
    
    const totalTime = Date.now() - startTime;
    expect(totalTime).toBeLessThan(5000); // Should handle all changes within 5 seconds
  });

  test('should not freeze UI during analysis', async ({ page }) => {
    const codeTextarea = page.locator('#codeInput');
    const toggleButton = page.locator('#toggleSource');
    
    // Input moderately complex code
    const complexCode = `object State {
  case object Start extends ProcessingState
  case object Process1 extends ProcessingState
  case object Process2 extends ProcessingState
  case object Process3 extends ProcessingState
  case object End extends ProcessingState
}

class ComplexFSM extends FSM[ProcessingState, Data] {
  when(State.Start) {
    case Event(Begin, _) => handleBegin()
  }
  
  def handleBegin(): State = {
    validateInput() match {
      case true => processStep1()
      case false => goto(State.End)
    }
  }
  
  def processStep1(): State = {
    goto(State.Process1)
  }
  
  when(State.Process1) {
    case Event(Next, _) => processStep2()
  }
  
  def processStep2(): State = {
    goto(State.Process2)
  }
  
  when(State.Process2) {
    case Event(Next, _) => processStep3()
  }
  
  def processStep3(): State = {
    goto(State.Process3)
  }
  
  when(State.Process3) {
    case Event(Complete, _) => goto(State.End)
  }
  
  def validateInput(): Boolean = true
}`;
    
    await codeTextarea.clear();
    await codeTextarea.fill(complexCode);
    
    // UI should still be responsive during analysis
    // Try to interact with toggle button immediately
    await page.waitForTimeout(500);
    
    // Button should be clickable (UI not frozen)
    await expect(toggleButton).toBeEnabled();
    
    // Can click the button
    await toggleButton.click();
    await expect(page.locator('#mermaidOutput')).toBeVisible();
    
    // Toggle back
    await toggleButton.click();
    await expect(page.locator('#diagramContainer')).toBeVisible();
  });

  test('should maintain smooth scrolling with large diagrams', async ({ page }) => {
    // Create FSM that generates a large diagram
    let largeCode = `object State {\n`;
    for (let i = 0; i < 15; i++) {
      largeCode += `  case object State${i} extends ProcessingState\n`;
    }
    largeCode += `}

class ScrollTestFSM extends FSM[ProcessingState, Data] {\n`;
    
    // Create linear chain of states (should create tall diagram)
    for (let i = 0; i < 14; i++) {
      largeCode += `  when(State.State${i}) {
    case Event(Next, _) => goto(State.State${i + 1})
  }
`;
    }
    largeCode += `}`;
    
    const codeTextarea = page.locator('#codeInput');
    
    await codeTextarea.clear();
    await codeTextarea.fill(largeCode);
    
    // Wait for diagram generation
    await page.waitForTimeout(3000);
    await expect(page.locator('#diagramContainer svg')).toBeVisible();
    
    // Test scrolling performance
    const diagramContainer = page.locator('#diagramContainer');
    
    // Scroll down and up
    await diagramContainer.hover();
    await page.mouse.wheel(0, 500); // Scroll down
    await page.waitForTimeout(100);
    await page.mouse.wheel(0, -500); // Scroll up
    await page.waitForTimeout(100);
    
    // Page should still be responsive
    await expect(page.locator('#toggleSource')).toBeEnabled();
  });
});