"""Supplement MCP references with browser-reviewed layouts and cached geometry.

Shared controls and original images are composed from the previously retrieved
design library. Text corrections are recorded separately, after reading the
actual frames in the browser. This does not fetch private browser state.
"""
import copy
import json
import re
import statistics
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
CACHE=ROOT/'.design-cache'
trees=json.loads((CACHE/'trees.json').read_text(encoding='utf-8'))
frames=json.loads((CACHE/'frames.json').read_text(encoding='utf-8'))
metadata=ET.parse(CACHE/'client-metadata.xml').getroot()
elements={el.get('id'):el for el in metadata.iter() if el.get('id')}
overrides=json.loads((ROOT/'tools/design-import/reference-overrides.json').read_text(encoding='utf-8'))
reviewed=set(overrides.get('reviewed',[]))
library={}
text_styles=[]
all_nodes=[]

def walk(node):
    yield node
    for child in node.get('children',[]):
        if isinstance(child,dict):
            yield from walk(child)

def label(node):
    return ' '.join(c if isinstance(c,str) else label(c) for c in node.get('children',[])).strip()

for frame in frames:
    tree=trees.get(frame['id'])
    if not tree: continue
    dark=frame['name'].endswith('Escuro')
    for node in walk(tree):
        all_nodes.append((dark,node))
        attrs=node.get('attrs',{})
        name=attrs.get('data-name','')
        if name: library.setdefault((dark,name),node)
        meta=elements.get(attrs.get('data-node-id',''))
        if node['type']=='p' and meta is not None:
            text_styles.append((dark,float(meta.get('height',20)),float(meta.get('width',364)),len(label(node)),attrs.get('className','')))

def node(kind,name,cls,child_nodes=None,id=''):
    return {'type':kind,'attrs':{'data-name':name,'data-node-id':id,'className':cls},'children':child_nodes or []}

def with_meta(template,el):
    result=copy.deepcopy(template)
    if el.get('name')=='Cliente / Avatar da Lô':
        cls=result['attrs'].get('className','')
        match=re.search(r'(?:size|w)-\[(?:var\([^,]+,)?([\d.]+)px',cls)
        old=float(match[1]) if match else 96
        factor=float(el.get('width',36))/old
        for part in walk(result):
            a=part.setdefault('attrs',{})
            a['className']=re.sub(r'(-?\d+(?:\.\d+)?)px',lambda m:f'{float(m[1])*factor:g}px',a.get('className',''))
    result['attrs']['data-node-id']=el.get('id')
    result['attrs']['data-name']=el.get('name','')
    return result

def image_for(dark,purpose):
    if purpose=='brand':
        candidate=library.get((dark,'Marca original completa / vetor 1786:3433'))
    elif purpose=='food':
        candidate=library.get((dark,'Ilustração editável / food'))
    elif purpose=='tsuru':
        candidate=library.get((dark,'Tsuru / Vetor original das camisetas'))
    elif purpose=='avatar':
        candidate=library.get((dark,'Cliente / Avatar da Lô'))
    else:
        candidate=library.get((dark,'Ícone / '+purpose))
    if candidate: return candidate
    if purpose=='mail':
        source=trees.get('2143:12063' if dark else '2143:10166')
        if source:
            content=next((n for n in walk(source) if n.get('attrs',{}).get('data-name')=='Conteúdo / Rolagem'),None)
            if content:
                return next((n for n in walk(content) if any(isinstance(c,dict) and c['type']=='img' for c in n.get('children',[]))),None)
    return None

def text_value(el):
    original=el.get('name','')
    value=overrides.get('texts',{}).get(el.get('id'),overrides.get('prefixes',{}).get(original,original))
    if value.startswith('Título / '): value=value.split(' / ',1)[1]
    return value

