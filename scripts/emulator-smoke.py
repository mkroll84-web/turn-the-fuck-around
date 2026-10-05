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
parser.add_argument('--phase', choices=['all', 'demo', 'search', 'input', 'purchases'], default='all')
args = parser.parse_args()
if not args.serial.startswith('emulator-'):
    raise SystemExit('This test clears demo state and is restricted to emulators.')
root = Path(__file__).resolve().parents[1]
output = root / 'app/build/reports/smoke'
output.mkdir(parents=True, exist_ok=True)

def adb(*command, timeout=60):
    return subprocess.check_output([args.adb, '-s', args.serial, *command], timeout=timeout, stderr=subprocess.STDOUT)

def nodes():
    try:
        result = adb('shell', 'uiautomator', 'dump', '/sdcard/ttfa-smoke.xml')
    except subprocess.CalledProcessError as error:
        # API 28's standalone app_process occasionally traps under TCG. Retry
        # that tool failure within the caller's deadline, never stale UI data.
        if error.returncode == 133:
            print('Retrying emulator UI dump after app_process SIGTRAP', flush=True)
            return []
        raise
    if b'dumped to:' not in result.lower():
        print('UI dump unavailable; retrying:', result.decode(errors='replace').strip(), flush=True)
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
        # Current Compose Switch exposes its spoken label on a child of the
        # actual toggle. Read checked/clickable state from the toggle itself.
        found = next((n for n in nodes() if n.get('checkable') == 'true' and
                      any(child.get('content-desc') == name for child in n.iter('node'))), None)
        if found is not None:
            return found
        time.sleep(1)
    raise AssertionError('Missing named accessible control: ' + name)

def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r'-?\d+', node.get('bounds')))
    adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))

def click(text):
    print("Checking/tapping:", text, flush=True)
    tap(wait_for(text))

screen = list(map(int, re.findall(r'\d+', adb('shell', 'wm', 'size').decode())[-2:]))

def scroll_find(predicate, label, after_section=None):
    deadline = time.monotonic() + 300
    swipes = 0
    while time.monotonic() < deadline and swipes < 12:
        current = nodes()
        def visible(n):
            values = list(map(int, re.findall(r'-?\d+', n.get('bounds', ''))))
            if len(values) != 4: return False
            x1, y1, x2, y2 = values
            return x2 > x1 and y2 > y1 and 0 <= (x1+x2)//2 < screen[0] and 285 <= (y1+y2)//2 < screen[1]-140
        candidates = current
        if after_section is not None:
            section_index = next((i for i,n in enumerate(current) if n.get('text') == after_section), None)
            candidates = current[section_index+1:] if section_index is not None else []
        found = next((n for n in candidates if predicate(n) and visible(n)), None)
        if found is not None:
            return found
        if not current:
            time.sleep(1)
            continue
        # Only move after reading a fresh tree, so bridge failures cannot
        # scroll past an unchecked control. Search back upward if necessary.
        swipes += 1
        start_y, end_y = (.84, .37) if swipes <= 6 else (.37, .84)
        adb('shell', 'input', 'swipe', str(int(screen[0] * .97)), str(int(screen[1] * start_y)),
            str(int(screen[0] * .97)), str(int(screen[1] * end_y)), '800')
        time.sleep(1)
    raise AssertionError('Missing scrollable UI control: ' + label)

def scroll_text(text):
    print('Checking scrollable text:', text, flush=True)
    return scroll_find(lambda n: text in n.get('text', ''), text)

def scroll_section_text(section, text):
    print('Checking section:', section, '/', text, flush=True)
    return scroll_find(lambda n: n.get('text') == text, section + '/' + text, after_section=section)

def editable():
    print('Checking editable input field', flush=True)
    return scroll_find(lambda n: n.get('class') == 'android.widget.EditText', 'editable text field')

def screenshot(name):
    (output / name).write_bytes(adb('exec-out', 'screencap', '-p'))

