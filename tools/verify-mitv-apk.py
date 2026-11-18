"""Check the candidate's merged manifest and dex metadata before distribution."""
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
from io import BytesIO
from zipfile import ZipFile

apk = Path(sys.argv[1])
android = "{http://schemas.android.com/apk/res/android}"
root = ET.parse(apk.with_name("manifest.xml")).getroot()
assert root.get("package") == "com.ts.launcher", "Wrong installed package"
app = root.find("application")
activity = next(a for a in app.findall("activity")
                if a.get(android + "name") == "app.orchard.tv.MainActivity")
assert any(c.get(android + "name") == "android.intent.category.HOME"
           for c in activity.findall("intent-filter/category")), "Missing HOME filter"
assert any(p.get(android + "authorities") == "com.ts.launcher.files"
           for p in app.findall("provider")), "File provider collision"
assert any(s.get(android + "name") == "app.orchard.tv.guard.HomeGuardService"
           for s in app.findall("service")), "Wrong guard class"
with ZipFile(apk) as archive:
    dm = archive.read("assets/aerial-dm/28.dm")
with ZipFile(BytesIO(dm)) as archive:
    meta = json.loads(archive.read("manifest.json"))
assert meta["packageName"] == "com.ts.launcher", "Wrong dex metadata package"
assert meta["versionCode"] == int(root.get(android + "versionCode")), "Wrong dex metadata version"
apk.with_suffix(".dm").write_bytes(dm)
print("Package, HOME component, provider, guard and Android 10 dex metadata verified.")
