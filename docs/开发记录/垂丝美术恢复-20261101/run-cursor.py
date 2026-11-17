"""Launch the authorized Cursor CLI and expose only its completed result."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--prompt-file', required=True)
parser.add_argument('--log-name', required=True)
parser.add_argument('--model', required=True, choices=['grok-4.7-high-fast'])
parser.add_argument('--chat-id', required=True)
parser.add_argument('--write-mode', action='store_true')
options = parser.parse_args()
if not re.fullmatch(r'[a-z0-9-]+', options.log_name):
    parser.error('Invalid log name')
if not re.fullmatch(r'[a-f0-9-]{36}', options.chat_id):
    parser.error('Invalid chat identifier')
task_path = Path(options.prompt_file).resolve(strict=True)
run_path = Path(__file__).resolve().parent
launcher_path = Path(r'C:\Users\baishang\AppData\Local\CursorAgentCodex\2026.10.22-27dcf5f\dist-package\cursor-agent.cmd')
task_prompt = f'Read the complete UTF-8 task specification at {task_path.as_posix()} and follow that authorized scope. Return the complete final report.'
arguments = [str(launcher_path), '--print', '--model', options.model,
             '--auto-review', '--workspace', r'D:\alianximod',
             '--output-format', 'stream-json']
if not options.write_mode:
    arguments.extend(['--mode', 'ask'])
arguments.extend(['--resume', options.chat_id, task_prompt])
if any(any(character in argument for character in '&|<>^%!"\r\n') for argument in arguments):
    parser.error('Unexpected shell metacharacter in fixed launch arguments')
output_path = run_path / (options.log_name + '.jsonl')
error_path = run_path / (options.log_name + '.stderr.log')
command_text = subprocess.list2cmdline(arguments)
with output_path.open('wb') as output_file, error_path.open('wb') as error_file:
    completed = subprocess.run(command_text, shell=True,
                               executable=os.path.join(os.environ['SystemRoot'], 'System32', 'cmd.exe'),
                               cwd=r'D:\alianximod', stdout=output_file, stderr=error_file)
(run_path / (options.log_name + '-exit.txt')).write_text(str(completed.returncode), encoding='utf-8')
final_event = None
with output_path.open(encoding='utf-8-sig', errors='replace') as output_file:
    for output_line in output_file:
        try:
            parsed_event = json.loads(output_line)
        except (json.JSONDecodeError, ValueError):
            continue
        if isinstance(parsed_event, dict) and parsed_event.get('type') == 'result':
            final_event = parsed_event
sys.stdout.reconfigure(encoding='utf-8')
if final_event is not None:
    final_text = final_event.get('result', '')
    if not isinstance(final_text, str):
        final_text = json.dumps(final_text, ensure_ascii=False)
    (run_path / (options.log_name + '-final.md')).write_text(final_text, encoding='utf-8')
    print(json.dumps(final_event, ensure_ascii=False))
else:
    print('No final result; exit ' + str(completed.returncode))
    print('\n'.join(error_path.read_text(encoding='utf-8-sig', errors='replace').splitlines()[-25:]))
raise SystemExit(completed.returncode)
