import datetime, hashlib, json, pathlib, subprocess, sys, urllib.request
ROOT = pathlib.Path("D:/alianximod")
HERE = pathlib.Path(__file__).resolve().parent
ENDPOINT = "http://127.0.0.1:3425/v1/chat/completions"
ROUTE = "group/smart-split"
PATCH_EXE = pathlib.Path("C:/Users/baishang/AppData/Local/OpenAI/Codex/bin/2993767bd1dfb009/codex.exe")
ALLOWED = {
 "src/main/java/com/bslx/XueXiMod/shu/balance/BalanceKeys.java",
 "src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainDefinition.java",
 "src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/motion/VolumeMotionRuntime.java",
 "docs/配置平衡指南.md",
}

def save(name, value):
    (HERE / name).write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding="utf-8")

def read_paths(paths):
    output = []
    for relative in paths:
        target = (ROOT / relative).resolve()
        if not target.is_relative_to(ROOT) or target.suffix not in {".java", ".md"}:
            output.append({"path": relative, "error": "outside read scope"})
            continue
        raw = target.read_bytes()
        output.append({"path": relative, "sha256": hashlib.sha256(raw).hexdigest(), "content": raw.decode("utf-8-sig")})
    return output

def main():
    attempt = "capacity-01"
    prompt = (HERE / "capacity-01.prompt.md").read_text(encoding="utf-8")
    history_path = HERE / "capacity-history.json"
    messages = json.loads(history_path.read_text(encoding="utf-8")) if history_path.exists() else []
    messages.append({"role": "user", "content": prompt})
    tools = [
        {"type": "function", "function": {"name": "read_files", "parameters": {"type": "object", "properties": {"paths": {"type": "array", "items": {"type": "string"}}}, "required": ["paths"]}}},
        {"type": "function", "function": {"name": "apply_patch", "parameters": {"type": "object", "properties": {"patch": {"type": "string"}, "expected_sha256": {"type": "string"}}, "required": ["patch", "expected_sha256"]}}},
        {"type": "function", "function": {"name": "name_gate", "parameters": {"type": "object", "properties": {}, "additionalProperties": False}}},
    ]
    receipt = {"batchId": "water-vortex-20261103", "taskId": "source-owner", "attemptId": attempt,
               "client": "Python urllib HTTP tool client", "endpoint": ENDPOINT, "requestedRoute": ROUTE,
               "clientCorrelationTag": "water-vortex-capacity-48-20261109", "providerSessionId": None,
               "stage": "implementation", "reviews": "design2/2 implementation3/3 exhausted; approved numeric repair only",
               "state": "running", "startedAt": datetime.datetime.now().astimezone().isoformat()}
    save(attempt + ".receipt.json", receipt)
    changes = []
    try:
        for turn in range(12):
            body = {"model": ROUTE, "messages": messages, "tools": tools, "stream": False}
            save(f"{attempt}.request-{turn}.json", body)
            request = urllib.request.Request(ENDPOINT, data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
                headers={"Content-Type": "application/json", "X-Magpie-Session": receipt["clientCorrelationTag"]}, method="POST")
            with urllib.request.urlopen(request, timeout=300) as response:
                result = json.loads(response.read())
                receipt["responseHeaders"] = {key: value for key, value in response.headers.items()
                    if key.lower().startswith("x-magpie") or key.lower() in {"x-request-id", "request-id"}}
            save(f"{attempt}.response-{turn}.json", result)
            message = result["choices"][0]["message"]
            messages.append(message)
            if message.get("tool_calls"):
                for call in message["tool_calls"]:
                    args = json.loads(call["function"]["arguments"] or "{}")
                    name = call["function"]["name"]
                    if name == "read_files":
                        value = read_paths(args["paths"])
                    elif name == "apply_patch":
                        patch = args["patch"]
                        headers = [line[len("*** Update File: "):] for line in patch.splitlines() if line.startswith("*** Update File: ")]
                        if len(headers) != 1 or headers[0] not in ALLOWED or "*** Delete File:" in patch or "*** Move to:" in patch:
                            value = {"error": "patch outside the four approved files"}
                        else:
                            target = ROOT / headers[0]
                            before = target.read_bytes()
                            actual = hashlib.sha256(before).hexdigest()
                            if actual.lower() != args["expected_sha256"].lower():
                                value = {"error": "hash changed", "actual": actual}
                            else:
                                (HERE / f"{attempt}.patch-{turn}.txt").write_text(patch, encoding="utf-8")
                                applied = subprocess.run([str(PATCH_EXE), "--codex-run-as-apply-patch", patch], cwd=ROOT,
                                    capture_output=True, text=True, encoding="utf-8", errors="replace")
                                value = {"exitCode": applied.returncode, "stdout": applied.stdout[-2000:], "stderr": applied.stderr[-1000:],
                                         "sha256": hashlib.sha256(target.read_bytes()).hexdigest()}
                                if applied.returncode == 0 and target.read_bytes() != before:
                                    changes.append(headers[0])
                    elif name == "name_gate":
                        gate = subprocess.run(["powershell", "-NoProfile", "-File", "tools/check-name-gate.ps1"], cwd=ROOT,
                            capture_output=True, text=True, encoding="utf-8", errors="replace")
                        value = {"exitCode": gate.returncode, "stdout": gate.stdout[-2000:], "stderr": gate.stderr[-1000:]}
                    else:
                        value = {"error": "unknown tool"}
                    messages.append({"role": "tool", "tool_call_id": call["id"], "content": json.dumps(value, ensure_ascii=False)})
                save("capacity-history.json", messages)
                continue
            if result["choices"][0].get("finish_reason") != "stop" or not message.get("content"):
                raise ValueError("incomplete response")
            (HERE / f"{attempt}.final.md").write_text(message["content"], encoding="utf-8")
            receipt.update({"state": "finished", "responseId": result.get("id"), "returnedModel": result.get("model"),
                            "writesSettled": True, "sourceChanges": sorted(set(changes)),
                            "final": str(HERE / f"{attempt}.final.md")})
            break
        else:
            raise RuntimeError("turn limit")
    except Exception as error:
        receipt.update({"state": "failed", "errorType": type(error).__name__, "error": str(error)})
        save("capacity-history.json", messages)
    receipt["finishedAt"] = datetime.datetime.now().astimezone().isoformat()
    save(attempt + ".receipt.json", receipt)
    print(json.dumps({"state": receipt["state"], "model": receipt.get("returnedModel"), "changes": receipt.get("sourceChanges"), "error": receipt.get("error")}, ensure_ascii=False))

if __name__ == "__main__":
    main()
