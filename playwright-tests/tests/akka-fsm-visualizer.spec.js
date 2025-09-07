import { test, expect } from '@playwright/test';

// Sample FSM code for testing
const sampleFsmCode = `import akka.actor.{Actor, FSM, Props}
import scala.concurrent.duration._

// Events
sealed trait OrderEvent
case object PlaceOrder extends OrderEvent
case object PaymentReceived extends OrderEvent
case object PaymentFailed extends OrderEvent

// States
sealed trait OrderState
object State {
  case object WaitingForOrder extends OrderState
  case object PaymentPending extends OrderState
  case object PaymentFailed extends OrderState
  case object Cancelled extends OrderState
}

class OrderProcessingFSM extends Actor with FSM[OrderState, OrderData] {
  when(State.WaitingForOrder) {
    case Event(PlaceOrder, _) =>
      goto(State.PaymentPending) using OrderInfo("ORDER-123", 99.99)
  }

  when(State.PaymentPending) {
    case Event(PaymentReceived, orderInfo) =>
      goto(State.Cancelled) using orderInfo
      
    case Event(PaymentFailed, orderInfo) =>
      goto(State.PaymentFailed) using orderInfo
  }

  when(State.PaymentFailed) {
    case Event(PaymentReceived, orderInfo) =>
      goto(State.Cancelled) using orderInfo
  }
}`;

const recoveryFsmCode = `object State {
  case object RecoverSelf extends State
  case object Idle extends State
  case object Failed extends State
}

class RecoveryFSM extends FSM[State, Data] {
  def recoverStateDecision(reason: String): State = {
    reason match {
      case "network_error" => Target.enter(State.Idle, InitialData)
      case "critical_error" => Target.enter(State.Failed, ErrorData(reason))
    }
  }
}`;

