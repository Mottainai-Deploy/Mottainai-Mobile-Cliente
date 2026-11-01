"""Download the exact assets provided by Figma; curl is the prescribed downloader."""
import concurrent.futures
import json
import subprocess
from pathlib import Path

root=Path(__file__).resolve().parents[2]
assets=json.loads((root/'.design-cache/assets.json').read_text())
def download(item):
    key,url=item
    path=root/'app/src/main/assets'/key
    path.parent.mkdir(parents=True,exist_ok=True)
    if path.exists() and path.stat().st_size:
        return
    subprocess.run(['curl.exe','-f','-sS','-L','--retry','2','--max-time','90',url,'-o',str(path)],check=True)
    if not path.stat().st_size:
        raise RuntimeError(f'Empty asset: {key}')
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
    list(pool.map(download,assets.items()))
print(f'{len(assets)} Figma assets verified locally')