def miss_turn(expected):
    # Slow software emulation can advance the driver near another segment
    # before Pause. A first injected fix may legitimately remain in tolerance.
    for _ in range(4):
        click('Miss a turn')
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            current = nodes()
            texts = [n.get('text', '') for n in current]
            if any(expected in text for text in texts):
                return
            choices = ('WTF MODE WINS', 'NORMAL REROUTE')
            assert not any(choice in text for choice in choices for text in texts), 'Unexpected reroute choice'
            if not any('Rerouting' in text for text in texts) and any('Still within route tolerance' in text for text in texts):
                break
            time.sleep(1)
        else:
            raise AssertionError('No response to missed-turn injection')
    raise AssertionError('Injected fixes never left the route')

def demo_checks():
    click('Common Sense Coffee')
    click('Start Navigation')
    wait_for('ETA')
    click('Pause')
    wait_for('Resume')
    miss_turn('WTF MODE WINS')
    wait_for('Saves 7.6 mi and 14 min')
    screenshot('wtf-wins.png')
    click('End navigation')
    click('Settings')
    wait_for('Your co-pilot')
    tap(control('WTF MODE'))
    tap(control('Unhinged'))
    click('Done')
    click('Start Navigation')
    click('Pause')
    wait_for('Resume')
    miss_turn('NORMAL REROUTE')
    wait_for('You missed the fucking turn.')
    screenshot('normal-fallback.png')
    click('End navigation')
    adb('shell', 'am', 'force-stop', 'com.ttfa')
    adb('shell', 'am', 'start', '-n', 'com.ttfa/.MainActivity')
    click('Settings')
    wait_for('Your co-pilot')
    assert control('WTF MODE').get('checked') == 'false', 'WTF preference must persist'
    assert control('Unhinged').get('checked') == 'true', 'Personality preference must persist'
    click('Done')
    print('PASS: demo navigation, WTF win/fallback, personality and saved settings', flush=True)

def search_checks():
    click('Settings')
    # Exercise runtime configuration without sending a real secret or calling Google.
    tap(editable())
    adb('shell', 'input', 'text', 'PASTE_YOUR_GOOGLE_PLACES_API_KEY_HERE')
    # Move focus off the blinking caret so UIAutomator can reach idle.
    adb('shell', 'input', 'keyevent', '61')
    tap(scroll_text('Save key'))
    scroll_text('Paste the real Google key, not the example placeholder.')
    tap(editable())
    adb('shell', 'input', 'keyevent', '123')
    adb('shell', 'input', 'keyevent', *(['67'] * 60))
    dummy = 'unit-test-not-a-google-api-key'
    adb('shell', 'input', 'text', dummy)
    # Move focus off the blinking caret so UIAutomator can reach idle.
    adb('shell', 'input', 'keyevent', '61')
    tap(scroll_text('Save key'))
    scroll_text('Google key saved securely')
    stored = adb('shell', 'run-as', 'com.ttfa', 'cat', 'shared_prefs/google_places_config.xml').decode()
    assert 'encrypted_key' in stored and dummy not in stored, 'Runtime key must be encrypted at rest'
    adb('shell', 'am', 'force-stop', 'com.ttfa')
    adb('shell', 'am', 'start', '-n', 'com.ttfa/.MainActivity')
    click('Settings')
    scroll_text('A Google key is configured.')
    tap(scroll_text('Remove saved key'))
    scroll_text('Google key removed. Demo drive still works offline.')
    click('Done')
    input_checks()
    print('PASS: encrypted runtime configuration, live address typing, missing-key handling, GPS denial and offline demo separation', flush=True)

def input_checks():
    click('Settings')
    tap(control('Demo drive'))
    click('Done')
    wait_for('GPS MAP')
    field = editable()
    assert field.get('enabled') == 'true', 'Live destination input must accept normal typing'
    tap(field)
    # ADB can inject keys before this software-emulated IME finishes attaching.
    # Wait for the actual keyboard, then let its input batch settle before Tab.
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        ime = adb('shell', 'dumpsys', 'input_method').decode()
        if 'mInputShown=true' in ime and 'mIsInputViewShown=true' in ime:
            break
        time.sleep(1)
    else:
        raise AssertionError('Android keyboard did not become ready')
    time.sleep(2)
    for character in '1600 Amphitheatre Parkway':
        adb('shell', 'input', 'text', '%s' if character == ' ' else character)
        time.sleep(.15)
    time.sleep(3)
    # Move focus off the blinking caret so UIAutomator can reach idle.
    adb('shell', 'input', 'keyevent', '61')
    wait_for('1600 Amphitheatre Parkway')
    scroll_text('Google search needs an API key.')
    finish_input_checks()