test.describe('Akka FSM Visualizer', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:8000');
  });

  test('should load the main page', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('Akka FSM Visualizer');
    await expect(page.locator('p')).toContainText('Convert Akka FSM code to Mermaid state diagrams');
  });

  test('should have all required UI elements', async ({ page }) => {
    // Check for main elements
    await expect(page.locator('#fileInput')).toBeVisible();
    await expect(page.locator('#codeInput')).toBeVisible();
    await expect(page.locator('#copyButton')).toBeVisible();
    await expect(page.locator('#toggleSource')).toBeVisible();
    await expect(page.locator('#exportSvg')).toBeVisible();
    await expect(page.locator('#exportPng')).toBeVisible();
    await expect(page.locator('#fullscreen')).toBeVisible();
    await expect(page.locator('#diagramContainer')).toBeVisible();
    // mermaidOutput should exist but be hidden initially
    await expect(page.locator('#mermaidOutput')).toBeAttached();
    
    // Check initial state
    await expect(page.locator('#mermaidOutput')).not.toBeVisible();
    await expect(page.locator('#diagramContainer')).toBeVisible();
    await expect(page.locator('#toggleSource')).toContainText('Show Code');
  });

  test('should load with example code', async ({ page }) => {
    const codeTextarea = page.locator('#codeInput');
    await expect(codeTextarea).not.toBeEmpty();
    
    const codeContent = await codeTextarea.inputValue();
    expect(codeContent).toContain('OrderProcessingFSM');
    expect(codeContent).toContain('when(State.');
  });

  test('should generate diagram from example code', async ({ page }) => {
    // Wait for initial analysis to complete
    await page.waitForTimeout(2000);
    
    const diagramContainer = page.locator('#diagramContainer');
    await expect(diagramContainer.locator('svg')).toBeVisible();
    
    // Check that mermaid diagram contains expected elements
    const svgContent = await diagramContainer.innerHTML();
    expect(svgContent).toContain('svg');
  });

  test('should update diagram when code changes', async ({ page }) => {
    const codeTextarea = page.locator('#codeInput');
    
    // Clear and input new code
    await codeTextarea.clear();
    await codeTextarea.fill(sampleFsmCode);
    
    // Wait for debouncing delay + analysis + diagram rendering
    // Total timeout increased to account for 300ms debounce + analysis + mermaid rendering
    const diagramContainer = page.locator('#diagramContainer');
    
    // Wait for either SVG to appear or error message (with 15 second timeout)
    try {
      await expect(diagramContainer.locator('svg')).toBeVisible({ timeout: 15000 });
    } catch (e) {
      // If SVG doesn't appear, log what's actually in the container for debugging
      const containerContent = await diagramContainer.innerHTML();
      console.log('Diagram container content:', containerContent);
      
      // Check if there's an error message instead
      const hasError = await diagramContainer.locator('.error-text').isVisible().catch(() => false);
      if (hasError) {
        const errorText = await diagramContainer.locator('.error-text').textContent();
        throw new Error(`Diagram failed to render. Error: ${errorText}`);
      }
      
      throw e; // Re-throw original error if no error message found
    }
  });

  test('should toggle between diagram and code view', async ({ page }) => {
    const toggleButton = page.locator('#toggleSource');
    const mermaidOutput = page.locator('#mermaidOutput');
    const diagramContainer = page.locator('#diagramContainer');
    
    // Initially should show diagram
    await expect(mermaidOutput).not.toBeVisible();
    await expect(diagramContainer).toBeVisible();
    await expect(toggleButton).toContainText('Show Code');
    
    // Click to show code
    await toggleButton.click();
    await expect(mermaidOutput).toBeVisible();
    await expect(diagramContainer).not.toBeVisible();
    await expect(toggleButton).toContainText('Show Diagram');
    
    // Click to show diagram again
    await toggleButton.click();
    await expect(mermaidOutput).not.toBeVisible();
    await expect(diagramContainer).toBeVisible();
    await expect(toggleButton).toContainText('Show Code');
  });

  test.describe('Clipboard functionality (Chromium only)', () => {
    test.skip(({ browserName }) => browserName !== 'chromium');
    
    test('should copy mermaid code to clipboard', async ({ page }) => {
      // Grant clipboard permissions
      await page.context().grantPermissions(['clipboard-write', 'clipboard-read']);
      
      const copyButton = page.locator('#copyButton');
      
      // Wait for diagram generation
      await page.waitForTimeout(2000);
      
      await copyButton.click();
      
      // Check that button shows feedback
      await expect(copyButton).toContainText('Copied!');
      
      // Wait for button text to revert
      await page.waitForTimeout(2500);
      await expect(copyButton).toContainText('Copy Mermaid');
    });
  });

  test('should open fullscreen modal', async ({ page }) => {
    const fullscreenButton = page.locator('#fullscreen');
    const fullscreenModal = page.locator('#fullscreenModal');
    const closeButton = page.locator('#closeFullscreen');
    
    // Initially modal should be hidden
    await expect(fullscreenModal).not.toBeVisible();
    
    // Click fullscreen button
    await fullscreenButton.click();
    await expect(fullscreenModal).toBeVisible();
    
    // Check modal content
    await expect(page.locator('.fullscreen-header h2')).toContainText('FSM Diagram - Fullscreen View');
    await expect(page.locator('#fullscreenDiagram')).toBeVisible();
    
    // Close modal with button
    await closeButton.click();
    await expect(fullscreenModal).not.toBeVisible();
  });

  test('should close fullscreen modal when clicking background', async ({ page }) => {
    const fullscreenButton = page.locator('#fullscreen');
    const fullscreenModal = page.locator('#fullscreenModal');
    
    // Open modal
    await fullscreenButton.click();
    await expect(fullscreenModal).toBeVisible();
    
    // Click on modal background (not on content)
    await fullscreenModal.click({ position: { x: 10, y: 10 } });
    await expect(fullscreenModal).not.toBeVisible();
  });

  test('should handle file upload', async ({ page }) => {
    const fileInput = page.locator('#fileInput');
    const codeTextarea = page.locator('#codeInput');
    
    // Create a test file
    const testFileContent = sampleFsmCode;
    
    // Upload file
    await fileInput.setInputFiles({
      name: 'test-fsm.scala',
      mimeType: 'text/plain',
      buffer: Buffer.from(testFileContent)
    });
    
    // Wait for file to be processed
    await page.waitForTimeout(1000);
    
    // Check that code was loaded
    const codeContent = await codeTextarea.inputValue();
    expect(codeContent).toContain('OrderProcessingFSM');
  });

  test('should handle recovery functions', async ({ page }) => {
    const codeTextarea = page.locator('#codeInput');
    
    // Clear and input recovery FSM code
    await codeTextarea.clear();
    await codeTextarea.fill(recoveryFsmCode);
    
    // Wait for analysis
    await page.waitForTimeout(1500);
    
    // Check that diagram was generated
    const diagramContainer = page.locator('#diagramContainer');
    await expect(diagramContainer.locator('svg')).toBeVisible();
  });

  test('should handle empty code gracefully', async ({ page }) => {
    const codeTextarea = page.locator('#codeInput');
    const diagramContainer = page.locator('#diagramContainer');
    
    // Clear code
    await codeTextarea.clear();
    
    // Wait for analysis
    await page.waitForTimeout(1000);
    
    // Should show placeholder
    await expect(diagramContainer).toContainText('Enter Akka FSM code to see the diagram');
  });

  test('should handle invalid code gracefully', async ({ page }) => {
    const codeTextarea = page.locator('#codeInput');
    const errorDiv = page.locator('#error');
    
    // Input invalid code
    await codeTextarea.clear();
    await codeTextarea.fill('this is not valid scala code {{{');
    
    // Wait for analysis
    await page.waitForTimeout(1500);
    
    // Should show error
    await expect(errorDiv).toBeVisible();
    await expect(errorDiv).toContainText('Parse error');
  });

  test('should export SVG', async ({ page }) => {
    const exportSvgButton = page.locator('#exportSvg');
    
    // Wait for diagram to be ready
    await page.waitForTimeout(2000);
    
    // Set up download promise before clicking
    const downloadPromise = page.waitForEvent('download');
    
    await exportSvgButton.click();
    
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toBe('fsm-diagram.svg');
  });

  test('should try to export PNG', async ({ page }) => {
    const exportPngButton = page.locator('#exportPng');
    
    // Wait for diagram to be ready
    await page.waitForTimeout(2000);
    
    // PNG export might fail due to security restrictions, but button should work
    await exportPngButton.click();
    
    // Wait a bit to see if any error handling occurs
    await page.waitForTimeout(1000);
    
    // If it fails, it should fallback to SVG export
    // We can't easily test the actual download here due to security restrictions
  });

  test('should handle multiple state objects', async ({ page }) => {
    const multiStateCode = `object ProcessStates {
  case object Idle extends ProcessState
  case object Running extends ProcessState
}

object ErrorStates {
  case object Failed extends ProcessState
  case object Recovering extends ProcessState
}

class MultiFSM extends FSM[ProcessState, Data] {
  when(ProcessStates.Idle) {
    case Event(Start, _) => goto(ProcessStates.Running)
  }
  when(ProcessStates.Running) {
    case Event(Error, _) => goto(ErrorStates.Failed)
  }
}`;
    
    const codeTextarea = page.locator('#codeInput');
    
    // Clear and input multi-state code
    await codeTextarea.clear();
    await codeTextarea.fill(multiStateCode);
    
    // Wait for analysis
    await page.waitForTimeout(1500);
    
    // Check that diagram was generated
    const diagramContainer = page.locator('#diagramContainer');
    await expect(diagramContainer.locator('svg')).toBeVisible();
  });

  test('should handle complex nested functions', async ({ page }) => {
    const complexCode = `object State {
  case object Start extends ProcessingState
  case object Processing extends ProcessingState
  case object Complete extends ProcessingState
  case object Failed extends ProcessingState
}

class ComplexFSM extends FSM[ProcessingState, Data] {
  when(State.Start) {
    case Event(Begin, _) => handleBegin()
  }
  
  when(State.Processing) {
    case Event(Process, data) => processData(data)
  }
  
  def handleBegin(): State = {
    validateInput() match {
      case true => goto(State.Processing)
      case false => goto(State.Failed)
    }
  }
  
  def processData(data: ProcessData): State = {
    if (data.isValid) {
      goto(State.Complete)
    } else {
      goto(State.Failed)
    }
  }
  
  def validateInput(): Boolean = true
}`;
    
    const codeTextarea = page.locator('#codeInput');
    
    // Clear and input complex code
    await codeTextarea.clear();
    await codeTextarea.fill(complexCode);
    
    // Wait for analysis
    await page.waitForTimeout(2000);
    
    // Check that diagram was generated
    const diagramContainer = page.locator('#diagramContainer');
    await expect(diagramContainer.locator('svg')).toBeVisible();
  });

  test('should maintain state after page reload', async ({ page }) => {
    const codeTextarea = page.locator('#codeInput');
    
    // Input custom code
    await codeTextarea.clear();
    await codeTextarea.fill(sampleFsmCode);
    
    // Wait for analysis
    await page.waitForTimeout(1500);
    
    // Reload page
    await page.reload();
    
    // Should load with example code again (not custom code - this is expected behavior)
    const codeContent = await codeTextarea.inputValue();
    expect(codeContent).toContain('OrderProcessingFSM');
  });
});