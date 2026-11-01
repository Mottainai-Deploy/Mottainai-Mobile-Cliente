"""Derive data slots from the PR's native Figma layouts without changing their styling."""
import copy
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / 'app/src/main/res'
A = '{http://schemas.android.com/apk/res/android}'
ET.register_namespace('android', A[1:-1])
ET.register_namespace('app', 'http://schemas.android.com/apk/res-auto')
strings = {}
for file in (ROOT / 'values').glob('*.xml'):
    for e in ET.parse(file).getroot():
        if e.tag == 'string':
            strings[e.get('name')] = e.text or ''

slots = {
    'Cesta hortifruti': 'title', 'Leite integral': 'title',
    'Mercado da Praça': 'store', 'Mercado da Praça — 1,2 km': 'storeDistance',
    'R$ 12,90': 'price', 'R$ 24,90': 'originalPrice',
    '−48%': 'discount', '−48% hoje': 'discount', '1,2 km': 'distance',
    'Retirada hoje': 'pickup', 'Retirada hoje · 16h–19h': 'pickup',
    'Hoje, das 16h às 19h': 'pickup', 'Hoje, até as 20h': 'period',
    'Val. 22/10/2026': 'expiry', 'Validade 22/10/2026': 'expiry',
    '22/10/2026 — faltam 3 dias': 'expiry',
    'Uma seleção de frutas e verduras do dia. A composição varia conforme a disponibilidade da loja.': 'description',
    'Rua Afonso Celso, 210': 'address', 'Vila Mariana, São Paulo': 'region',
    'Vila Mariana, SP': 'region',
    'Camila': 'customerName', 'Camila Oliveira': 'customerName',
    'camila@exemplo.com': 'customerEmail', '(11) 90000-0000': 'customerPhone',
    'Bom dia, Camila': 'greeting', 'Oi, Camila.': 'greeting',
    '320': 'points', '320 pontos de fidelidade': 'points',
    'Praça da Sé, 100': 'reviewStreet', 'Sé, São Paulo — SP': 'reviewCity',
    'CEP 01001-000': 'reviewZip',
}

def tag(e):
    return json.loads(e.get(A + 'tag', '{}'))

def find(root, name):
    return next(e for e in root.iter() if tag(e).get('name') == name)

def write(path, root):
    ET.indent(root, space='    ')
    ET.ElementTree(root).write(path, encoding='utf-8', xml_declaration=True)

for directory in ['layout', 'layout-night']:
    folder = ROOT / directory
    # These are the original PR cards. Data binding uses the same spacing, typography and backgrounds.
    home = ET.parse(folder / 'fragment_home.xml').getroot()
    catalog = ET.parse(folder / 'fragment_catalog.xml').getroot()
    stores = ET.parse(folder / 'fragment_stores.xml').getroot()
    templates = {
        'design_item_offer_compact': copy.deepcopy(find(home, 'Oferta / Cesta hortifruti')),
        'design_item_offer_grid': copy.deepcopy(list(find(catalog, 'Ofertas / Grade compacta'))[0]),
        'design_item_store': copy.deepcopy(find(stores, 'Opção / Mercado da Praça')),
    }
    for name, root in templates.items():
        root.set(A + 'layout_width', 'match_parent')
        root.attrib.pop(A + 'layout_weight', None)
        for e in root.iter():
            e.attrib.pop(A + 'contentDescription', None)
            t = tag(e)
            t.pop('action', None)
            e.set(A + 'tag', json.dumps(t, ensure_ascii=False))
        write(folder / (name + '.xml'), root)
    for file in folder.glob('*.xml'):
        tree = ET.parse(file)
        changed = False
        for e in tree.getroot().iter():
            if e.tag.endswith('EditText'):
                continue
            value = e.get(A + 'text', '')
            value = strings.get(value.removeprefix('@string/'), value)
            if value in slots:
                t = tag(e)
                t['binding'] = slots[value]
                e.set(A + 'tag', json.dumps(t, ensure_ascii=False))
                changed = True
        if changed:
            write(file, tree.getroot())
