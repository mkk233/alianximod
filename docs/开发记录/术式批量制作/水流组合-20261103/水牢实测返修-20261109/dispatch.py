import datetime
import hashlib
import json
import os
import pathlib
import subprocess
import sys
import urllib.request

ROOT = pathlib.Path("D:/alianximod")
HERE = pathlib.Path(__file__).resolve().parent
ENDPOINT = "http://127.0.0.1:3425/v1/chat/completions"
ROUTE = "group/smart-split"
SOURCE_TARGET = "src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/motion/VolumeMotionRuntime.java"
PATCH_EXE = pathlib.Path("C:/Users/baishang/AppData/Local/OpenAI/Codex/bin/2993767bd1dfb009/codex.exe")


def save(name, value):
    (HERE / name).write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding="utf-8")


def read_paths(paths):
    output = []
    for relative in paths:
        target = (ROOT / relative).resolve()
        if not target.is_relative_to(ROOT) or target.suffix not in {".java", ".md", ".json", ".log", ".txt", ".toml"}:
            raise ValueError("outside read scope")
        if not target.is_file():
            output.append({"path": relative, "missing": True})
            continue
        raw = target.read_bytes()
        output.append({"path": relative, "sha256": hashlib.sha256(raw).hexdigest(),
                       "content": raw.decode("utf-8-sig")})
    return output


def source_snapshot():
    paths = list((ROOT / "src/main").rglob("*"))
    paths.extend(ROOT / name for name in ["build.gradle", "settings.gradle", "gradle.properties", "gradlew.bat"])
    return {str(path.relative_to(ROOT)).replace("\\", "/"): hashlib.sha256(path.read_bytes()).hexdigest()
            for path in paths if path.is_file()}


def compile_project(attempt):
    before = source_snapshot()
    save(attempt + ".before.json", before)
    environment = os.environ.copy()
    environment["JAVA_HOME"] = "C:/Program Files/Java/jdk-17"
    environment["GRADLE_USER_HOME"] = "C:/Users/baishang/.gradle"
    started = datetime.datetime.now().astimezone().isoformat()
    command = ["cmd.exe", "/d", "/c", "gradlew.bat", "compileJava", "--offline", "--no-daemon"]
    with (HERE / (attempt + ".stdout.log")).open("wb") as stdout, (HERE / (attempt + ".stderr.log")).open("wb") as stderr:
        process = subprocess.run(command, cwd=ROOT, env=environment, stdout=stdout, stderr=stderr, timeout=240)
    after = source_snapshot()
    save(attempt + ".after.json", after)
    changed = sorted(path for path in set(before) | set(after) if before.get(path) != after.get(path))
    result = {"command": ".\\gradlew.bat compileJava --offline --no-daemon",
              "startedAt": started, "finishedAt": datetime.datetime.now().astimezone().isoformat(),
              "exitCode": process.returncode, "inputCount": len(before), "changedInputs": changed,
              "stdoutTail": (HERE / (attempt + ".stdout.log")).read_text(encoding="utf-8", errors="replace")[-6500:],
              "stderrTail": (HERE / (attempt + ".stderr.log")).read_text(encoding="utf-8", errors="replace")[-3500:]}
    save(attempt + ".compile.json", result)
    return result


