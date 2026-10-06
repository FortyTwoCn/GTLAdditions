#!/usr/bin/env python3
"""Run complete native production server/client JVMs, then verify an actual disk restart.

Prepare the two game directories with Core's source-verified pack registration/asset
profile first. Gradle exports its exact ModDevGradle commands after compiling all
sources; this runner never launches a competing Gradle build or edits mod outputs.
"""
import argparse
import json
import os
from pathlib import Path
import socket
import subprocess
import time


def text(path):
    return path.read_text(errors="replace") if path.is_file() else ""


def launch(definition, log, reload):
    command = definition["command"][:]
    if reload:
        command.insert(1, "-Dgtl.nativeProductionDedicatedReload=true")
    environment = os.environ.copy()
    environment.update(definition["environment"])
    with log.open("w") as output:
        return subprocess.Popen(command, cwd=definition["directory"], env=environment,
                                stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT, text=True)


def stop_server(process):
    if process.poll() is not None:
        return
    process.stdin.write("stop\n")
    process.stdin.flush()
    try:
        process.wait(timeout=45)
    except subprocess.TimeoutExpired:
        process.terminate()
        process.wait(timeout=10)
        raise RuntimeError("Dedicated server failed graceful save/stop")
    if process.returncode != 0:
        raise RuntimeError(f"Dedicated server exited with {process.returncode}")


