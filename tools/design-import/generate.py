"""Generate responsive Android XML from the retrieved Figma contexts.

Run parse.mjs first. Coordinates are used only inside illustrations/maps; app
content uses native flow layouts, weighted columns, and real editable fields.
"""
import copy
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'app/src/main/res'
ASSETS = ROOT / 'app/src/main/assets'
CACHE = ROOT / '.design-cache'
trees = json.loads((CACHE / 'trees.json').read_text(encoding='utf-8'))
frames = json.loads((CACHE / 'frames.json').read_text(encoding='utf-8'))
links = json.loads((CACHE / 'links.json').read_text(encoding='utf-8'))
by_id = {f['id']: f for f in frames}
by_name = {f['name']: f for f in frames}
metadata = ET.parse(CACHE / 'client-metadata.xml').getroot()
geometry = {n.get('id'): {key: float(n.get(key,'0')) for key in ('x','y','width','height')}
            for n in metadata.iter() if n.get('id')}
screen_actions = {}
for element in metadata.iter():
    if element.get('id') in by_id:
        screen_actions[element.get('id')] = [
            {'id': child.get('id'), 'name': child.get('name', '')}
            for child in element.iter() if child.get('id') != element.get('id')
        ]
strings, assets, backgrounds, specs = {}, {}, {}, {}
A = 'http://schemas.android.com/apk/res/android'
APP = 'http://schemas.android.com/apk/res-auto'
ET.register_namespace('android', A)
ET.register_namespace('app', APP)

def prop(el, key, value):
    el.set('{%s}%s' % (A, key), str(value))

def custom(el, key, value):
    el.set('{%s}%s' % (APP, key), str(value))

def save(path, el):
    path.parent.mkdir(parents=True, exist_ok=True)
    ET.indent(el, space='    ')
    ET.ElementTree(el).write(path, encoding='utf-8', xml_declaration=True)

def digest(value):
    return hashlib.sha256(value.encode()).hexdigest()[:12]

def string(value):
    key = 'design_' + digest(value)
    strings[key] = value
    return '@string/' + key

def classes(node):
    return node.get('attrs', {}).get('className', '').replace('\\/', '/')

def name(node):
    return node.get('attrs', {}).get('data-name', '')

def children(node):
    result = []
    for child in node.get('children', []):
        if not isinstance(child, dict):
            continue
        if ' contents ' in ' '+classes(child)+' ':
            result.extend(children(child))
        else:
            result.append(child)
    return result

def art_bounds(cls, width, height):
    inset = re.search(r'inset-\[([^\]]+)\]',cls)
    x,y,w,h = number(cls,'left',0),number(cls,'top',0),number(cls,'w',number(cls,'size',width)),number(cls,'h',number(cls,'size',height))
    if inset:
        values=inset[1].split('_')
        if len(values)==1: values*=4
        if len(values)==2: values*=2
        def length(value,basis):
            return float(value[:-1])*basis/100 if value.endswith('%') else float(value.removesuffix('px'))
        top,right,bottom,left=[length(v,height if i%2==0 else width) for i,v in enumerate(values)]
        x,y,w,h=left,top,width-left-right,height-top-bottom
    if '-translate-x-1/2' in cls:x-=w/2
    if '-translate-y-1/2' in cls:y-=h/2
    return x,y,max(1,w),max(1,h)

def text(node):
    return ' '.join(c if isinstance(c, str) else text(c) for c in node.get('children', []))

def number(cls, key, default=None):
    match = re.search(r'(?:^|\s)' + re.escape(key) + r'-\[(?:var\([^,]+,)?(-?[\d.]+)(?:px)?\)?\]', cls)
    return float(match[1]) if match else default

def color(cls, key):
    rgba=re.search(r'(?:^|\s)'+key+r'-\[rgba\(([\d.]+),([\d.]+),([\d.]+),([\d.]+)\)\]',cls)
    if rgba:
        red,green,blue,alpha=map(float,rgba.groups())
        return '#%02X%02X%02X%02X' % (round(alpha*255),round(red),round(green),round(blue))
    match = re.search(r'(?:^|\s)' + key + r'-\[(?:color:)?(?:var\([^,]+,)?(#[a-fA-F0-9]+|white|black)\)?\]', cls)
    if match:
        return {'white': '#FFFFFF', 'black': '#000000'}.get(match[1], match[1])
    if key + '-white' in cls:
        return '#FFFFFF'
    return None

