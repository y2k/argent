import json
import re
import sqlite3
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

SERIAL = sys.argv[1]
PACKAGE = 'im.y2k.argent'
OUT = Path('.build/counter-device-check')
OUT.mkdir(parents=True, exist_ok=True)

def adb(*args, **kwargs):
    return subprocess.check_output(['adb', '-s', SERIAL, *args], **kwargs)

def launch():
    adb('shell', 'am start -W -n "im.y2k.argent/argent.app\\$MainActivity"')

def layout(label):
    path = OUT / f'{label}.json'
    subprocess.check_output(['android', 'layout', '--device', SERIAL, '--full', '--output', str(path)])
    nodes = json.loads(path.read_text())
    if nodes and 'content' in nodes[0]:
        nodes = next(window['content'] for window in nodes
                     if window['type'] == 'APPLICATION' and window.get('active'))
    def flatten(nodes):
        for node in nodes:
            yield node
            yield from flatten(node.get('children', []))
    return list(flatten(nodes))

def check(expected, label):
    nodes = layout(label)
    assert values(nodes) == [expected], (label, values(nodes))
    check_controls(nodes, 1)
    print(f'{label}: {expected}', flush=True)
    return nodes

def tap(nodes, description, times=1):
    node = [n for n in nodes if n.get('content-desc') == description][-1]
    x, y = re.findall(r'\d+', node['center'])
    for _ in range(times):
        adb('shell', 'input', 'tap', x, y)

def values(nodes):
    return [int(n['text']) for n in nodes
            if n['class'] == 'android.widget.TextView' and re.fullmatch(r'-?\d+', n.get('text', ''))]

def check_controls(nodes, messages):
    for description in ('Increment', 'Decrement', 'New message', 'Scan QR'):
        assert len([n for n in nodes if n.get('content-desc') == description]) == messages

def check_messages(expected, label):
    nodes = layout(label)
    assert values(nodes) == expected, (label, values(nodes))
    check_controls(nodes, len(expected))
    return nodes

log_start = adb('shell', 'date "+%m-%d %H:%M:%S.000"').decode().strip()
adb('shell', 'am', 'force-stop', PACKAGE)
launch()
nodes = check(0, 'initial')
tap(nodes, 'Increment', 2)
nodes = check(2, 'two-increments')
tap(nodes, 'Decrement')
nodes = check(1, 'one-decrement')
tap(nodes, 'Decrement', 2)
nodes = check(-1, 'negative')
tap(nodes, 'Increment', 20)
nodes = check(19, 'burst-increment')
tap(nodes, 'Decrement', 20)
check(-1, 'burst-decrement')

adb('shell', 'am', 'force-stop', PACKAGE)
fixture = OUT / 'pending.db'
fixture.unlink(missing_ok=True)
with sqlite3.connect(fixture) as db:
    db.execute('CREATE TABLE events (id INTEGER PRIMARY KEY AUTOINCREMENT, event TEXT NOT NULL)')
    db.executemany('INSERT INTO events(event) VALUES (?)', [('domain:increment',), ('domain:increment',), ('domain:decrement',)])
adb('shell', 'run-as', PACKAGE, 'rm', '-f', 'databases/events.db', 'databases/events.db-journal', 'databases/events.db-wal', 'databases/events.db-shm')
adb('shell', 'run-as im.y2k.argent sh -c "cat > databases/events.db"', input=fixture.read_bytes())
launch()
check(1, 'pending-replayed')
adb('shell', 'am', 'force-stop', PACKAGE)
processed = OUT / 'processed.db'
processed.write_bytes(adb('exec-out', 'run-as', PACKAGE, 'cat', 'databases/events.db'))
with sqlite3.connect(processed) as db:
    assert db.execute('SELECT count(*) FROM events').fetchone()[0] == 0
print('queue drained', flush=True)
launch()
nodes = check(0, 'empty-restart')
tap(nodes, 'Increment')
check(1, 'before-recreation')
adb('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
adb('shell', 'settings', 'put', 'system', 'user_rotation', '1')
nodes = check(0, 'after-recreation')
tap(nodes, 'Increment')
check(1, 'single-consumer')
adb('shell', 'settings', 'put', 'system', 'user_rotation', '0')
adb('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '1')
check(0, 'portrait-recreation')

tap(layout('chat-initial'), 'Increment')
nodes = check(1, 'chat-first-value')
tap(nodes, 'New message')
nodes = check_messages([1, 1], 'chat-added')
tap(nodes, 'Increment')
nodes = check_messages([1, 2], 'chat-active-updated')

# android layout omits enabled; read the platform accessibility attribute.
# Release its UiAutomation connection before starting the platform dumper.
adb('shell', 'am', 'force-stop', 'com.android.cli.interact.instrumentation')
adb('shell', 'uiautomator', 'dump', '/sdcard/argent-chat.xml')
xml = ET.fromstring(adb('exec-out', 'cat', '/sdcard/argent-chat.xml'))
buttons = [n for n in xml.iter('node') if n.get('class') == 'android.widget.Button']
for description in ('Increment', 'Decrement', 'New message', 'Scan QR'):
    assert [n.get('enabled') for n in buttons if n.get('content-desc') == description] == ['false', 'true']
for old in [n for n in nodes if n['class'] == 'android.widget.Button'][:4]:
    x, y = re.findall(r'\d+', old['center'])
    adb('shell', 'input', 'tap', x, y)
check_messages([1, 2], 'frozen-buttons')

for value in range(3, 17):
    tap(nodes, 'New message')
    nodes = layout(f'message-{value}-added')
    # No manual scroll: appending must make the new button reachable.
    new_button = [n for n in nodes if n.get('content-desc') == 'New message'][-1]
    assert not new_button.get('off-screen', False)
    before = [n.get('bounds') for n in nodes if n.get('content-desc') == 'New message']
    tap(nodes, 'Increment')
    nodes = layout(f'message-{value}-updated')
    assert values(nodes)[-1] == value, values(nodes)
    assert [n.get('bounds') for n in nodes if n.get('content-desc') == 'New message'] == before

# --full includes offscreen children (without bounds), so this checks the
# actual retained history rather than only the currently visible viewport.
check_messages(list(range(2, 17)), 'retained-history')
print('chat history, frozen buttons, scrolling and 15-message limit passed', flush=True)

adb('shell', 'am', 'force-stop', PACKAGE)
launch()
check(0, 'chat-history-reset')
logs = adb('logcat', '-d', '-T', log_start, '-s', 'AndroidRuntime').decode()
(OUT / 'runtime-logcat.txt').write_text(logs)
assert 'FATAL EXCEPTION' not in logs, logs
print('device checks passed', flush=True)
