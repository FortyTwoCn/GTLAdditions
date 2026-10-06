#!/usr/bin/env python3
"""Validate assembled JARs on an ordinary NeoForge installer server, then reload its disk save.

No Gradle, test mod, custom network packets or development classpaths are used.
The directory must already contain the installer output, verified native pack
profile, staged artifact manifest and accepted Minecraft EULA.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import socket
import subprocess
import time


def contents(path):
    return path.read_text(errors="replace") if path.is_file() else ""


def wait_for(process, log, marker, timeout=600):
    deadline = time.monotonic() + timeout
    while marker not in contents(log):
        if process.poll() is not None:
            raise RuntimeError(f"Packaged server exited with {process.returncode}; inspect {log}")
        if time.monotonic() >= deadline:
            raise RuntimeError(f"Packaged server timed out waiting for {marker}; inspect {log}")
        time.sleep(1)


def command(process, value):
    process.stdin.write(value + "\n")
    process.stdin.flush()


def stop(process):
    if process.poll() is not None:
        return
    command(process, "stop")
    try:
        process.wait(timeout=60)
    except subprocess.TimeoutExpired:
        process.terminate()
        process.wait(timeout=15)
        raise RuntimeError("Packaged server did not save and stop normally")
    if process.returncode != 0:
        raise RuntimeError(f"Packaged server failed normal exit: {process.returncode}")


def run_round(directory, java, output, reload):
    label = "reload" if reload else "first"
    log = output / f"packaged-server-{label}.log"
    args_file = directory / "libraries/net/neoforged/neoforge/21.1.248/unix_args.txt"
    with log.open("w") as stream:
        process = subprocess.Popen([str(java), "-Xmx3G", "-XX:ActiveProcessorCount=4", "-Dmixin.debug.verify=true", "-Dmixin.checks=true",
                                    "-Dmixin.debug.strict.unique=true", "@" + str(args_file), "--nogui"],
                                   cwd=directory, stdin=subprocess.PIPE, stdout=stream,
                                   stderr=subprocess.STDOUT, text=True)
    try:
        wait_for(process, log, 'For help, type "help"')
        command(process, "forceload add 0 0")
        if not reload:
            command(process, "setblock 0 96 0 gtceu:steam_piston_hammer")
            command(process, "setblock 4 96 0 gtladditions:arcanic_astrograph")
        command(process, "execute if block 0 96 0 gtceu:steam_piston_hammer run say GTL_PACKAGED_CORE_BLOCK=" + label)
        command(process, "execute if block 4 96 0 gtladditions:arcanic_astrograph run say GTL_PACKAGED_ADDITIONS_BLOCK=" + label)
        command(process, "data get block 0 96 0")
        command(process, "data get block 4 96 0")
        for dimension in ("ad_astra:moon", "ad_astra:mars", "sgjourney:lantea"):
            command(process, f"execute in {dimension} run say GTL_PACKAGED_DIMENSION={dimension}:{label}")
            wait_for(process, log, f"GTL_PACKAGED_DIMENSION={dimension}:{label}", 90)
        wait_for(process, log, "GTL_PACKAGED_CORE_BLOCK=" + label, 15)
        wait_for(process, log, "GTL_PACKAGED_ADDITIONS_BLOCK=" + label, 15)
        if contents(log).count("has the following block data") < 2:
            raise RuntimeError(f"Both production block entities were not readable: {log}")
        command(process, "save-all flush")
        wait_for(process, log, "Saved the game", 60)
        stop(process)
    finally:
        stop(process)
    text = contents(log)
    if "gtl_native_probe" in text or "nativeProductionProbe" in text:
        raise RuntimeError(f"Development probe appeared in packaged runtime: {log}")
    if re.search(r"Parsing error loading recipe gtl(core|additions):", text) or "Input item is not one of:" in text or "Output item is not one of:" in text:
        raise RuntimeError(f"Production recipe rejected in packaged runtime: {log}")
    if re.search(r"failed to add recipe from staging into lookup DB: gtl(core|additions):", text):
        raise RuntimeError(f"Production recipe missing from native machine lookup: {log}")
    if not re.search(r"Loaded 2/2 KubeJS startup scripts[^\n]*with 0 errors", text):
        raise RuntimeError(f"Native registration profile completion missing: {log}")
    dimensions = sorted(set(re.findall(r"Saving chunks for level '.*?'/([^\s]+)", text)))
    if len(dimensions) < 16:
        raise RuntimeError(f"Packaged runtime did not save all native dimensions: {dimensions}")
    print(f"Packaged native server {label}: production blocks, native block entities, dimensions and save passed", flush=True)
    return {"log": log.name, "sha256": hashlib.sha256(log.read_bytes()).hexdigest(),
            "dimensions_saved": dimensions, "recipes_loaded": re.findall(r"Loaded (\d+) recipes", text),
            "normal_exit": process.returncode == 0, "production_block_entities": 2}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", type=Path, required=True)
    parser.add_argument("--java", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    directory = args.directory.resolve()
    manifest_path = directory / "native-artifacts.json"
    manifest = json.loads(manifest_path.read_text())
    for record in manifest["mods"]:
        jar = directory / "mods" / record["file"]
        if hashlib.sha256(jar.read_bytes()).hexdigest() != record["sha256"]:
            raise RuntimeError(f"Staged production JAR differs: {jar}")
    if not (directory / "native-profile-manifest.json").is_file() or "eula=true" not in contents(directory / "eula.txt"):
        raise RuntimeError("Source-verified pack profile and accepted EULA are required")
    marker = "# Packaged native validation; isolated loopback profile."
    ownership = directory / ".native-packaged-server-validation"
    properties = directory / "server.properties"
    # Vanilla rewrites server.properties on startup, including its leading comment.
    # Keep ownership outside that file so a second invocation retains the same guard.
    if properties.is_file() and not ownership.is_file() and not contents(properties).startswith(marker):
        raise RuntimeError(f"Existing server configuration preserved: {properties}")
    with socket.socket() as probe:
        if probe.connect_ex(("127.0.0.1", 25577)) == 0:
            raise RuntimeError("Packaged validation port 25577 is already occupied")
    ownership.write_text(marker + "\n")
    properties.write_text(marker + "\nserver-ip=127.0.0.1\nserver-port=25577\nonline-mode=false\n"
                          "enforce-secure-profile=false\nlevel-name=world\nlevel-type=minecraft:flat\n"
                          "generate-structures=false\ngamemode=creative\ndifficulty=peaceful\n"
                          "spawn-protection=0\nview-distance=3\nsimulation-distance=3\n")
    args.output.mkdir(parents=True, exist_ok=True)
    rounds = [run_round(directory, args.java.resolve(), args.output, reload) for reload in (False, True)]
    proof = {"result": "PASS", "minecraft": "1.21.1", "neoforge": "21.1.248",
             "ordinary_installer_server": True, "production_probe_present": False,
             "actual_disk_restart": True, "rounds": rounds,
             "artifact_manifest_sha256": hashlib.sha256(manifest_path.read_bytes()).hexdigest(),
             "limitations": ["No physical player connects in this packaged server smoke test.",
                             "Complete physical-player and machine-function checks are recorded separately."]}
    (args.output / "packaged-server-evidence.json").write_text(json.dumps(proof, indent=2) + "\n")
    print("GTL_PACKAGED_NATIVE_SERVER=PASS ordinary_installer=true production_jars_only=true actual_disk_restart=true", flush=True)


if __name__ == "__main__":
    main()