def dp(value):
    return f'{value:g}dp'

def background(cls):
    fill = color(cls, 'bg')
    stroke = color(cls, 'border')
    radius = number(cls, 'rounded', 0)
    corners = [number(cls, 'rounded-'+side, radius) for side in ('tl','tr','br','bl')]
    if not fill and not stroke:
        return None
    key = 'design_' + digest(str((fill, stroke, corners)))
    if key not in backgrounds:
        shape = ET.Element('shape')
        prop(shape, 'shape', 'rectangle')
        solid = ET.SubElement(shape, 'solid')
        prop(solid, 'color', fill or '#00000000')
        if any(corners):
            corner = ET.SubElement(shape, 'corners')
            for side, value in zip(('topLeft','topRight','bottomRight','bottomLeft'), corners):
                prop(corner, side+'Radius', dp(value))
        if stroke:
            item = ET.SubElement(shape, 'stroke')
            prop(item, 'width', '1dp')
            prop(item, 'color', stroke)
        backgrounds[key] = shape
    return '@drawable/' + key

def asset(url):
    if url.startswith('local/'):
        return url
    key = 'figma/' + url.rsplit('/', 1)[-1]
    assets[key] = url
    return key

def convert(node, parent_horizontal=False, inherited=None, field='', art=False, parent_width=364, parent_end=False, parent_center=False):
    inherited = inherited or {}
    cls, nm = classes(node), name(node)
    ch = children(node)
    typ = node['type']
    own_width = number(cls, 'w', number(cls, 'size', parent_width))
    own_height = number(cls, 'h', number(cls, 'size'))
    current = {**inherited}
    for token, weight in [('font-normal',400),('font-medium',500),('font-semibold',600),('font-bold',700),('font-extrabold',800)]:
        if token in cls: current['weight'] = weight
    for alignment in ('left','center','right'):
        if 'text-'+alignment in cls: current['align'] = alignment
    for key in ('text', 'leading'):
        val = number(cls, key)
        if val is not None:
            current[key] = val
    txt_color = color(cls, 'text') or current.get('color', '#132B50')
    current['color'] = txt_color
    if field not in ('Mensagem','Busca') and 'Campo' in nm and ch and ch[0]['type'] == 'p':
        field = text(ch[0])
    editable = (nm == 'Entrada' and bool(field)) or (typ == 'p' and field in ('Mensagem','Busca'))
    if editable and ch and ch[0]['type'] == 'p':
        current['text'] = number(classes(ch[0]), 'text', current.get('text',16))
        txt_color = color(classes(ch[0]), 'text') or txt_color
    mask_style = node.get('attrs',{}).get('style',{}).get('maskImage','')
    mask_url = re.search(r'https://[^"\)]+',mask_style)
    single_image = len(ch) == 1 and ch[0]['type'] == 'img' and not mask_url
    visual = bool(ch) and (nm.startswith('Apresentação / Cena ilustrada') or all('absolute' in classes(c) for c in ch) or
                          (own_height is not None and any('absolute' in classes(c) for c in ch) and ('flex' not in cls or nm == 'Foto da conta'))) and not single_image and typ != 'p'
    if nm=='Painel / Conteúdo rolável':visual=False
    horizontal = 'flex' in cls and 'flex-col' not in cls
    if editable:
        el = ET.Element('androidx.appcompat.widget.AppCompatEditText')
    elif typ == 'p':
        el = ET.Element('androidx.appcompat.widget.AppCompatTextView')
    elif single_image or typ == 'img':
        srcnode = ch[0] if single_image else node
        src = srcnode['attrs'].get('src', '')
        el = ET.Element('com.mottainai.cliente.design.DesignImageView')
        if src:
            custom(el, 'designAsset', asset(src))
        prop(el,'scaleType','fitStart' if nm.startswith('Marca original completa /') else 'fitCenter')
        prop(el, 'importantForAccessibility', 'no')
    elif visual or mask_url:
        el = ET.Element('com.mottainai.cliente.design.DesignMaskLayout' if mask_url else 'com.mottainai.cliente.design.DesignArtLayout')
        custom(el, 'designWidth', own_width)
        custom(el, 'designHeight', own_height or own_width)
        if mask_url:
            custom(el,'designMask',asset(mask_url[0]))
            pos=re.search(r'mask-position-\[([-\d.]+)px_([-\d.]+)px\]',cls)
            size=re.search(r'mask-size-\[([\d.]+)px_([\d.]+)px\]',cls)
            custom(el,'maskX',pos[1] if pos else 0);custom(el,'maskY',pos[2] if pos else 0)
            custom(el,'maskWidth',size[1] if size else own_width);custom(el,'maskHeight',size[2] if size else own_height or own_width)
        visual = True
    else:
        el = ET.Element('LinearLayout')
        prop(el, 'orientation', 'horizontal' if horizontal else 'vertical')
        if horizontal:
            prop(el, 'baselineAligned', 'false')
        prop(el, 'clipChildren', 'false')
        prop(el, 'clipToPadding', 'false')
        gravity = []
        if 'items-center' in cls:
            gravity.append('center_vertical' if horizontal else 'center_horizontal')
        if 'justify-center' in cls:
            gravity.append('center_horizontal' if horizontal else 'center_vertical')
        if 'justify-end' in cls:
            gravity.append('end' if horizontal else 'bottom')
        if 'items-end' in cls:
            gravity.append('bottom' if horizontal else 'end')
        if gravity:
            prop(el, 'gravity', '|'.join(gravity))

    width = 'match_parent'
    if (typ != 'p' and own_width <= 112) or (single_image and own_width < parent_width):
        width = dp(own_width)
    if typ != 'p' and not parent_horizontal and number(cls,'w') is not None and own_width < parent_width:
        width = dp(own_width)
        if own_width > 112 and not single_image and not visual:
            width = 'match_parent'
            if el.tag=='LinearLayout':
                el.tag='com.mottainai.cliente.design.DesignBoundedLayout'
                custom(el,'designMaxWidth',dp(own_width))
    flexible = parent_horizontal and not single_image and (typ == 'p' or editable or own_width > 112 or 'flex-[1_' in cls or 'w-full' in cls)
    if flexible:
        width = '0dp'
        prop(el, 'layout_weight', max(own_width, 1))
    prop(el, 'layout_width', width)
    height = 'wrap_content'
    if single_image or typ == 'img':
        height = dp(own_height or own_width)
        if art:
            width = 'match_parent'
            height = 'match_parent'
            prop(el, 'layout_width', width)
        if 'w-full' in cls:
            prop(el, 'layout_width', 'match_parent')
        if own_width > 112:
            prop(el, 'layout_width', 'match_parent')
        if nm=='Ellipse':
            prop(el,'layout_width',dp(own_height or own_width))
            prop(el,'layout_height',dp(own_height or own_width))
            prop(el,'scaleType','centerCrop')
    elif visual:
        height = 'wrap_content'
    elif own_height is not None and typ != 'p' and nm!='Painel / Conteúdo rolável':
        prop(el, 'minHeight', dp(own_height))
        if nm=='Ilustração do produto':
            height=dp(own_height)
            prop(el,'clipChildren','true');prop(el,'clipToPadding','false');prop(el,'clipToOutline','true')
    prop(el, 'layout_height', height)
    rotation=re.search(r'(?:^|\s)(-?)rotate-(\d+(?:\.\d+)?)',cls)
    if rotation:prop(el,'rotation',float(rotation[2])*(-1 if rotation[1] else 1))
    opacity=re.search(r'(?:^|\s)opacity-(\d+)',cls)
    if opacity:prop(el,'alpha',float(opacity[1])/100)
    bg = background(cls)
    if bg:
        prop(el, 'background', bg)
    pads = {side: number(cls, 'p', 0) for side in ('Left','Right','Top','Bottom')}
    for axis, sides in [('x', ('Left','Right')), ('y', ('Top','Bottom'))]:
        val = number(cls, 'p'+axis)
        if val is not None:
            for side in sides:
                pads[side] = val
    for letter, side in [('l','Left'),('r','Right'),('t','Top'),('b','Bottom')]:
        pads[side] = number(cls, 'p'+letter, pads[side])
    for side,val in pads.items():
        if val:
            prop(el, 'padding'+side, dp(val))
    if nm in ('Maré / Marca original / Completa','Logo original Mottainai'):
        prop(el,'paddingHorizontal','6dp')
    if 'overflow-clip' in cls and nm=='Cliente / Avatar da Lô':
        prop(el,'clipChildren','true');prop(el,'clipToOutline','true')
    if nm.startswith('Registro /') and horizontal:
        prop(el,'gravity','center_vertical')
    node_id = node['attrs'].get('data-node-id', node['attrs'].get('id', ''))
    if node_id.startswith('node-'):
        node_id = node_id[5:].replace('_', ':')
    tag = {'id':node_id, 'name':nm}
    semantic_button = not editable and not single_image and typ != 'p' and not nm.startswith('Campo /') and 'justify-center' in cls and 'flex' in cls and own_height is not None and own_height >= 44 and (any(c['type']=='p' for c in ch) or nm=='Enviar mensagem')
    if typ in ('a','button') or 'cursor-pointer' in cls or nm.startswith('Destino /') or semantic_button:
        tag['action'] = text(node)
        prop(el, 'clickable', 'true')
        prop(el, 'focusable', 'true')
        prop(el, 'minHeight', dp(max(48,own_height or 0)))
        prop(el, 'contentDescription', string(text(node) or nm))
    if typ == 'p' or editable:
        value = text(node)
        prop(el, 'fontFamily', 'sans-serif' if 'Roboto:' in cls else '@font/dm_sans')
        prop(el, 'includeFontPadding', 'false')
        tag['fontWeight']=current.get('weight',400)
        if 'Roboto:' not in cls:tag['fontOpticalSize']=14
        prop(el, 'textSize', f'{current.get("text",16):g}sp')
        prop(el, 'textColor', txt_color)
        tracking=number(cls,'tracking')
        if tracking is not None:prop(el,'letterSpacing',tracking/current.get('text',16))
        prop(el, 'lineSpacingMultiplier', current.get('leading',1.4))
        prop(el, 'minHeight', f'{current.get("text",16)*current.get("leading",1.4):g}sp')
        prop(el, 'gravity', 'start|center_vertical')
        if current.get('weight') == 700:
            prop(el, 'textStyle', 'bold')
        if current.get('align') == 'center':
            prop(el, 'gravity', 'center')
        elif current.get('align') == 'right':
            prop(el, 'gravity', 'end|center_vertical')
        if editable:
            tag['field'] = field
            tag['id'] = node_id + ':' + digest(field)
            prop(el, 'id', '@+id/input_'+digest(field))
            prop(el, 'contentDescription', string(field))
            prop(el, 'hint', string(value))
            if node.get('attrs',{}).get('data-initial') is not None:
                prop(el,'text',string(node['attrs']['data-initial']))
            elif ch and 'color/text,' in classes(ch[0]) and not any(v in value.lower() for v in ('digite','seu nome','dd/mm','••','senha')):
                prop(el,'text',string(value))
            elif field in ('Busca','Mensagem') and not value.lower().startswith(('buscar','pergunte','escreva')):
                prop(el,'text',string(value))
            prop(el, 'textColorHint', txt_color)
            input_type = 'textCapSentences'
            if 'mail' in field.lower():
                input_type = 'textEmailAddress'
            elif 'senha' in field.lower():
                input_type = 'textPassword'
            elif any(x in field.lower() for x in ('cep','cpf','número')):
                input_type = 'number'
            elif 'telefone' in field.lower():
                input_type = 'phone'
            prop(el, 'inputType', input_type)
            prop(el, 'imeOptions', 'actionNext')
            prop(el, 'singleLine', 'true')
            if field in ('Mensagem','Busca'):
                prop(el, 'background', '@android:color/transparent')
                prop(el, 'imeOptions', 'actionSend' if field == 'Mensagem' else 'actionSearch')
                prop(el, 'padding', '0dp')
                prop(el, 'minHeight', f'{current.get("text",16)*current.get("leading",1.4):g}sp')
            else:
                prop(el, 'minHeight', '56dp')
            prop(el, 'saveEnabled', 'true')
        else:
            prop(el, 'text', string(value))
            if 'line-through' in cls:
                tag['strike'] = True
    prop(el, 'tag', json.dumps(tag,ensure_ascii=False))
    if not editable and typ != 'p' and not single_image and typ != 'img':
        gap = number(cls, 'gap', 0)
        available_width=max(1,own_width-pads['Left']-pads['Right'])
        auto_columns=[c for c in ch if 'flex-[1_' in classes(c) and number(classes(c),'w') is None]
        fixed_width=sum(number(classes(c),'w',number(classes(c),'size',0)) for c in ch if c not in auto_columns)
        composer=(nm.startswith('Mensagem /') or nm=='Escrever mensagem') and 'absolute' in cls and number(cls,'top',0)>600
        composer_opacity=re.search(r'(?:^|\s)opacity-(\d+)',cls)
        composer_disabled=composer and bool(composer_opacity and int(composer_opacity[1])<100)
        for index, child in enumerate(ch):
            if index and not horizontal and 'justify-between' in cls and own_height:
                spacer=ET.SubElement(el,'Space')
                prop(spacer,'layout_width','1dp');prop(spacer,'layout_height','0dp');prop(spacer,'layout_weight','1')
            if index and horizontal and 'justify-between' in cls:
                spacer=ET.SubElement(el,'Space')
                prop(spacer,'layout_width','0dp');prop(spacer,'layout_height','1dp');prop(spacer,'layout_weight','1')
            if horizontal and child in auto_columns:
                child=copy.deepcopy(child)
                column_width=max(1,(available_width-fixed_width-gap*(len(ch)-1))/len(auto_columns))
                child['attrs']['className']=classes(child)+f' w-[{column_width}px]'
            if visual:
                if child['type']=='p' and number(classes(child),'h') is None:
                    child=copy.deepcopy(child)
                    h=number(classes(child),'text',current.get('text',16))*number(classes(child),'leading',current.get('leading',1.4))
                    child['attrs']['className']=classes(child)+f' h-[{h}px]'
                x,y,w,h=art_bounds(classes(child),own_width,own_height or own_width)
                measured=geometry.get(child.get('attrs',{}).get('data-node-id'))
                if nm.startswith('Apresentação / Cena ilustrada') and measured:
                    x,y,w,h=(measured[k] for k in ('x','y','width','height'))
                if number(classes(child),'h',number(classes(child),'size')) is None and 'inset-' not in classes(child) and child['type'] not in ('img','p') and not nm.startswith('Apresentação / Cena ilustrada'):
                    h=0
                if 'absolute' not in classes(child) and 'items-center' in cls:
                    x=(own_width-w)/2
                child=copy.deepcopy(child)
                child['attrs']['className']=classes(child)+f' w-[{w}px] h-[{h}px]'
            child_field = 'Mensagem' if composer and not composer_disabled and (child['type']=='p' or name(child).lower()=='campo / mensagem') else ('Busca' if nm == 'Campo / Buscar produtos' and child['type'] == 'p' else field)
            child_el = convert(child, horizontal and not visual and len(ch)>1, current, child_field, visual,
                               own_width if visual else available_width, 'items-end' in cls,
                               'items-center' in cls and not horizontal)
            if 'Google' in nm and child['type']=='p':
                prop(child_el,'layout_width','wrap_content')
                child_el.attrib.pop('{'+A+'}layout_weight',None)
                if number(classes(child),'w') is not None:prop(child_el,'minWidth',dp(number(classes(child),'w')))
            if composer_disabled and name(child)=='Enviar mensagem':prop(child_el,'enabled','false')
            if index and gap and not visual:
                prop(child_el, 'layout_marginStart' if horizontal else 'layout_marginTop', dp(gap))
            if visual:
                custom(child_el, 'designX', x)
                custom(child_el, 'designY', y)
                custom(child_el, 'designChildWidth', w)
                custom(child_el, 'designChildHeight', h)
            el.append(child_el)
        if nm.startswith('Registro /') and horizontal and len(el)>1:
            last=el[-1]
            prop(last,'layout_width','wrap_content')
            last.attrib.pop('{'+A+'}layout_weight',None)
            prop(last,'gravity','end|center_vertical')
    return el