def text_class(el,dark,parent):
    value=text_value(el)
    h=float(el.get('height',20));w=float(el.get('width',364))
    if value=='320' and h>=50:
        return f'relative font-bold text-[40px] text-[{"#F9F1E7" if dark else "#132B50"}] leading-[1.4] w-[{w}px]'
    if value.isupper() and parent.startswith('Prévia ilustrativa'):
        return f'relative font-bold text-[10px] text-[{"#FF984F" if dark else "#0B5EA8"}] leading-[1.5] w-[{w}px]'
    if parent=='Conteúdo / Principal' and h in (48,96) and len(value)>50:
        return f'relative text-[16px] text-[{"#B5C5DA" if dark else "#596A80"}] leading-[1.5] w-[{w}px]'
    if parent=='Lô / Identificação':
        return f'relative text-[{23 if h>25 else 13}px] '+('font-bold ' if h>25 else 'font-normal ')+f'leading-[1.35] text-[{"#F9F1E7" if dark else "#132B50"}] w-[{w}px]'
    if parent=='Preço da oferta':
        return f'relative text-[{20 if h>20 else 12}px] '+('font-bold ' if h>20 else 'font-normal ')+f'leading-[1.4] text-[{"#F9F1E7" if dark else "#132B50"}] w-[{w}px]'
    candidates=[style for mode,sh,sw,sl,style in text_styles if mode==dark and abs(sh-h)<1.1 and abs(sw-w)<30 and abs(sl-len(value))<45]
    if candidates:
        base=max(set(candidates),key=candidates.count)
        # Preserve typography and colors, while the native flow adapts width/height.
        base=re.sub(r'\b(?:w|h|left|top)-\[[^\]]+\]','',base)
        return base.replace('line-through','')+f' w-[{w}px]'
    size=15;bold=False
    if h>=70 and len(value)<70: size=30;bold=True
    elif 34<=h<=40 and len(value)<65: size=28 if h>=37 else 26;bold=True
    elif 29<=h<=33 and len(value)<60: size=23;bold=True
    elif h>=40 and len(value)>65: size=15
    elif h<=18: size=12
    elif h<=22: size=15
    elif h<=26: size=18;bold=parent=='Cabeçalho' or value.isupper()
    color=('#F9F1E7' if dark else '#132B50') if bold or parent=='Cabeçalho' else ('#B5C5DA' if dark else '#596A80')
    return f'relative font-["DM_Sans:{"Bold" if bold else "Regular"}"] {"font-bold" if bold else "font-normal"} text-[{size}px] text-[{color}] leading-[1.4] w-[{w}px]'