def finish_input_checks():
    wait_for('GPS MAP')
    start = scroll_find(lambda n: n.get('clickable') == 'true' and any(child.get('text') == 'Start Navigation' for child in n.iter('node')), 'Start Navigation button')
    assert start.get('enabled') == 'false', 'No resolved place means no navigation'
    assert not any('Common Sense Coffee' in n.get('text', '') for n in nodes()), 'Live search must not show demo results'
    screenshot('live-search-needs-key.png')
    tap(scroll_text('Enable phone GPS'))
    # Android 9's runtime permission dialog uses an uppercase DENY label.
    click('DENY')
    scroll_text('Location permission declined')
    screenshot('gps-permission-denied.png')
    tap(scroll_text('Try demo navigation'))
    wait_for('Where to?')
    wait_for('Common Sense Coffee')
    print('PASS: complete real address typing, unresolved-navigation guard, GPS denial and demo separation', flush=True)

def purchase_checks():
    click('Settings')
    wait_for('Your co-pilot')
    tap(scroll_find(lambda n: n.get('checkable') == 'true' and any(c.get('content-desc') == 'Voo Mode' for c in n.iter()), 'Voo Mode'))
    wait_for('UNLOCK VOO MODE')
    wait_for('One-time purchase:')
    scroll_text('No subscription')
    checkout = scroll_find(lambda n: n.get('enabled') == 'false' and any(child.get('text') == 'UNLOCK VOO MODE' for child in n.iter()), 'disabled Voo checkout')
    assert checkout.get('enabled') == 'false', 'Unconfigured sideload checkout must be disabled'
    screenshot('voo-locked-paywall.png')
    scroll_text('NOT TODAY, DUMBASS')
    click('Close')
    wait_for('Your co-pilot')
    scroll_text('Development Voo override')
    switch = control('Development Voo override')
    assert switch.get('checked') == 'false'
    tap(switch)
    scroll_text('development override (not a purchase)')
    adb('shell', 'am', 'force-stop', 'com.ttfa')
    adb('shell', 'am', 'start', '-n', 'com.ttfa/.MainActivity')
    click('Settings')
    tap(scroll_find(lambda n: n.get('checkable') == 'true' and any(c.get('content-desc') == 'Voo Mode' for c in n.iter()), 'Voo Mode'))
    wait_for('Done')
    assert control('Voo Mode').get('checked') == 'true', 'Development entitlement should select Voo without paywall'
    tap(scroll_text('Absolutely Foul'))
    tap(scroll_section_text('Appearance', 'Dark'))
    screenshot('settings-dark-voo.png')
    click('Done')
    click('Common Sense Coffee')
    click('Start Navigation')
    click('Pause')
    wait_for('Resume')
    miss_turn('WTF MODE WINS')
    wait_for('You absolute fucking disaster')
    screenshot('voo-foul-demo-dark.png')
    click('End navigation')
    click('Settings')
    tap(scroll_section_text('Appearance', 'Light'))
    screenshot('settings-light.png')
    print('PASS: locked paywall, disabled unconfigured checkout, development unlock persistence, Voo Foul demo and both themes', flush=True)

adb('shell', 'input', 'keyevent', '224')
adb('shell', 'wm', 'dismiss-keyguard')
print('Launching fresh demo state', flush=True)
adb('shell', 'pm', 'clear', 'com.ttfa')
adb('shell', 'am', 'start', '-n', 'com.ttfa/.MainActivity')
wait_for('Where to?', timeout=180)
screenshot('home.png')
if args.phase in ('all', 'demo'):
    demo_checks()
if args.phase in ('all', 'search'):
    search_checks()
if args.phase == 'input':
    input_checks()
if args.phase == 'purchases':
    purchase_checks()
print('PASS:', args.phase, 'screen checks')
print('Screenshots:', output)