def screen_xml(tree):
    shell = ET.Element('FrameLayout')
    prop(shell,'layout_width','match_parent');prop(shell,'layout_height','match_parent')
    canvas=color(classes(tree),'bg') or ('#09142A' if name(tree).endswith('/ Escuro') else '#F6F7FA')
    prop(shell,'background',canvas)
    body = ET.SubElement(shell,'LinearLayout')
    prop(body,'layout_width','match_parent');prop(body,'layout_height','match_parent')
    prop(body,'orientation','vertical');prop(body,'paddingHorizontal','24dp');prop(body,'paddingTop','32dp')
    items = children(tree)
    header = next((n for n in items if name(n)=='Cabeçalho'),None)
    scrolling = next((n for n in items if name(n)=='Conteúdo / Rolagem'),None)
    if scrolling is None:
        scrolling = next((n for n in items if name(n).startswith('Conversa /')),None)
    nav = next((n for n in items if name(n).startswith('Navbar persistente')),None)
    camera_header=next((n for n in items if name(n)=='Câmera / Cabeçalho'),None)
    if camera_header:
        body.append(convert(camera_header))
        viewport=ET.SubElement(body,'androidx.core.widget.NestedScrollView')
        prop(viewport,'layout_width','match_parent');prop(viewport,'layout_height','0dp');prop(viewport,'layout_weight','1')
        prop(viewport,'layout_marginTop','28dp')
        stack=ET.SubElement(viewport,'LinearLayout');prop(stack,'layout_width','match_parent');prop(stack,'layout_height','wrap_content');prop(stack,'orientation','vertical')
        for item in items:
            if name(item) in ('Câmera / Prévia ilustrativa','Câmera / Captura'):
                child=convert(item)
                if name(item)=='Câmera / Captura':
                    preview=next(n for n in items if name(n)=='Câmera / Prévia ilustrativa')
                    preview_box=geometry[preview['attrs']['data-node-id']]
                    capture_box=geometry[item['attrs']['data-node-id']]
                    gap=capture_box['y']-preview_box['y']-preview_box['height']
                    prop(child,'layout_marginTop',f'{gap:g}dp')
                stack.append(child)
        return shell
    drawer = next((n for n in items if name(n)=='Histórico / Drawer'),None)
    if drawer:
        prop(shell,'background','@android:color/transparent')
        shell.remove(body)
        backdrop=ET.SubElement(shell,'View')
        prop(backdrop,'layout_width','match_parent');prop(backdrop,'layout_height','match_parent')
        prop(backdrop,'background','#99000000')
        prop(backdrop,'clickable','true');prop(backdrop,'contentDescription',string('Fechar histórico'))
        prop(backdrop,'tag',json.dumps({'name':'Fundo / Fechar histórico'}))
        viewport=ET.SubElement(shell,'androidx.core.widget.NestedScrollView')
        prop(viewport,'layout_width','match_parent');prop(viewport,'layout_height','match_parent')
        prop(viewport,'layout_marginEnd','84dp');prop(viewport,'fillViewport','true')
        panel=convert(drawer);panel.attrib.pop('{'+A+'}minHeight',None)
        viewport.append(panel)
        return shell
    if not scrolling and any(name(n).startswith('Marca / Abertura') for n in items):
        splash=ET.Element('androidx.constraintlayout.widget.ConstraintLayout')
        prop(splash,'layout_width','match_parent');prop(splash,'layout_height','match_parent')
        prop(splash,'background',color(classes(tree),'bg') or '#F6F7FA')
        for item in (n for n in tree.get('children',[]) if isinstance(n,dict)):
            nm=name(item)
            if not any(nm.startswith(prefix) for prefix in ('Marca / Abertura','Tsuru /','Papel /','Mensagem /')):
                continue
            if nm.startswith('Tsuru /'):
                item=copy.deepcopy(children(item)[0])
                item['attrs']['className']='relative w-[360px] h-[141.765px]'
            el=convert(item)
            prop(el,'layout_width','0dp')
            custom(el,'layout_constraintStart_toStartOf','parent');custom(el,'layout_constraintEnd_toEndOf','parent')
            if nm.startswith('Marca /'):
                prop(el,'layout_marginStart','40dp');prop(el,'layout_marginEnd','40dp');prop(el,'layout_marginTop','110dp')
                custom(el,'layout_constraintTop_toTopOf','parent')
            elif nm.startswith('Tsuru /'):
                prop(el,'layout_marginStart','26dp');prop(el,'layout_marginEnd','26dp');prop(el,'layout_height','0dp')
                custom(el,'layout_constraintTop_toTopOf','parent');custom(el,'layout_constraintBottom_toBottomOf','parent')
                custom(el,'layout_constraintVertical_bias','0.426');custom(el,'layout_constraintDimensionRatio','360:141.765')
            elif nm.startswith('Papel /'):
                prop(el,'id','@+id/design_splash_paper')
                prop(el,'layout_height','0dp');custom(el,'layout_constraintDimensionRatio','412:350')
                custom(el,'layout_constraintBottom_toBottomOf','parent')
            else:
                prop(el,'layout_marginStart','40dp');prop(el,'layout_marginEnd','40dp');prop(el,'layout_marginTop',dp(number(classes(item),'top',683)-567))
                custom(el,'layout_constraintTop_toTopOf','@id/design_splash_paper')
            splash.append(el)
        return splash
    if scrolling is None and any(name(n).startswith('Fundo /') for n in items):
        overlay=ET.Element('FrameLayout')
        prop(overlay,'layout_width','match_parent');prop(overlay,'layout_height','match_parent')
        dim=next(n for n in items if name(n).startswith('Fundo /'))
        prop(overlay,'background','@android:color/transparent')
        backdrop=ET.SubElement(overlay,'View')
        prop(backdrop,'layout_width','match_parent');prop(backdrop,'layout_height','match_parent')
        prop(backdrop,'background',color(classes(dim),'bg') or '#7A000000')
        prop(backdrop,'clickable','true');prop(backdrop,'contentDescription',string('Fechar painel'))
        prop(backdrop,'tag',json.dumps({'id':dim['attrs'].get('data-node-id',''),'name':name(dim)}))
        panel=next(n for n in items if not name(n).startswith('Fundo /'))
        viewport=ET.SubElement(overlay,'androidx.core.widget.NestedScrollView')
        prop(viewport,'layout_width','match_parent');prop(viewport,'layout_height','wrap_content')
        prop(viewport,'layout_gravity','bottom' if number(classes(panel),'left',0)==0 else 'center')
        if number(classes(panel),'left',0)>0:
            prop(viewport,'layout_marginStart','24dp');prop(viewport,'layout_marginEnd','24dp')
        viewport.append(convert(panel))
        return overlay
    if header:
        header_el = convert(header)
        prop(header_el,'layout_marginBottom',dp(max(18,number(classes(scrolling or {}),'top',122)-104)))
        body.append(header_el)
    if scrolling:
        if not header:
            prop(body,'paddingTop',dp(max(24,number(classes(scrolling),'top',56)-24)))
        scroll = ET.SubElement(body,'androidx.core.widget.NestedScrollView')
        prop(scroll,'id','@+id/design_scroll');prop(scroll,'layout_width','match_parent');prop(scroll,'layout_height','0dp');prop(scroll,'layout_weight','1')
        prop(scroll,'fillViewport','false');prop(scroll,'clipToPadding','false');prop(scroll,'scrollbars','none')
        content=children(scrolling)
        scroll.append(convert(content[0] if len(content)==1 else scrolling))
    else:
        # Splash artwork is proportional; text remains separate and scales with font size.
        scroll = ET.SubElement(body,'androidx.core.widget.NestedScrollView')
        prop(scroll,'layout_width','match_parent');prop(scroll,'layout_height','0dp');prop(scroll,'layout_weight','1');prop(scroll,'fillViewport','true')
        stack=ET.SubElement(scroll,'LinearLayout');prop(stack,'layout_width','match_parent');prop(stack,'layout_height','wrap_content');prop(stack,'orientation','vertical')
        for item in items:
            if any(x in name(item) for x in ('Barra do sistema','Indicador de gestos','Papel /')):
                continue
            el=convert(item);prop(el,'layout_marginTop','48dp');stack.append(el)
    extra = [n for n in items if n is not header and n is not scrolling and n is not nav and not any(x in name(n) for x in ('Barra do sistema','Indicador de gestos','Flutuante','flutuante'))]
    if scrolling:
        for item in extra:
            el=convert(item);prop(el,'layout_marginTop','12dp');body.append(el)
    if nav:
        composer=next((n for n in extra if name(n).startswith('Mensagem /') or name(n)=='Escrever mensagem'),None)
        nav_gap=max(12,number(classes(nav),'top',818)-number(classes(composer),'top',734)-number(classes(composer),'h',56)) if composer else 16
        el=convert(nav);prop(el,'layout_marginTop',dp(nav_gap));prop(el,'layout_marginBottom','0dp')
        # All five touch targets take an equal share of the available width.
        for destination in list(el):
            if destination.tag=='Space':
                el.remove(destination);continue
            prop(destination,'layout_width','0dp');prop(destination,'layout_weight','1')
        body.append(el)
    fab=next((n for n in items if 'Flutuante' in name(n) or 'flutuante' in name(n)),None)
    if fab:
        el=convert(fab);prop(el,'layout_width','84dp');prop(el,'layout_gravity','bottom|end');prop(el,'layout_marginEnd','24dp');prop(el,'layout_marginBottom','96dp' if nav else '24dp');shell.append(el)
    return shell