def convert(el,dark,screen,parent='',button_index=0):
    nm=el.get('name','');w=float(el.get('width',364));h=float(el.get('height',20))
    base=f'relative w-[{w:g}px]'
    ident=el.get('id','')
    if el.tag=='text' and '/ Tirar foto /' in screen:
        size=23 if nm=='Tirar foto' else (15 if h>20 else 12)
        return node('p','',f'{base} text-[{size}px] text-[#FFFFFF] leading-[1.4] '+('font-bold' if size==23 else 'text-center'),[text_value(el)],ident)
    if el.tag=='text':
        return node('p','',text_class(el,dark,parent),[text_value(el)],ident)
    if el.tag=='instance':
        if nm=='Logo original Mottainai':
            return with_meta(library[(dark,'Maré / Marca original / Completa')],el)
        if nm.startswith('Navbar persistente'):
            template=next(n for mode,n in all_nodes if mode==dark and n.get('attrs',{}).get('data-name','').startswith('Navbar persistente') and ('Conversa com' in screen or 'bg-[' in n['children'][-1].get('attrs',{}).get('className','')))
            return with_meta(template,el)
        if nm=='Cliente / Avatar da Lô':
            template=image_for(dark,'avatar')
            if template: return with_meta(template,el)
        if nm.startswith('Tsuru /'):
            template=image_for(dark,'tsuru')
            if template:
                result=with_meta(template,el)
                result['attrs']['className']=base+f' h-[{h:g}px]'
                return result
        if nm.startswith('Campo /'):
            field=nm.split(' / ',1)[1]
            template=next(n for mode,n in all_nodes if mode==dark and any(isinstance(c,dict) and c.get('attrs',{}).get('data-name')=='Entrada' for c in n.get('children',[])))
            result=with_meta(template,el)
            current_label=field
            if field=='Número' and 'Endereço preenchido' in screen: current_label='Número (informado por você)'
            result['children'][0]['children']=[current_label]
            samples={'CEP':'01001-000','Rua':'Praça da Sé','Número':'100','Complemento (opcional)':'Apartamento, bloco... (opcional)','Bairro':'Sé','Cidade':'São Paulo','UF':'SP','Senha atual':'••••••••'}
            sample=overrides.get('fields',{}).get(ident,samples.get(field,''))
            result['children'][1]['children'][0]['children']=[sample]
            if 'Endereço preenchido' in screen and field!='Complemento (opcional)':result['children'][1]['attrs']['data-initial']=sample
            return result
        if nm.startswith('Ação /'):
            title=overrides.get('buttons',{}).get(ident,nm.split(' / ',1)[1])
            primary=(h>=55 and button_index==0) or title=='Nova conversa'
            text_color=('#09142A' if dark else '#FFFFFF') if primary else ('#FF984F' if dark else '#0B5EA8')
            style=base+f' h-[{h:g}px] flex items-center justify-center px-[16px] cursor-pointer'
            if primary: style+=f' bg-[{"#FF984F" if dark else "#102654"}] rounded-[14px]'
            elif h>=55: style+=f' border border-[{"#2B4162" if dark else "#E8E0D8"}] rounded-[14px]'
            child=node('p','',f'w-full font-["DM_Sans:{"Bold" if h>=55 or primary else "Medium"}"] {"font-bold" if h>=55 or primary else "font-medium"} text-[15px] leading-[1.4] text-[{text_color}] text-center',[title])
            return node('button',nm,style,[child],ident)
    visible=[c for c in el if c.get('hidden')!='true']
    if not visible:
        if nm.startswith('Fundo /'):return node('div',nm,base+f' h-[{h:g}px] bg-[rgba(0,0,0,0.4)]',[],ident)
        if (dark,nm) in library and nm not in ('Frame','Alternar tema'):
            result=with_meta(library[(dark,nm)],el)
            result['attrs']['className']=base+f' h-[{h:g}px]'
            return result
        purpose='back' if parent.startswith('Voltar') or nm.startswith('Voltar') else ('moon' if nm=='Alternar tema' else '')
        if 'food' in nm: purpose='food'
        if nm=='Frame' and 'E-mail' in screen: purpose='mail'
        if nm=='Frame' and 'Localização' in parent: purpose='pin'
        if nm=='Frame' and parent.startswith('Selo /'):
            purpose='pin' if 'região' in parent else ('bag' if 'Ofertas' in parent else 'check')
        if nm=='Frame' and parent in ('Fechar histórico','Cancelar captura'): purpose='close'
        if '/ Tirar foto /' in screen:
            if nm=='Guia de enquadramento':purpose='camera-guide'
            if nm=='Capturar foto':purpose='camera-shutter'
            if nm=='Frame' and parent=='Câmera / Prévia ilustrativa':return node('div',nm,base+f' h-[{h:g}px]',[],ident)
        if nm=='Frame' and parent=='Abrir conversa': purpose='chevron' if w<18 else 'chat'
        if purpose in ('bag','check','chat','close','chevron','camera-guide','camera-shutter'):
            return {'type':'img','attrs':{'data-name':nm,'data-node-id':ident,'className':base+f' h-[{h:g}px]','src':'local/'+purpose+('_camera' if purpose=='close' and '/ Tirar foto /' in screen else ('_dark' if dark and purpose not in ('camera-guide','camera-shutter') else ''))+'.svg'},'children':[]}
        if nm=='Frame' and 'Acesso' in screen: purpose='pin'
        if nm=='Frame' and 'Avisos / Permissão' in screen: purpose='chat'
        image=image_for(dark,purpose) if purpose else None
        if image:
            result=with_meta(image,el);result['attrs']['className']=base+f' h-[{h:g}px]';return result
        if el.tag in ('ellipse','rounded-rectangle'):
            fill=('#FF984F' if dark else '#102654') if 'Atual' in nm else ('#2B4162' if dark else '#D9E3EF')
            return node('div',nm,base+f' h-[{h:g}px] bg-[{fill}] rounded-[{h/2:g}px]',[],ident)
        return node('div',nm,base+f' h-[{h:g}px]',[],ident)
    xs=[float(c.get('x',0)) for c in visible];ys=[float(c.get('y',0)) for c in visible]
    vertical=all(ys[i]>=ys[i-1]+float(visible[i-1].get('height',0))-2 for i in range(1,len(visible)))
    horizontal=not vertical and all(xs[i]>=xs[i-1]+float(visible[i-1].get('width',0))-2 for i in range(1,len(visible)))
    illustrated=('Cena ilustrada' in nm or 'Prévia ilustrativa' in nm)
    absolute=not vertical and not horizontal
    style=base+' content-stretch flex '+('flex-col items-start' if vertical else 'items-center')
    padx=min(xs) if xs and min(xs)>0 else 0
    pady=min(ys) if ys and min(ys)>0 else 0
    if not absolute:
        gaps=[(ys[i]-ys[i-1]-float(visible[i-1].get('height',0))) if vertical else (xs[i]-xs[i-1]-float(visible[i-1].get('width',0))) for i in range(1,len(visible))]
        gap=max(0,statistics.median(gaps)) if gaps else 0
        style+=f' gap-[{gap:g}px]'
        if padx:style+=f' px-[{padx:g}px]'
        if pady:style+=f' pt-[{pady:g}px]'
    else:style+=f' h-[{h:g}px]'
    if padx>=16 and nm not in ('Cabeçalho','Conteúdo / Principal','Conteúdo / Formulário','Câmera / Prévia ilustrativa'):
        style+=f' bg-[{"#132440" if dark else "#FFFFFF"}] rounded-[20px] pb-[{pady:g}px]'
    if nm.startswith('Voltar /'):style+=f' bg-[{"#132440" if dark else "#FFFFFF"}] rounded-[24px] pb-[{pady:g}px]'
    if nm=='Alternar tema':style+=f' bg-[{"#1C385B" if dark else "#EAF2FC"}] rounded-[24px] pb-[{pady:g}px]'
    if nm=='Câmera / Captura':style+=' items-center'
    if nm=='Câmera / Prévia ilustrativa':style+=' bg-[#101622] rounded-[20px]'
    if nm=='Histórico / Drawer':style+=f' bg-[{"#132440" if dark else "#FFFFFF"}]'
    if nm.startswith('Selo /'):style+=f' bg-[{"#FF984F" if dark else "#102654"}] rounded-[28px] pb-[{pady:g}px]'
    if illustrated:style+=f' bg-[{"#142D4B" if dark else "#EAF2FC"}] rounded-[24px]'
    result=node('div',nm,style,[],ident)
    action_index=0
    for child in visible:
        item=convert(child,dark,screen,nm,action_index)
        if child.get('name','').startswith('Ação /') and float(child.get('height',0))>=55:action_index+=1
        if absolute:
            item['attrs']['className']+=f' h-[{float(child.get("height",0)):g}px] absolute'+f' left-[{float(child.get("x",0)):g}px] top-[{float(child.get("y",0)):g}px]'
        result['children'].append(item)
    return result

