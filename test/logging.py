"""Проверяет реальный stderr: отложенность, вложенность и результат операций."""

from pathlib import Path
import subprocess


def evaluate(paths, source):
    program = "\n".join(Path(path).read_text() for path in paths) + "\n" + source
    result = subprocess.run(
        ["ly2k", "--target", "eval"], input=program,
        text=True, capture_output=True, check=True,
    )
    return result.stderr.splitlines()


lines = evaluate(
    ["src/argent/vendor/effect.clj", "src/argent/vendor/event.clj"],
    """
(ns logging-check
  (:require [argent.vendor.effect :as fx] [argent.vendor.event :as event]))
(let [env (-> {}
              (fx/register :inner (fn [arg] arg))
              (event/register :outer (fn [arg] (event/dispatch :inner arg))))
      call (event/dispatch :outer 42)]
  (eprintln "before-execution")
  (assert (= (call env) 42))
  (eprintln "after-execution"))
""",
)
assert lines == [
    "before-execution",
    "Argent op.begin name=outer",
    "Argent op.begin name=inner",
    "Argent op.end name=inner",
    "Argent op.end name=outer",
    "after-execution",
], lines
lines = evaluate(
    ["src/argent/vendor/effect.clj", "src/argent/vendor/event.clj",
     "src/argent/main.clj", "src/argent/application.clj",
     "src/argent/vendor/event_pump.clj", "test/event_pump.eval"],
    "",
)
delivery = [line for line in lines if not line.startswith("Argent op.")]
expected = []
for record_id, event in [(42, "domain:increment"), (7, "domain:increment"),
                         (12, "domain:increment"), (18, "domain:decrement")]:
    expected += [
        f"Argent queue.handle.begin id={record_id} event={event}",
        f"Argent queue.handle.end id={record_id} event={event}",
        f"Argent queue.ack id={record_id} event={event}",
        f"Argent render.end source=domain id={record_id} event={event}",
    ]
assert delivery == expected, delivery
assert evaluate(["src/argent/vendor/event_pump.clj"], """
(argent.vendor.event_pump/step {:peek (fn [] nil)})
""") == []
lines = evaluate(["src/argent/vendor/qr_image.clj", "test/qr_image.eval"], "")
expected = []
for status in ["success", "cancelled", "success", "scan-failed"]:
    expected += ["Argent qr.prepare", "Argent qr.ready", "Argent qr.scan.start",
                 f"Argent qr.result status={status}", "Argent qr.stop"]
expected += ["Argent qr.prepare", "Argent qr.stop",
             "Argent qr.prepare", "Argent qr.ready", "Argent qr.scan.start",
             "Argent qr.stop"]
assert lines == expected, lines
print("logging ok")