def run_round(definitions, root, reload):
    name = "reload" if reload else "first"
    server_log = root / f"native-server-{name}.log"
    client_log = root / f"native-client-{name}.log"
    server = launch(definitions["runProductionDedicatedServer"], server_log, reload)
    client = None
    try:
        deadline = time.monotonic() + 600
        while "GTL_NATIVE_PRODUCTION_DEDICATED_READY" not in text(server_log):
            if server.poll() is not None:
                raise RuntimeError(f"Dedicated server failed startup; inspect {server_log}")
            if time.monotonic() >= deadline:
                raise RuntimeError(f"Dedicated server startup timed out; inspect {server_log}")
            time.sleep(1)
        print(f"Native dedicated {name}: server ready, starting independent client", flush=True)
        client = launch(definitions["runProductionDedicatedClient"], client_log, reload)
        deadline = time.monotonic() + 660
        while client.poll() is None:
            if server.poll() is not None:
                raise RuntimeError(f"Dedicated server stopped during client checks; inspect {server_log}")
            if time.monotonic() >= deadline:
                raise RuntimeError(f"Dedicated client timed out; inspect {client_log}")
            time.sleep(1)
        suffix = "_RELOAD" if reload else ""
        if client.returncode != 0 or f"GTL_NATIVE_PRODUCTION_DEDICATED_CLIENT{suffix}=PASS" not in text(client_log):
            raise RuntimeError(f"Dedicated client validation failed (exit {client.returncode}); inspect {client_log}")
        if f"GTL_NATIVE_PRODUCTION_DEDICATED_SERVER{suffix}=PASS" not in text(server_log):
            raise RuntimeError(f"Dedicated server did not confirm actual menu actions; inspect {server_log}")
        if text(client_log).count("GTL_NATIVE_PRODUCTION_DEDICATED_AE_CLIENT fluid=") != 2:
            raise RuntimeError(f"Dedicated client did not verify both exact native AE menus; inspect {client_log}")
        if not reload and text(server_log).count("wheel_decrement_exact=true") != 2:
            raise RuntimeError(f"Dedicated server did not receive both exact native AE wheel actions; inspect {server_log}")
        if "GTL_NATIVE_PRODUCTION_DEDICATED_TRAVEL_CLIENT" not in text(client_log) or "permit_consumed=true" not in text(server_log):
            raise RuntimeError(f"Native physical-player protected dimension transfer was not verified; inspect {client_log} and {server_log}")
        if "GTL_NATIVE_PRODUCTION_DEDICATED_SPOIL_CLIENT" not in text(client_log) or "GTL_NATIVE_PRODUCTION_DEDICATED_SPOIL_SERVER" not in text(server_log):
            raise RuntimeError(f"Native spoilable entity component did not cross the physical connection; inspect {client_log} and {server_log}")
        if "GTL_NATIVE_PRODUCTION_WIRELESS_CLIENT=PASS" not in text(client_log) or "GTL_NATIVE_PRODUCTION_WIRELESS_SERVER=PASS" not in text(server_log):
            raise RuntimeError(f"Native held wireless terminals and real-grid synchronization were not verified; inspect {client_log} and {server_log}")
        if "GTL_NATIVE_PRODUCTION_BLOOM=PASS" not in text(client_log):
            raise RuntimeError(f"Native bloom buffer lifecycle was not verified; inspect {client_log}")
        if "GTL_NATIVE_PRODUCTION_ROCKET_TRAVEL_CLIENT=PASS" not in text(client_log) or "GTL_NATIVE_PRODUCTION_ROCKET_TRAVEL_SERVER=PASS" not in text(server_log):
            raise RuntimeError(f"Native survival rocket flight and planet landing were not verified; inspect {client_log} and {server_log}")
        if "GTL_NATIVE_PRODUCTION_ELEVATOR_CLIENT=PASS" not in text(client_log) or "GTL_NATIVE_PRODUCTION_ELEVATOR_SERVER=PASS" not in text(server_log):
            raise RuntimeError(f"Native paid Space Elevator departure and planet landing were not verified; inspect {client_log} and {server_log}")
        stop_server(server)
        print(f"Native dedicated {name}: exact quantities, components and native GUI passed; world saved", flush=True)
    finally:
        if client is not None and client.poll() is None:
            client.terminate()
            client.wait(timeout=15)
        stop_server(server)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--launch", required=True, type=Path)
    args = parser.parse_args()
    definitions = json.loads(args.launch.read_text())
    server_dir = Path(definitions["runProductionDedicatedServer"]["directory"])
    client_dir = Path(definitions["runProductionDedicatedClient"]["directory"])
    for directory in (server_dir, client_dir):
        if not (directory / "native-profile-manifest.json").is_file():
            raise RuntimeError(f"Source-verified native pack registration profile is missing: {directory}")
    if not (server_dir / "native-machine-recipe-manifest.json").is_file():
        raise RuntimeError("Source-verified original Space Elevator recipe slice is missing")
    options = client_dir / "options.txt"
    settings = dict(line.split(":", 1) for line in text(options).splitlines() if ":" in line)
    settings.update(onboardAccessibility="false", guiScale="2", enableVsync="false", maxFps="30",
                    renderDistance="3", simulationDistance="5", soundCategory_master="0.0", tutorialStep="none")
    options.write_text("".join(f"{key}:{value}\n" for key, value in settings.items()), encoding="utf-8")
    with socket.socket() as probe:
        if probe.connect_ex(("127.0.0.1", 25576)) == 0:
            raise RuntimeError("Local validation port 25576 is already in use")
    properties = server_dir / "server.properties"
    marker = "# Native dedicated validation; isolated loopback profile."
    ownership = server_dir / ".native-dedicated-validation"
    if properties.is_file() and not ownership.is_file() and not text(properties).startswith(marker):
        raise RuntimeError(f"Refusing to overwrite a server profile not owned by this validation: {properties}")
    ownership.write_text(marker + "\n", encoding="utf-8")
    properties.write_text(marker + "\nserver-ip=127.0.0.1\nserver-port=25576\nonline-mode=false\n"
                          "enforce-secure-profile=false\nlevel-name=world\nlevel-type=minecraft:flat\n"
                          "generate-structures=false\ngamemode=creative\ndifficulty=peaceful\n"
                          "spawn-protection=0\nview-distance=3\nsimulation-distance=3\n", encoding="utf-8")
    output = args.launch.parent
    output.mkdir(parents=True, exist_ok=True)
    run_round(definitions, output, False)
    run_round(definitions, output, True)
    print("GTL_NATIVE_PRODUCTION_DEDICATED=PASS independent_jvms=true actual_tcp=true actual_disk_restart=true", flush=True)


if __name__ == "__main__":
    main()
