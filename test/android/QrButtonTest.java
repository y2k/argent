package argent;

import android.widget.Button;
import android.widget.LinearLayout;
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
                    Object[] payload = {hash_map("err", null, "value", "{\"id\":123}")};
                    Object env = argent.vendor.effect.register(hash_map("context", activity),
                        "qr:pick", (Fn1) listener -> {
                            ((Fn1) listener).call(payload[0]);
                            return null;
                        });
                    Object original = list("qr-image-button", hash_map("text", "Read QR",
                        "onresult", (Fn1) result -> (Fn1) w -> {
                            assertSame(env, w);
                            assertSame(payload[0], result);
                            seen.add("first:" + get(result, "value"));
                            return null;
                        }));
                    Button button = (Button) ((Fn1) argent.vendor.view.create(original)).call(env);
                    button.performClick();
                    assertEquals("first:{\"id\":123}", seen.get(0));
                    Object updated = list("qr-image-button", hash_map("text", "Read again",
                        "onresult", (Fn1) result -> (Fn1) w -> {
                            assertSame(env, w);
                            assertSame(payload[0], result);
                            seen.add("updated:" + get(result, "err") + ":" + get(result, "value"));
                            return null;
                        }));
                    argent.vendor.view.update_BANG_(env, button, updated);
                    assertEquals("Read again", button.getText().toString());
                    payload[0] = hash_map("err", "cancelled", "value", null);
                    button.performClick();
                    assertEquals(2, seen.size());
                    assertEquals("updated:cancelled:null", seen.get(1));
                    var parent = new LinearLayout(activity);
                    parent.addView(button);
                    argent.vendor.view.disable_children_BANG_(parent, 0);
                    assertFalse(button.isEnabled());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