supplement={}
for frame in frames:
    canonical=frame['name'].replace('/ Escuro','/ Claro')
    light=next((f for f in frames if f['name']==canonical),frame)
    if light['id'] not in reviewed or frame['id'] in trees:continue
    el=elements[frame['id']]
    dark=frame['name'].endswith('Escuro')
    if dark:
        light_texts=[c for c in elements[light['id']].iter() if c.tag=='text']
        dark_texts=[c for c in el.iter() if c.tag=='text']
        for lt,dt in zip(light_texts,dark_texts):
            if lt.get('id') in overrides.get('texts',{}):overrides['texts'][dt.get('id')]=overrides['texts'][lt.get('id')]
        light_buttons=[c for c in elements[light['id']].iter() if c.tag=='instance']
        dark_buttons=[c for c in el.iter() if c.tag=='instance']
        for lt,dt in zip(light_buttons,dark_buttons):
            if lt.get('id') in overrides.get('buttons',{}):overrides['buttons'][dt.get('id')]=overrides['buttons'][lt.get('id')]
    result=convert(el,dark,frame['name'])
    result['attrs']['className']=f'bg-[{"#09142A" if dark else "#F6F7FA"}] relative w-[412px] h-[917px]'
    if '/ Tirar foto /' in frame['name']:result['attrs']['className']='bg-[#070B10] relative w-[412px] h-[917px]'
    for child,source in zip(result['children'],[c for c in el if c.get('hidden')!='true']):
        child['attrs']['className']+=' absolute'+f' left-[{float(source.get("x",0)):g}px] top-[{float(source.get("y",0)):g}px]'
    supplement[frame['id']]=result
(CACHE/'browser-trees.json').write_text(json.dumps(supplement,ensure_ascii=False),encoding='utf-8')
print(json.dumps({'browser_screens':len(supplement),'reviewed_light_frames':len(reviewed)}))
