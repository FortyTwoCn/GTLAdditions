#!/usr/bin/env python3
"""Prepare a Linux x64 client command from ordinary installed NeoForge/vanilla version metadata.

This uses the installer's patched game and published libraries, never ModDevGradle
outputs or probe classes. It does not start the game or modify the mods directory.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import urllib.request
import uuid


def sha1(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha1").hexdigest()


def allowed(entry):
    rules = entry.get("rules")
    if not rules:
        return True
    result = False
    for rule in rules:
        system = rule.get("os", {})
        matches = system.get("name", "linux") == "linux"
        matches &= system.get("arch", "x86_64") in ("x86_64", "amd64")
        matches &= not rule.get("features")
        if matches:
            result = rule["action"] == "allow"
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", required=True, type=Path)
    parser.add_argument("--java", required=True, type=Path)
    parser.add_argument("--assets", required=True, type=Path)
    parser.add_argument("--library-cache", type=Path, help="Optional previously downloaded Maven artifact cache")
    parser.add_argument("--server", default="127.0.0.1:25577")
    args = parser.parse_args()
    root = args.directory.resolve()
    if not (root / ".native-packaged-client-validation").is_file():
        raise SystemExit("Refusing to prepare a client directory not owned by this validation")
    vanilla_path = root / "versions/1.21.1/1.21.1.json"
    neo_path = root / "versions/neoforge-21.1.248/neoforge-21.1.248.json"
    vanilla = json.loads(vanilla_path.read_text())
    neo = json.loads(neo_path.read_text())
    if vanilla["id"] != "1.21.1" or neo["inheritsFrom"] != "1.21.1" or neo["id"] != "neoforge-21.1.248":
        raise SystemExit("Unexpected installed version metadata")
    libraries = {}
    for entry in vanilla["libraries"] + neo["libraries"]:
        if not allowed(entry):
            continue
        parts = entry["name"].split(":")
        identity = (parts[0], parts[1], parts[3] if len(parts) > 3 else "")
        libraries[identity] = entry
    cached = {}
    if args.library_cache:
        for path in args.library_cache.rglob("*.jar"):
            cached.setdefault(path.name, []).append(path)

    def resolve(entry):
        artifact = entry["downloads"]["artifact"]
        relative = Path(artifact["path"])
        if relative.is_absolute() or ".." in relative.parts:
            raise ValueError("Invalid library metadata path")
        target = root / "libraries" / relative
        expected = artifact["sha1"]
        if target.exists():
            if sha1(target) != expected:
                raise ValueError(f"Existing installer library checksum differs: {target}")
            return target
        target.parent.mkdir(parents=True, exist_ok=True)
        for source in cached.get(target.name, []):
            if sha1(source) == expected:
                shutil.copy2(source, target)
                return target
        with urllib.request.urlopen(artifact["url"], timeout=45) as response:
            data = response.read()
        if hashlib.sha1(data).hexdigest() != expected:
            raise ValueError(f"Published library checksum differs: {entry['name']}")
        target.write_bytes(data)
        return target

    with ThreadPoolExecutor(max_workers=6) as executor:
        files = list(executor.map(resolve, libraries.values()))
    game_jar = root / "versions/1.21.1/1.21.1.jar"
    if not game_jar.is_file() or sha1(game_jar) != vanilla["downloads"]["client"]["sha1"]:
        raise SystemExit("Ordinary installer vanilla game JAR checksum differs")
    natives = root / "natives"
    natives.mkdir(exist_ok=True)
    asset_root = args.assets.resolve()
    if not (asset_root / "indexes" / (vanilla["assetIndex"]["id"] + ".json")).is_file():
        raise SystemExit("The selected official asset index has not been downloaded")
    classpath = os.pathsep.join(str(path) for path in files + [game_jar])
    replacements = {
        "classpath": classpath, "classpath_separator": os.pathsep,
        "library_directory": str(root / "libraries"), "natives_directory": str(natives),
        "launcher_name": "GTLNativeInstallerValidation", "launcher_version": "1",
        "version_name": "1.21.1", "version_type": "release",
        "auth_player_name": "NativeGTLRelease", "auth_access_token": "0", "user_type": "legacy",
        "auth_uuid": str(uuid.UUID(bytes=hashlib.md5(b"OfflinePlayer:NativeGTLRelease").digest(), version=3)),
        "clientid": "", "auth_xuid": "", "game_directory": str(root),
        "assets_root": str(asset_root), "assets_index_name": vanilla["assetIndex"]["id"],
    }

    def expand(items):
        output = []
        for entry in items:
            if isinstance(entry, dict):
                if not allowed(entry):
                    continue
                entry = entry["value"]
            for value in entry if isinstance(entry, list) else [entry]:
                value = re.sub(r"\$\{([^}]+)\}", lambda match: replacements[match[1]], value)
                output.append(value)
        return output

    command = [str(args.java.resolve()), "-Xmx3G", "-XX:ActiveProcessorCount=4",
               "-Dmixin.debug.verify=true", "-Dmixin.checks=true", "-Dmixin.debug.strict.unique=true"]
    command += expand(vanilla["arguments"]["jvm"] + neo["arguments"]["jvm"])
    command += [neo["mainClass"]]
    command += expand(vanilla["arguments"]["game"] + neo["arguments"]["game"])
    command += ["--width", "1280", "--height", "900", "--quickPlayMultiplayer", args.server]
    options = root / "options.txt"
    if not options.exists():
        options.write_text("onboardAccessibility:false\nguiScale:2\nenableVsync:false\nmaxFps:30\n"
                           "renderDistance:3\nsimulationDistance:3\nsoundCategory_master:0.0\ntutorialStep:none\n")
    proof = {
        "command": command, "directory": str(root), "ordinary_installer": True,
        "production_probe_present": False, "environment": {"LP_NUM_THREADS": "2", "OMP_NUM_THREADS": "2"},
        "vanilla_metadata_sha256": hashlib.sha256(vanilla_path.read_bytes()).hexdigest(),
        "neoforge_metadata_sha256": hashlib.sha256(neo_path.read_bytes()).hexdigest(),
        "official_library_sha1": {str(path.relative_to(root)): sha1(path) for path in files + [game_jar]},
        "scope": "isolated loopback offline validation; installed published JARs only",
    }
    (root / "native-client-launch.json").write_text(json.dumps(proof, indent=2) + "\n")
    print(f"Prepared ordinary NeoForge client launch from {len(files)} verified published libraries")


if __name__ == "__main__":
    main()