layout_names = json.loads((ROOT/'tools/design-import/layout-names.json').read_text(encoding='utf-8'))
used_layout_names = set()
missing=[]
for frame in frames:
    if not frame['name'].endswith('/ Claro'):
        continue
    dark=by_name.get(frame['name'][:-5]+'Escuro')
    light_tree=trees.get(frame['id'])
    dark_tree=trees.get(dark['id']) if dark else None
    if not light_tree:
        missing.append(frame);continue
    overlay = any(name(n).startswith('Fundo /') or name(n)=='Histórico / Drawer' for n in children(light_tree))
    key = {
        '2005:3812':'fragment_home', '2005:3914':'fragment_catalog',
        '2005:4305':'fragment_stores', '2093:6331':'fragment_points',
        '2005:4483':'fragment_profile', '2143:11079':'fragment_impact',
        '2055:5379':'activity_assistant', '2005:4104':'activity_offer_detail',
    }.get(frame['id'], layout_names.get(frame['id']))
    if key is None: raise ValueError('Add an English layout name for '+frame['id']+' in layout-names.json')
    if key in used_layout_names: raise ValueError('Duplicate layout name: '+key)
    used_layout_names.add(key)
    save(RES/'layout'/f'{key}.xml',screen_xml(light_tree))
    if dark_tree:
        save(RES/'layout-night'/f'{key}.xml',screen_xml(dark_tree))
    else:
        missing.append({'id':dark['id'],'name':dark['name']})
        raise ValueError('Missing original dark context: '+frame['name'])
    specs[frame['id']]={'layout':key,'name':frame['name'].removesuffix(' / Claro'),'night':bool(dark_tree),
                       'overlay':any(name(n).startswith('Fundo /') or name(n)=='Histórico / Drawer' for n in children(light_tree)),
                       'actions':screen_actions.get(frame['id'], [])}
    if dark:
        specs[dark['id']]={**specs[frame['id']],'canonical':frame['id']}
    stale_key = 'screen_'+frame['id'].replace(':','_')
    if stale_key != key:
        for folder in ('layout','layout-night'):
            stale = RES/folder/f'{stale_key}.xml'
            if stale.exists():
                stale.unlink()

for key,shape in backgrounds.items():
    save(RES/'drawable'/f'{key}.xml',shape)
resource=ET.Element('resources')
for key,val in strings.items():
    el=ET.SubElement(resource,'string',{'name':key,'formatted':'false'})
    el.text=val.replace('\\','\\\\').replace("'","\\'").replace('"','\\"').replace('\n','\\n')
save(RES/'values'/'design_strings.xml',resource)
ASSETS.mkdir(parents=True,exist_ok=True)
(ASSETS/'design_screens.json').write_text(json.dumps(specs,ensure_ascii=False,indent=2),encoding='utf-8')
(ASSETS/'design_navigation.json').write_text(json.dumps(links,ensure_ascii=False),encoding='utf-8')
(CACHE/'assets.json').write_text(json.dumps(assets,indent=2),encoding='utf-8')
(ROOT/'docs').mkdir(exist_ok=True)
(ROOT/'docs'/'figma-context-gaps.json').write_text(json.dumps(missing,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'implemented':len(specs)//2,'missing_references':len(missing),'assets':len(assets)}))
