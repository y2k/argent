(ns argent.checks.support)

(defn check [label ok]
  (if (not ok)
    (sneaky-throw (AssertionError. label))))

(raw-code "public static Object capture(Object action) throws Exception {
  var original = System.err;
  var bytes = new java.io.ByteArrayOutputStream();
  try (var stream = new java.io.PrintStream(bytes, true, java.nio.charset.StandardCharsets.UTF_8)) {
    System.setErr(stream);
    ((Fn0) action).call();
    return bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
  } finally {
    System.setErr(original);
  }
}")
