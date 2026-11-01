"""Build a local, exhaustive Figma/native review from instrumented PNGs.

Requires Pillow. References and renders remain in the ignored design cache.
The contact sheets are review aids, never application assets.
"""
import argparse
import html
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageChops, ImageFilter, ImageStat

ROOT=Path(__file__).resolve().parents[2]
CACHE=ROOT/'.design-cache/visual-review'
parser=argparse.ArgumentParser()
parser.add_argument('run',choices=['baseline','intermediate','final'])
args=parser.parse_args()
native=CACHE/args.run
output=CACHE/('sheets-'+args.run)
output.mkdir(exist_ok=True,parents=True)
frames=json.loads((ROOT/'.design-cache/frames.json').read_text(encoding='utf-8'))
by_name={frame['name']:frame for frame in frames}
font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',13)
small=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',11)
entries=[]
for light in sorted((f for f in frames if f['name'].endswith('/ Claro')),key=lambda f:f['name']):
    for dark in (False,True):
        frame=by_name[light['name'].removesuffix('Claro')+'Escuro'] if dark else light
        key=light['id'].replace(':','_')+('_dark' if dark else '_light')
        ref=CACHE/'references'/(frame['id'].replace(':','_')+'.png')
        actual=native/(key+'.png')
        if not actual.exists():continue
        a=Image.open(ref).convert('RGB').resize((412,917))
        b=Image.open(actual).convert('RGB').resize((412,917))
        diff=ImageChops.difference(a.filter(ImageFilter.GaussianBlur(4)),b.filter(ImageFilter.GaussianBlur(4))).crop((24,52,388,870))
        score=sum(ImageStat.Stat(diff).mean)/3
        entries.append({'id':frame['id'],'canonical':light['id'],'name':frame['name'],'key':key,'difference':round(score,2),'reference':ref.relative_to(CACHE).as_posix(),'native':actual.relative_to(CACHE).as_posix()})

for page,start in enumerate(range(0,len(entries),12),1):
    sheet=Image.new('RGB',(1272,2056),'#E2E8F0');draw=ImageDraw.Draw(sheet)
    for index,entry in enumerate(entries[start:start+12]):
        x=(index%3)*424;y=(index//3)*514
        title=entry['name'].replace('Cliente / ','')
        draw.text((x+4,y+3),title[:57],font=small,fill='#132B50')
        draw.text((x+4,y+19),entry['id']+' | Figma / Android',font=font,fill='#132B50')
        for column,path in enumerate((entry['reference'],entry['native'])):
            image=Image.open(CACHE/path).convert('RGB').resize((206,459),Image.Resampling.LANCZOS)
            sheet.paste(image,(x+4+column*208,y+45))
        entry['sheet']=page
    sheet.save(output/f'{page:02}.jpg',quality=92)

geometry_path=native/'geometry.json'
issues=[]
if geometry_path.exists():
    geometry=json.loads(geometry_path.read_text(encoding='utf-8'))
    for screen in geometry:
        for view in screen['views']:
            if not view.get('text') or view.get('editable'):continue
            for axis in ('Height','Width'):
                if view.get('needed'+axis,0)>view.get('available'+axis,0)+2:
                    issues.append({'screen':screen['screen'],'dark':screen['dark'],'axis':axis,**view})
(output/'index.json').write_text(json.dumps(entries,ensure_ascii=False,indent=2),encoding='utf-8')
(output/'text-clipping.json').write_text(json.dumps(issues,ensure_ascii=False,indent=2),encoding='utf-8')
cards=[]
for entry in entries:
    title=html.escape(entry['name'])
    cards.append(f'<article data-name="{title.lower()}"><h2>{title}</h2><p>{entry["id"]} · Figma / Android</p><div><img loading="lazy" src="../{entry["reference"]}"><img loading="lazy" src="../{entry["native"]}"></div></article>')
document='''<!doctype html><html lang="pt-BR"><meta charset="utf-8"><title>App Cliente — revisão integral</title><style>body{font:16px system-ui;background:#e2e8f0;color:#132b50;margin:24px}header{position:sticky;top:0;background:#e2e8f0;padding:12px;z-index:1}input{padding:12px;width:min(90%,600px)}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(420px,1fr));gap:24px}article{background:white;padding:12px;border-radius:16px}h2{font-size:17px}article div{display:flex;gap:4px}img{width:calc(50% - 2px);object-fit:contain;align-self:start}</style><header><h1>App Cliente — revisão integral</h1><input placeholder="Filtrar por tela, fluxo ou tema" oninput="document.querySelectorAll('article').forEach(e=>e.hidden=!e.dataset.name.includes(this.value.toLowerCase()))"><p>Esquerda: Figma. Direita: renderização nativa. Barras do sistema são gerenciadas pelo Android.</p></header><main>'''+''.join(cards)+'</main></html>'
(output/'index.html').write_text(document,encoding='utf-8')
print(json.dumps({'screens':len(entries),'sheets':(len(entries)+11)//12,'text_clipping':len(issues),'largest_differences':sorted(entries,key=lambda e:e['difference'],reverse=True)[:8]},ensure_ascii=False))
