#!/usr/bin/env python3
"""Stage assembled native production mods and record every artifact hash.

Input comes from Gradle exportNativeRuntime. Embedded mods remain in their
owning JAR. Existing different files are preserved and cause a failure.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import shutil
import tomllib
import zipfile


def digest(data):
    return hashlib.sha256(data).hexdigest()


def inspect(data, name):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        names = archive.namelist()
        if "META-INF/mods.toml" in names and "META-INF/neoforge.mods.toml" not in names:
            raise ValueError(f"Legacy Forge mod in production distribution: {name}")
        if any(value.startswith("com/gtladd/gtladditions/migration/") for value in names):
            raise ValueError(f"Production probe classes in distribution: {name}")
        metadata = tomllib.loads(archive.read("META-INF/neoforge.mods.toml").decode()) \
            if "META-INF/neoforge.mods.toml" in names else {}
        mods = [value["modId"] for value in metadata.get("mods", [])]
        embedded = []
        if "META-INF/jarjar/metadata.json" in names:
            for entry in json.loads(archive.read("META-INF/jarjar/metadata.json"))["jars"]:
                nested = archive.read(entry["path"])
                nested_mods, nested_entries = inspect(nested, name + "!" + entry["path"])
                embedded.append({"path": entry["path"], "sha256": digest(nested),
                                 "mod_ids": nested_mods, "identifier": entry["identifier"],
                                 "version": entry["version"]["artifactVersion"]})
                embedded.extend(nested_entries)
        return mods, embedded


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runtime", type=Path, required=True)
    parser.add_argument("--directory", type=Path, required=True)
    args = parser.parse_args()
    runtime = json.loads(args.runtime.read_text())
    if runtime["schema"] != 1 or runtime["production_tests"]:
        raise ValueError("Expected the production-only native runtime export")
    candidates, omitted, seen_paths = [], [], set()
    for entry in runtime["artifacts"]:
        path = Path(entry["path"]).resolve()
        if path in seen_paths:
            continue
        seen_paths.add(path)
        if not path.is_file() or path.suffix != ".jar":
            raise ValueError(f"Unassembled production artifact: {path}")
        data = path.read_bytes()
        mod_ids, embedded = inspect(data, str(path))
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            manifest = archive.read("META-INF/MANIFEST.MF").decode() \
                if "META-INF/MANIFEST.MF" in archive.namelist() else ""
            native_type = next((line.split(":", 1)[1].strip() for line in manifest.splitlines()
                                if line.startswith("FMLModType:")), None)
            language_provider = "META-INF/services/net.neoforged.neoforgespi.language.IModLanguageLoader" \
                in archive.namelist()
        record = {"file": path.name, "component": entry["component"], "sha256": digest(data),
                  "bytes": len(data), "mod_ids": mod_ids, "embedded": embedded,
                  "native_library_type": native_type, "language_provider": language_provider}
        # Native libraries and language providers are discovered by their manifest/service,
        # not a mod ID. KotlinForForge's published runtime needs all three KFF components
        # as well as its library aggregator; the aggregator contains only Kotlin libraries.
        native_library = native_type in {"LIBRARY", "GAMELIBRARY", "LANGPROVIDER"}
        installer_component = entry["component"].startswith("net.neoforged.fancymodloader:")
        if installer_component or (not mod_ids and not embedded and not native_library) \
                or set(mod_ids) & {"minecraft", "neoforge"}:
            omitted.append({**record, "reason": "runtime library or game/loader supplied by the NeoForge installer"})
        else:
            candidates.append((path, record))
    embedded_hashes = {nested["sha256"] for _, record in candidates for nested in record["embedded"]}
    selected, mod_owners, nested_owners = [], {}, {}
    for path, record in candidates:
        if record["sha256"] in embedded_hashes:
            omitted.append({**record, "reason": "identical mod already embedded in a production dependency"})
            continue
        for mod_id in record["mod_ids"]:
            if mod_id in mod_owners:
                raise ValueError(f"Duplicate mod ID {mod_id}: {mod_owners[mod_id]} and {path}")
            mod_owners[mod_id] = path.name
        selected.append((path, record))
    for path, record in selected:
        for nested in record["embedded"]:
            for mod_id in nested["mod_ids"]:
                if mod_id in mod_owners and mod_id not in nested_owners:
                    raise ValueError(f"Top-level and embedded mod ID conflict {mod_id}: {mod_owners[mod_id]} and {path}")
                if mod_id in nested_owners:
                    previous = nested_owners[mod_id]
                    # Native JarJar selects one dependency with the same published identity/version.
                    # Different owners may have separately built JAR bytes for that native release.
                    if previous["identifier"] != nested["identifier"] or previous["version"] != nested["version"]:
                        raise ValueError(f"Conflicting embedded mod ID {mod_id}: {previous} and {nested}")
                else:
                    nested_owners[mod_id] = nested
                    mod_owners[mod_id] = path.name + "!" + nested["path"]
    if not {"gtlcore", "gtladditions", "gtceu"}.issubset(mod_owners):
        raise ValueError("Distribution is missing a complete GTLCore, GTLAdditions or GTCEu production JAR")
    destination = args.directory.resolve()
    mods_directory = destination / "mods"
    mods_directory.mkdir(parents=True, exist_ok=True)
    expected = {record["file"] for _, record in selected}
    if unexpected := {value.name for value in mods_directory.glob("*.jar")} - expected:
        raise ValueError(f"Existing unrelated mods preserved: {sorted(unexpected)}")
    for path, record in selected:
        target = mods_directory / record["file"]
        if target.exists() and digest(target.read_bytes()) != record["sha256"]:
            raise ValueError(f"Existing different artifact preserved: {target}")
        if not target.exists():
            shutil.copy2(path, target)
    manifest = {"schema": 1, "minecraft": runtime["minecraft"], "neoforge": runtime["neoforge"],
                "production_probe_present": False, "mods": [record for _, record in selected],
                "omitted": omitted}
    (destination / "native-artifacts.json").write_text(json.dumps(manifest, indent=2) + "\n")
    print(json.dumps({"result": "PASS", "directory": str(destination), "top_level_mod_jars": len(selected),
                      "production_probe_present": False, "mod_ids": sorted(mod_owners)}, indent=2))


if __name__ == "__main__":
    main()
