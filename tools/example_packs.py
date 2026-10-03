"""Create editable resource/data pack examples without changing default assets."""
from pathlib import Path
import json, shutil, zipfile
from build import ROOT

def write(root, path, value):
    file = root / path
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding='utf-8')

def main():
    out = ROOT / 'examples'
    assets = ROOT / 'src/main/resources/assets/teleport_loading'
    resource = out / 'loading-resources'
    data = out / 'loading-data'
    write(resource, 'pack.mcmeta', {'pack': {'pack_format': 34, 'description': 'Teleport Loading: editable PNG, OGG, TTF and tips'}})
    write(data, 'pack.mcmeta', {'pack': {'pack_format': 48, 'description': 'Teleport Loading: server tips and dimension mappings'}})
    icon = {'texture': 'loading_example:textures/gui/dimensions.png', 'width': 1254, 'height': 1254,
            'u': 0, 'v': 627, 'region_width': 627, 'region_height': 627, 'base': 100}
    definition = {
        'dimensions': {'example:moon': icon},
        'elements': {'texture': 'loading_example:textures/gui/elements.png', 'width': 2172, 'height': 724, 'base': 80},
        'font': 'loading_example:ui', 'sound': 'loading_example:arrival',
        'tips': [{'title': '资源包小贴士', 'body': '此处可以替换成自己的内容。', 'translate': False},
                 {'title': 'loading.teleport_loading.tip.0.title', 'body': 'loading.teleport_loading.tip.0.body', 'translate': True}]}
    write(resource, 'assets/loading_example/loading_screen/screen.json', definition)
    write(data, 'data/loading_example/loading_screen/screen.json', {
        'dimensions': {'example:moon': icon},
        'tips': [{'title': '数据包小贴士', 'body': '服务器可通过 /reload 更新文案和维度图标映射。'}]})
    providers = [{'type': 'reference', 'id': 'minecraft:default'}]
    if (assets / 'font/wenhei.ttf').is_file():
        providers.insert(0, {'type': 'ttf', 'file': 'loading_example:ui.ttf', 'shift': [0, 0], 'size': 9, 'oversample': 4})
    write(resource, 'assets/loading_example/font/ui.json', {'providers': providers})
    sounds = [{'name': 'loading_example:arrival', 'preload': True}] if (assets / 'sounds/loading.ogg').is_file() else []
    write(resource, 'assets/loading_example/sounds.json', {'arrival': {'sounds': sounds}})
    for source, destination in [('textures/gui/elements.png', 'textures/gui/elements.png'),
                                ('textures/gui/dimensions.png', 'textures/gui/dimensions.png'),
                                ('textures/gui/elements.png.mcmeta', 'textures/gui/elements.png.mcmeta'),
                                ('textures/gui/dimensions.png.mcmeta', 'textures/gui/dimensions.png.mcmeta'),
                                ('font/wenhei.ttf', 'font/ui.ttf'), ('sounds/loading.ogg', 'sounds/arrival.ogg')]:
        target = resource / 'assets/loading_example' / destination
        if not (assets / source).is_file():
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(assets / source, target)
    for pack in [resource, data]:
        with zipfile.ZipFile(out / (pack.name + '.zip'), 'w', zipfile.ZIP_DEFLATED) as archive:
            for file in sorted(pack.rglob('*')):
                if file.is_file(): archive.write(file, file.relative_to(pack).as_posix())
    print('Created editable resource/data pack directories and ZIPs in ' + str(out))

if __name__ == '__main__': main()
