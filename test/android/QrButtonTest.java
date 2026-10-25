package argent;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.view.ViewGroup;
import androidx.test.core.app.ActivityScenario;
import org.junit.Test;
import java.util.ArrayList;
import static org.junit.Assert.*;
import static y2k.language.language_runtime.*;

public class QrButtonTest {
    @Test public void resultUsesCurrentListenerAndOldButtonsAreDisabled() {
        try (var scenario = ActivityScenario.launch(app.MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    var seen = new ArrayList<String>();
                    Object[] currentEnv = {null};
                    Object output = argent.vendor.chat.start(activity, (Fn0) () -> {
                        assertNotNull(currentEnv[0]);
                        return currentEnv[0];
                    });
                    Object[] payload = {hash_map("err", null, "value", "{\"id\":123}")};
                    Object input = argent.vendor.effect.register(hash_map("context", activity),
                        "qr:pick", (Fn1) listener -> {
                            ((Fn1) listener).call(payload[0]);
                            return null;
                        });
                    Object env = argent.vendor.effect.register(
                        argent.vendor.effect.register(input, "chat:append-message", get(output, "append-message")),
                        "chat:render-message", get(output, "render-message"));
                    currentEnv[0] = env;
                    Object original = list("qr-image-button", hash_map("text", "Read QR",
                        "onresult", (Fn1) result -> (Fn1) w -> {
                            assertSame(env, w);
                            assertSame(payload[0], result);
                            seen.add("first:" + get(result, "value"));
                            return null;
                        }));
                    assertNull(((Fn1) argent.vendor.effect.call("chat:append-message", original)).call(env));
                    var content = (ViewGroup) activity.findViewById(android.R.id.content);
                    var scroll = (ScrollView) content.getChildAt(0);
                    var parent = (LinearLayout) scroll.getChildAt(0);
                    assertEquals(1, parent.getChildCount());
                    Button button = (Button) parent.getChildAt(0);
                    button.performClick();
                    assertEquals("first:{\"id\":123}", seen.get(0));
                    Object updated = list("qr-image-button", hash_map("text", "Read again",
                        "onresult", (Fn1) result -> (Fn1) w -> {
                            assertSame(env, w);
                            assertSame(payload[0], result);
                            seen.add("updated:" + get(result, "err") + ":" + get(result, "value"));
                            return null;
                        }));
                    assertNull(((Fn1) argent.vendor.effect.call("chat:render-message", updated)).call(env));
                    assertEquals("Read again", button.getText().toString());
                    payload[0] = hash_map("err", "cancelled", "value", null);
                    button.performClick();
                    assertEquals(2, seen.size());
                    assertEquals("updated:cancelled:null", seen.get(1));
                    assertNull(((Fn1) argent.vendor.effect.call("chat:append-message", original)).call(env));
                    assertEquals(2, parent.getChildCount());
                    assertFalse(button.isEnabled());
                    Button nextButton = (Button) parent.getChildAt(1);
                    assertTrue(nextButton.isEnabled());
                    nextButton.performClick();
                    assertEquals(3, seen.size());
                    assertEquals("first:null", seen.get(2));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