def main():
    attempt = sys.argv[1]
    prompt = (HERE / (attempt + ".prompt.md")).read_text(encoding="utf-8")
    history_name = "compile-history.json" if attempt.startswith("compile-") else "history.json"
    history_path = HERE / history_name
    messages = json.loads(history_path.read_text(encoding="utf-8")) if history_path.exists() else []
    requested_ids = {call["id"] for message in messages for call in message.get("tool_calls", [])}
    answered_ids = {message.get("tool_call_id") for message in messages if message.get("role") == "tool"}
    for pending_id in requested_ids - answered_ids:
        messages.append({"role": "tool", "tool_call_id": pending_id, "content": json.dumps({"error": "Previous local dispatch interrupted; missing files and disallowed reads now return tool errors. Retry only needed operations."})})
    messages.append({"role": "user", "content": prompt})
    tools = [
        {"type": "function", "function": {"name": "read_files", "description": "Read specific project source/doc files, returns original content and hash.",
         "parameters": {"type": "object", "properties": {"paths": {"type": "array", "items": {"type": "string"}}}, "required": ["paths"]}}},
        {"type": "function", "function": {"name": "search", "description": "Read-only ripgrep search in src/main/java or docs. Output limited to 24000 chars.",
         "parameters": {"type": "object", "properties": {"pattern": {"type": "string"}, "scope": {"type": "string"}, "glob": {"type": "string"}}, "required": ["pattern", "scope", "glob"]}}},
        {"type": "function", "function": {"name": "list_files", "description": "List project filenames by glob, no contents.",
         "parameters": {"type": "object", "properties": {"scope": {"type": "string"}, "glob": {"type": "string"}}, "required": ["scope", "glob"]}}},
    ]
    implementation = attempt.startswith("fix-")
    compilation = attempt.startswith("compile-")
    if implementation:
        tools.append({"type": "function", "function": {"name": "apply_patch",
            "description": "Apply an authored patch using the native Codex apply_patch tool backend. Only VolumeMotionRuntime.java allowed; old hash must match; no delete/rename.",
            "parameters": {"type": "object", "properties": {"patch": {"type": "string"}, "expected_sha256": {"type": "string"}}, "required": ["patch", "expected_sha256"]}}})
    if compilation:
        tools.append({"type": "function", "function": {"name": "compile_java",
            "description": "Execute the authorized final offline compileJava once on settled source. Captures input hashes, command, exit code and logs. No source writes.",
            "parameters": {"type": "object", "properties": {}, "additionalProperties": False}}})
    receipt = {"batchId": "water-vortex-20261103", "taskId": "compile-final" if compilation else "water-prison-target-repair", "attemptId": attempt,
               "client": "Python urllib HTTP tool client", "endpoint": ENDPOINT, "requestedRoute": ROUTE,
               "clientCorrelationTag": "water-prison-wet4-20261109", "providerSessionId": None,
               "priorClientSession": "fb3f3a66-3dd6-4b31-98f5-e96803986e77" if compilation else "none-new-local-context",
               "continuation": "Cross-client handoff; prior task reports and current user feedback supplied, full HTTP messages retained. IDs not interchangeable.",
               "stage": "implementation", "reviews": "design2/2 implementation3/3 exhausted; necessary bug diagnosis only, no new formal review",
               "sourceWriteAllowed": implementation, "compileAllowed": compilation, "state": "running",
               "startedAt": datetime.datetime.now().astimezone().isoformat()}
    save(attempt + ".receipt.json", receipt)
    changes = []
    compiled = False
    try:
        for turn in range(18):
            request_body = {"model": ROUTE, "messages": messages, "tools": tools, "stream": False}
            save(attempt + f".request-{turn}.json", request_body)
            request = urllib.request.Request(ENDPOINT, data=json.dumps(request_body, ensure_ascii=False).encode("utf-8"),
                headers={"Content-Type": "application/json", "X-Magpie-Session": receipt["clientCorrelationTag"]}, method="POST")
            with urllib.request.urlopen(request, timeout=300) as response:
                result = json.loads(response.read())
                receipt["responseHeaders"] = {key: value for key, value in response.headers.items()
                    if key.lower().startswith("x-magpie") or key.lower() in {"x-request-id", "request-id"}}
            save(attempt + f".response-{turn}.json", result)
            choice = result["choices"][0]
            message = choice["message"]
            messages.append(message)
            if message.get("tool_calls"):
                for call in message["tool_calls"]:
                    args = json.loads(call["function"]["arguments"])
                    if call["function"]["name"] == "read_files":
                        value = read_paths(args["paths"])
                    elif call["function"]["name"] == "search":
                        scope = (ROOT / args["scope"]).resolve()
                        if not any(scope.is_relative_to(ROOT / allowed) for allowed in ["src/main/java", "docs", "run/config", "run/saves", "run/defaultconfigs", "run/logs", "tools"]):
                            value = {"error": "outside allowed read scope"}
                        else:
                            search = subprocess.run(["rg", "-n", "-g", args["glob"], "--", args["pattern"], str(scope)],
                                                    capture_output=True, text=True, encoding="utf-8", errors="replace")
                            value = {"exitCode": search.returncode, "output": search.stdout[:24000], "stderr": search.stderr[:1000]}
                    elif call["function"]["name"] == "list_files":
                        scope = (ROOT / args["scope"]).resolve()
                        if not scope.is_relative_to(ROOT):
                            raise ValueError("outside listing scope")
                        listing = subprocess.run(["rg", "--files", "--hidden", "-g", args["glob"], str(scope)],
                                                 capture_output=True, text=True, encoding="utf-8", errors="replace")
                        value = {"output": listing.stdout[:24000], "exitCode": listing.returncode}
                    elif call["function"]["name"] == "apply_patch" and implementation:
                        patch = args["patch"]
                        headers = [line for line in patch.splitlines() if line.startswith("*** ") and "File:" in line]
                        if headers != ["*** Update File: " + SOURCE_TARGET] or "*** Move to:" in patch:
                            raise ValueError("patch exceeds sole source ownership")
                        target = ROOT / SOURCE_TARGET
                        before = target.read_bytes()
                        if hashlib.sha256(before).hexdigest().lower() != args["expected_sha256"].lower():
                            value = {"error": "source hash changed; re-read and reconcile, no overwrite"}
                        else:
                            (HERE / (attempt + f".before-{turn}.java")).write_bytes(before)
                            (HERE / (attempt + f".patch-{turn}.txt")).write_text(patch, encoding="utf-8")
                            applied = subprocess.run([str(PATCH_EXE), "--codex-run-as-apply-patch", patch], cwd=ROOT,
                                                     capture_output=True, text=True, encoding="utf-8", errors="replace")
                            value = {"exitCode": applied.returncode, "stdout": applied.stdout, "stderr": applied.stderr,
                                     "sha256": hashlib.sha256(target.read_bytes()).hexdigest()}
                            if target.read_bytes() != before:
                                changes.append(SOURCE_TARGET)
                    elif call["function"]["name"] == "compile_java" and compilation and not compiled:
                        value = compile_project(attempt)
                        compiled = True
                    else:
                        raise ValueError("unknown tool")
                    messages.append({"role": "tool", "tool_call_id": call["id"], "content": json.dumps(value, ensure_ascii=False)})
                save(history_name, messages)
                continue
            if choice.get("finish_reason") != "stop" or not message.get("content"):
                raise ValueError("incomplete response")
            (HERE / (attempt + ".final.md")).write_text(message["content"], encoding="utf-8")
            receipt.update({"state": "finished", "responseId": result.get("id"), "returnedModel": result.get("model"),
                            "writesSettled": True, "sourceChanges": sorted(set(changes)), "compileRun": compiled,
                            "final": str(HERE / (attempt + ".final.md"))})
            save(history_name, messages)
            break
        else:
            raise RuntimeError("bounded call limit; continue only after receipt")
    except Exception as error:
        receipt.update({"state": "failed", "errorType": type(error).__name__, "error": str(error)})
        if hasattr(error, "read"):
            (HERE / (attempt + ".error.txt")).write_bytes(error.read())
        save(history_name, messages)
    receipt["finishedAt"] = datetime.datetime.now().astimezone().isoformat()
    save(attempt + ".receipt.json", receipt)
    print(json.dumps({"state": receipt["state"], "receipt": str(HERE / (attempt + ".receipt.json"))}))


if __name__ == "__main__":
    main()


