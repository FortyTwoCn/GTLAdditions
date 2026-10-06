#!/usr/bin/env python3
"""Archive a completed two-JVM production validation with its actual inputs."""
import argparse
import datetime
import hashlib
import json
from pathlib import Path
import re
import shutil


def digest(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def hashes(root, directories):
    return {str(path.relative_to(root)): digest(path)
            for directory in directories for path in sorted((root / directory).rglob("*"))
            if path.is_file()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--driver-log", type=Path, required=True)
    parser.add_argument("--preparation-log", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    additions = Path(__file__).resolve().parent.parent
    core = additions.parent / "GTLCore"
    if "GTL_NATIVE_PRODUCTION_DEDICATED=PASS" not in args.driver_log.read_text(errors="replace"):
        raise SystemExit("The complete native validation did not pass")
    if "BUILD SUCCESSFUL" not in args.preparation_log.read_text(errors="replace"):
        raise SystemExit("The native launch preparation did not pass")
    artifacts = [args.driver_log, args.preparation_log]
    vm_args = additions / "build/moddev/productionDedicatedServerRunVmArgs.txt"
    artifacts.append(vm_args)
    for round_name in ("first", "reload"):
        for side in ("server", "client"):
            path = additions / "build/native-dedicated" / f"native-{side}-{round_name}.log"
            contents = path.read_text(errors="replace")
            suffix = "_RELOAD" if round_name == "reload" else ""
            required = [f"GTL_NATIVE_PRODUCTION_DEDICATED_{side.upper()}{suffix}=PASS",
                        f"GTL_NATIVE_PRODUCTION_WIRELESS_{side.upper()}=PASS",
                        f"GTL_NATIVE_PRODUCTION_ROCKET_TRAVEL_{side.upper()}=PASS",
                        f"GTL_NATIVE_PRODUCTION_ELEVATOR_{side.upper()}=PASS"]
            if side == "client":
                required.append("GTL_NATIVE_PRODUCTION_BLOOM=PASS")
            if any(marker not in contents for marker in required):
                raise SystemExit(f"Missing completion markers in {path}")
            if any(marker in contents for marker in ("[SIZING]", "Missing value transformer",
                    "Sync: Failed to serialize/deserialize", "Description: Tesselating block model")):
                raise SystemExit(f"Native GUI/render failure in {path}")
            artifacts.append(path)
    # Include only screenshots written by this run, not older files in the game directory.
    started = (additions / "build/native-dedicated/native-server-first.log").stat().st_mtime
    # Use the log's first timestamp instead of its final modification time.
    server_contents = (additions / "build/native-dedicated/native-server-first.log").read_text(errors="replace")
    timestamp = re.search(r"(?m)^\[(\d{2}:\d{2}:\d{2})\]", server_contents)
    if timestamp:
        clock = datetime.time.fromisoformat(timestamp[1])
        day = datetime.datetime.fromtimestamp(started, datetime.timezone.utc).date()
        started = datetime.datetime.combine(day, clock, datetime.timezone.utc).timestamp()
    artifacts.extend(path for path in sorted((additions / "migration/full-dedicated/client/screenshots").glob("production-*.png"))
                     if path.stat().st_mtime >= started)
    output = args.output.resolve()
    if output.exists():
        raise SystemExit(f"Refusing to replace an existing evidence directory: {output}")
    output.mkdir(parents=True)
    for path in artifacts:
        shutil.copy2(path, output / path.name)
    compiled = {project: hashes(additions.parent / project,
                    ["build/classes/java/main", "build/classes/kotlin/main", "build/resources/main"])
                for project in ("GTLCore", "GTLAdditions", "WildcardPattern")}
    source_hashes = {project: hashes(root, ["src/main", "src/generated", "gradle"])
                     for project, root in (("GTLCore", core), ("GTLAdditions", additions))}
    evidence = {
        "date": datetime.datetime.now(datetime.timezone.utc).date().isoformat(),
        "result": "PASS", "minecraft": "1.21.1", "neoforge": "21.1.248",
        "scope": "Complete production Core/Additions in independent physical client and dedicated server JVMs; actual TCP packets, native item/block/menu use and two cycles separated by a clean disk save/restart. Source-verified pack registration/assets and original Space Elevator recipe slice.",
        "targeted_inventory_trigger_jit_workaround": "CompileCommand: exclude net/minecraft/advancements/critereon/InventoryChangeTrigger*." in server_contents,
        "assertions": ["native menus and exact long item/fluid component synchronization",
            "real maintenance/distinct and AE popup/wheel C2S/S2C",
            "real spoilable entity data and protected Lantea travel",
            "three linked, powered, survival-held wireless terminals through native item use",
            "component-key throughput from real AE grid insert/extract to the physical client",
            "Bloom enabled; real empty/ended native builders retired and active support rendered",
            "native survival T7 rocket launch, fuel debit, countdown, Moon lander and named cargo",
            "original formed Space Elevator and computation provider with optical cable route",
            "original recipe paid EU/CWU progress, native departure menu and Mars lander/cargo",
            "all checks repeated after actual disk restart"],
        "limitations": ["One connected physical player; concurrent observers are not tested.",
            "The Space Elevator energy fixture transfers a finite budget into real native input hatches; external batteries and energy cable delivery are not tested.",
            "Wireless throughput is populated and verified on the first terminal; the other two verify the linked native held-item/menu route, not every GT controller row.",
            "Wormhole receive API is exercised; walking through the gate aperture is not tested.",
            "Rocket/elevator journeys verify the real planet landing request, dimension respawn, native lander and cargo; complete natural descent to each planet's surface is not tested. The saved login lander collision is separate fixture preparation.",
            "This run does not test every recipe, structure repetition or timed food spoilage.",
            "Full modpack server scripts and quests are not loaded; no 1.20 save conversion is provided."],
        "artifacts_sha256": {path.name: digest(output / path.name) for path in artifacts},
        "compiled_inputs_sha256": compiled,
        "compiled_probe_inputs_sha256": hashes(additions,
            ["build/classes/java/nativeProductionProbe", "build/resources/nativeProductionProbe"]),
        "production_source_sha256": source_hashes,
        "probe_source_sha256": hashes(additions, ["migration/production-tests", "gradle"]),
        "runner_source_sha256": digest(additions / "migration/run_native_dedicated.py"),
    }
    (output / "evidence.json").write_text(json.dumps(evidence, indent=2) + "\n")
    print(f"Archived PASS evidence: {output}; {len(artifacts)} artifacts")


if __name__ == "__main__":
    main()
