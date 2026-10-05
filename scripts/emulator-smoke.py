#!/usr/bin/env python3
"""End-to-end checks on an emulator only; never clear data on a physical phone."""
import argparse
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('--adb', default='adb')
parser.add_argument('--serial', default='emulator-5554')
args = parser.parse_args()
if not args.serial.startswith('emulator-'):
    raise SystemExit('This test clears demo state and is restricted to emulators.')
root = Path(__file__).resolve().parents[1]
output = root / 'app/build/reports/smoke'
output.mkdir(parents=True, exist_ok=True)

def adb(*command, timeout=60):
    return subprocess.check_output([args.adb, '-s', args.serial, *command], timeout=timeout, stderr=subprocess.STDOUT)

def nodes():
    result = adb('shell', 'uiautomator', 'dump', '/sdcard/ttfa-smoke.xml')
    if b'dumped to:' not in result.lower():
        return []  # A transient null root must never reuse a stale hierarchy.
    return list(ET.fromstring(adb('exec-out', 'cat', '/sdcard/ttfa-smoke.xml')).iter('node'))

def wait_for(text, timeout=90):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        current = nodes()
        found = next((n for n in current if text in n.get('text', '')), None)
        if found is not None:
            return found
        time.sleep(1)
    raise AssertionError('Missing UI text: ' + text)

def control(name, timeout=90):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        found = next((n for n in nodes() if n.get('content-desc') == name and n.get('checkable') == 'true'), None)
        if found is not None:
            return found
        time.sleep(1)
    raise AssertionError('Missing named accessible control: ' + name)

def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))

def click(text):
    print("Checking/tapping:", text, flush=True)
    tap(wait_for(text))

def screenshot(name):
    (output / name).write_bytes(adb('exec-out', 'screencap', '-p'))

adb('shell', 'input', 'keyevent', '224')
adb('shell', 'wm', 'dismiss-keyguard')
print('Launching fresh demo state', flush=True)
adb('shell', 'pm', 'clear', 'com.ttfa')
adb('shell', 'am', 'start', '-n', 'com.ttfa/.MainActivity')
wait_for('Where to?', timeout=180)
screenshot('home.png')
click('Common Sense Coffee')
click('Start Navigation')
wait_for('ETA')
click('Pause')
wait_for('Resume')
click('Miss a turn')
wait_for('WTF MODE WINS')
wait_for('Saves 7.6 mi and 14 min')
screenshot('wtf-wins.png')
click('End navigation')
click('Settings')
wait_for('Your co-pilot')
tap(control('WTF MODE'))
tap(control('Full roast'))
click('Done')
click('Start Navigation')
click('Pause')
click('Miss a turn')
wait_for('NORMAL REROUTE')
wait_for('You missed the fucking turn.')
screenshot('normal-fallback.png')
click('End navigation')
adb('shell', 'am', 'force-stop', 'com.ttfa')
adb('shell', 'am', 'start', '-n', 'com.ttfa/.MainActivity')
click('Settings')
wait_for('Your co-pilot')
assert control('WTF MODE').get('checked') == 'false', 'WTF preference must persist'
assert control('Full roast').get('checked') == 'true', 'Personality preference must persist'
tap(control('Demo drive'))
click('Done')
wait_for('GPS MAP')
wait_for('Real driving directions')
click('Enable phone GPS')
# Android 9's runtime permission dialog uses an uppercase DENY label.
click('DENY')
wait_for('Location permission declined')
screenshot('gps-permission-denied.png')
click('Try demo navigation')
wait_for('Where to?')
print('PASS: launch, destination, navigation, pause, deviation/reroute, WTF win, normal fallback, personality, persisted settings, GPS map mode, and denied-location fallback')
print('Screenshots:', output)
