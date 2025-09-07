import { test, expect } from '@playwright/test';

test.describe('Akka FSM Visualizer - Edge Cases', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:8000');
  });

  test('should handle very large FSM without crashing', async ({ page }) => {
    // Generate a large FSM with many states
    let largeCode = `object State {\n`;
    for (let i = 0; i < 50; i++) {
      largeCode += `  case object State${i} extends ProcessingState\n`;
    }
    largeCode += `  case object Complete extends ProcessingState
  case object Failed extends ProcessingState
}

class LargeFSM extends FSM[ProcessingState, Data] {`;
    
    for (let i = 0; i < 50; i++) {
      const nextState = i === 49 ? 'Complete' : `State${i + 1}`;
      largeCode += `
  when(State.State${i}) {
    case Event(Next, _) => goto(State.${nextState})
    case Event(Fail, _) => goto(State.Failed)
  }`;
    }
    
    largeCode += `
  when(State.Complete) {
    case Event(Reset, _) => goto(State.State0)
  }
  
  when(State.Failed) {
    case Event(Reset, _) => goto(State.State0)
  }
}`;

    const codeTextarea = page.locator('#codeInput');
    
    // Input large FSM code
    await codeTextarea.clear();
    await codeTextarea.fill(largeCode);
    
    // Wait for analysis (might take longer)
    await page.waitForTimeout(5000);
    
    // Should still generate diagram without crashing
    const diagramContainer = page.locator('#diagramContainer');
    await expect(diagramContainer.locator('svg')).toBeVisible();
  });

  test('should handle FSM with special characters in names', async ({ page }) => {
    const specialCharCode = `object MyStates {
  case object StartState extends ProcessingState  
  case object ProcessingState extends ProcessingState
  case object EndState extends ProcessingState
}

class SpecialFSM extends FSM[ProcessingState, Data] {
  when(MyStates.StartState) {
    case Event(Begin, _) => goto(MyStates.ProcessingState)
  }
  
  when(MyStates.ProcessingState) {
    case Event(Complete, _) => goto(MyStates.EndState)
  }
}`;
    
    const codeTextarea = page.locator('#codeInput');
    
    await codeTextarea.clear();
    await codeTextarea.fill(specialCharCode);
    
    await page.waitForTimeout(1500);
    
    const diagramContainer = page.locator('#diagramContainer');
    await expect(diagramContainer.locator('svg')).toBeVisible();
  });

  test('should handle FSM with no transitions (only state definitions)', async ({ page }) => {
    const noTransitionsCode = `// Events
sealed trait OrderEvent
case object PlaceOrder extends OrderEvent

// States  
sealed trait OrderState
object State {
  case object WaitingForOrder extends OrderState
  case object PaymentPending extends OrderState
  case object Cancelled extends OrderState
}`;
    
    const codeTextarea = page.locator('#codeInput');
    const diagramContainer = page.locator('#diagramContainer');
    
    await codeTextarea.clear();
    await codeTextarea.fill(noTransitionsCode);
    
    await page.waitForTimeout(1500);
    
    // Should show "no transitions" message
    const diagramContent = await diagramContainer.innerHTML();
    expect(diagramContent).toContain('NoTransitions');
  });

  test('should handle malformed Scala code gracefully', async ({ page }) => {
    const malformedCodes = [
      'class FSM {', // Incomplete class
      'when(State.Something { case Event => }', // Malformed when block
      'object State extends trait', // Wrong syntax
      '### This is not scala at all ###', // Completely invalid
    ];
    
    const codeTextarea = page.locator('#codeInput');
    const errorDiv = page.locator('#error');
    
    for (const malformedCode of malformedCodes) {
      await codeTextarea.clear();
      await codeTextarea.fill(malformedCode);
      
      await page.waitForTimeout(1000);
      
      // Should show error for malformed code
      await expect(errorDiv).toBeVisible();
      await expect(errorDiv).toContainText('Parse error');
    }
  });

  test('should handle rapid code changes without crashing', async ({ page }) => {
    const codeTextarea = page.locator('#codeInput');
    
    const codes = [
      `object State { case object A extends State }
class FSM1 extends FSM[State, Data] {
  when(State.A) { case Event(E, _) => goto(State.A) }
}`,
      `object OtherState { case object B extends OtherState }
class FSM2 extends FSM[OtherState, Data] {
  when(OtherState.B) { case Event(E, _) => goto(OtherState.B) }
}`,
      `object ThirdState { case object C extends ThirdState }
class FSM3 extends FSM[ThirdState, Data] {
  when(ThirdState.C) { case Event(E, _) => goto(ThirdState.C) }
}`,
    ];
    
    // Rapidly change code multiple times
    for (let i = 0; i < codes.length; i++) {
      await codeTextarea.clear();
      await codeTextarea.fill(codes[i]);
      await page.waitForTimeout(200); // Short wait between changes
    }
    
    // Wait for final analysis
    await page.waitForTimeout(2000);
    
    // Should still work
    const diagramContainer = page.locator('#diagramContainer');
    await expect(diagramContainer.locator('svg')).toBeVisible();
  });

  test('should handle FSM with circular references', async ({ page }) => {
    const circularCode = `object State {
  case object A extends ProcessState
  case object B extends ProcessState
  case object C extends ProcessState
}

class CircularFSM extends FSM[ProcessState, Data] {
  when(State.A) {
    case Event(Next, _) => goto(State.B)
  }
  
  when(State.B) {
    case Event(Next, _) => goto(State.C)
  }
  
  when(State.C) {
    case Event(Next, _) => goto(State.A) // Back to A - circular!
  }
}`;
    
    const codeTextarea = page.locator('#codeInput');
    
    await codeTextarea.clear();
    await codeTextarea.fill(circularCode);
    
    await page.waitForTimeout(2000);
    
    const diagramContainer = page.locator('#diagramContainer');
    await expect(diagramContainer.locator('svg')).toBeVisible();
  });

  test('should handle mixed case state names', async ({ page }) => {
    const mixedCaseCode = `object ProcessingStates {
  case object IdleState extends ProcessingState
  case object ACTIVE_STATE extends ProcessingState  
  case object processing_state extends ProcessingState
  case object FinalState extends ProcessingState
}

class MixedCaseFSM extends FSM[ProcessingState, Data] {
  when(ProcessingStates.IdleState) {
    case Event(Start, _) => goto(ProcessingStates.ACTIVE_STATE)
  }
  
  when(ProcessingStates.ACTIVE_STATE) {
    case Event(Process, _) => goto(ProcessingStates.processing_state)
  }
  
  when(ProcessingStates.processing_state) {
    case Event(Complete, _) => goto(ProcessingStates.FinalState)
  }
}`;
    
    const codeTextarea = page.locator('#codeInput');
    
    await codeTextarea.clear();
    await codeTextarea.fill(mixedCaseCode);
    
    await page.waitForTimeout(1500);
    
    const diagramContainer = page.locator('#diagramContainer');
    await expect(diagramContainer.locator('svg')).toBeVisible();
  });

  test('should handle FSM with only stopSuccess transitions', async ({ page }) => {
    const stopOnlyCode = `object State {
  case object Active extends ProcessState
  case object Stopping extends ProcessState
}

class StopOnlyFSM extends FSM[ProcessState, Data] {
  when(State.Active) {
    case Event(Stop, _) => stopSuccess()
  }
  
  when(State.Stopping) {
    case Event(ForceStop, _) => stopSuccess()
  }
}`;
    
    const codeTextarea = page.locator('#codeInput');
    
    await codeTextarea.clear();
    await codeTextarea.fill(stopOnlyCode);
    
    await page.waitForTimeout(1500);
    
    const diagramContainer = page.locator('#diagramContainer');
    await expect(diagramContainer.locator('svg')).toBeVisible();
    
    // Should contain stop transitions
    const svgContent = await diagramContainer.innerHTML();
    expect(svgContent).toContain('stop');
  });

  test('should handle browser refresh during analysis', async ({ page }) => {
    const codeTextarea = page.locator('#codeInput');
    
    // Start inputting code
    await codeTextarea.clear();
    await codeTextarea.type('object State { case object A extends State }', { delay: 50 });
    
    // Refresh during typing
    await page.reload();
    
    // Should load normally with example code
    const codeContent = await codeTextarea.inputValue();
    expect(codeContent).toContain('OrderProcessingFSM');
    
    // Diagram should be visible
    await page.waitForTimeout(2000);
    const diagramContainer = page.locator('#diagramContainer');
    await expect(diagramContainer.locator('svg')).toBeVisible();
  });

  test('should handle very long state and event names', async ({ page }) => {
    const longNamesCode = `object VeryLongStateNames {
  case object ThisIsAVeryLongStateNameThatShouldStillWorkCorrectly extends ProcessingState
  case object AnotherExtremelyLongStateNameForTestingPurposes extends ProcessingState
  case object ShortState extends ProcessingState
}

class LongNamesFSM extends FSM[ProcessingState, Data] {
  when(VeryLongStateNames.ThisIsAVeryLongStateNameThatShouldStillWorkCorrectly) {
    case Event(VeryLongEventNameThatMightCauseIssues, _) => 
      goto(VeryLongStateNames.AnotherExtremelyLongStateNameForTestingPurposes)
  }
  
  when(VeryLongStateNames.AnotherExtremelyLongStateNameForTestingPurposes) {
    case Event(ShortEvent, _) => goto(VeryLongStateNames.ShortState)
  }
}`;
    
    const codeTextarea = page.locator('#codeInput');
    
    await codeTextarea.clear();
    await codeTextarea.fill(longNamesCode);
    
    await page.waitForTimeout(2000);
    
    const diagramContainer = page.locator('#diagramContainer');
    await expect(diagramContainer.locator('svg')).toBeVisible();
  });
});