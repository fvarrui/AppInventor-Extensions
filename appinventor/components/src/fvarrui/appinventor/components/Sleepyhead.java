package fvarrui.appinventor.components;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.ErrorMessages;

import android.app.Activity;

@DesignerComponent(
		version = 1, 
		description = "Stops execution for the specified milliseconds.", 
		category = ComponentCategory.EXTENSION, 
		nonVisible = true,
		iconName = "images/clock.png"
	)
@SimpleObject(external = true)
public final class Sleepyhead extends AndroidNonvisibleComponent {

	private final Activity activity;

	public Sleepyhead(ComponentContainer container) {
		super(container.$form());
		activity = container.$context();
	}

	@SimpleFunction(description = "Stops execution for the specified milliseconds")
	public void Sleep(final long millis) {

		try {
			
			Thread.sleep(millis);

			// Dispatch the event.
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Awake();
				}
			});

		} catch (InterruptedException e) {
			
			form.dispatchErrorOccurredEvent(Sleepyhead.this, "Sleep", ErrorMessages.ERROR_DEFAULT);
			
		}

	}

	@SimpleEvent(description = "Event triggered when the Sleepyhead component wakes up.")
	public void Awake() {
		EventDispatcher.dispatchEvent(this, "Awake");
	}

}
