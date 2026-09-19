"""Orthographic geometry preview of the actual Minecraft JSON cuboids (Pillow).

Not an in-game screenshot: lighting is approximate; face UVs use their mean color.
"""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageStat

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'fabric-1.21.1/src/main/resources/assets/mineboard'
OUT = ROOT / 'build/review'
OUT.mkdir(parents=True, exist_ok=True)
FONT = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 23)
SMALL = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 17)

def load_model(name):
    model = json.loads((ASSETS / f'models/{name}.json').read_text())
    if model.get('parent','').startswith('mineboard:'):
        base = load_model(model['parent'].split(':')[1])
        model['textures'] = {**base.get('textures',{}), **model.get('textures',{})}
        model.setdefault('elements',base['elements'])
    return model

def render(draw, name, origin, scale=10):
    model = load_model(f'item/{name}')
    faces = []
    for element in model['elements']:
        x,y,z = element['from']; X,Y,Z = element['to']
        for side, points, shade in [
            ('up', [(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],1),
            ('south', [(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)],.72),
            ('east', [(X,y,z),(X,y,Z),(X,Y,Z),(X,Y,z)],.85)]:
            face = element['faces'].get(side)
            if not face: continue
            texture = model['textures'][face['texture'][1:]]
            tex = texture.split(':')[1]
            image = Image.open(ASSETS / f'textures/{tex}.png').convert('RGB') if texture.startswith('mineboard:') else Image.new('RGB',(16,16),'#ece4cc' if 'white' in tex else '#4c3525')
            u,v,U,V = face.get('uv',[0,0,16,16])
            region = image.crop((int(u*image.width/16), int(v*image.height/16),
                                 max(int(U*image.width/16),int(u*image.width/16)+1),
                                 max(int(V*image.height/16),int(v*image.height/16)+1)))
            color = tuple(round(c*shade) for c in ImageStat.Stat(region).mean)
            projected = [(origin[0]+(a-c)*.82*scale,
                          origin[1]+((a+c-16)*.38-(b-6.8)*.92)*scale) for a,b,c in points]
            faces.append((sum(a+b+c for a,b,c in points)/4, projected, color))
    for _, points, color in sorted(faces, key=lambda f:f[0]):
        draw.polygon(points, fill=color)

image = Image.new('RGB',(1600,800),'#172527')
draw = ImageDraw.Draw(image)
draw.text((45,28),'MINEBOARD / PIECES D’ECHECS',font=FONT,fill='#eee4cb')
draw.text((45,65),'Aperçu des volumes JSON · éclairage simplifié · proportions identiques en jeu',font=SMALL,fill='#a6b6af')
names=['pawn','knight','bishop','rook','queen','king']
labels=['Pion','Cavalier','Fou','Tour','Dame','Roi']
for row,tone in enumerate(['light','dark']):
    for col,(name,label) in enumerate(zip(names,labels)):
        x=145+col*260; y=350+row*340
        draw.rounded_rectangle((x-113,y-225,x+113,y+62),radius=14,fill='#26383a')
        draw.polygon([(x-90,y),(x,y-42),(x+90,y),(x,y+42)],fill='#56655d')
        render(draw,f'chess_{name}_{tone}',(x,y),11)
        draw.text((x-45,y+65),label,font=FONT,fill='#eee4cb')
image.save(OUT/'chess-models.png')
print(OUT/'chess-models.png')

image = Image.new('RGB',(1500,760),'#172527'); draw=ImageDraw.Draw(image)
draw.text((35,24),'MINEBOARD / PLATEAU, CARTES ET PIONS',font=FONT,fill='#eee4cb')
for i,(name,label) in enumerate([('table','Tapis'),('token_light','Pion de dames'),('token_king_dark','Dame'),('wonder','Plaque de cité'),('coin','Pièce')]):
    x=150+i*295
    draw.rounded_rectangle((x-130,95,x+130,345),radius=12,fill='#26383a')
    render(draw,name,(x,220 if name=='table' else 270),10)
    draw.text((x-105,310),label,font=FONT,fill='#eee4cb')
for i,name in enumerate(['card_0','card_19','card_25','back','ages_brown','ages_green','ages_purple']):
    tex=Image.open(ASSETS/f'textures/item/{name}.png').convert('RGB')
    tex.thumbnail((145,210),Image.Resampling.NEAREST)
    tex=tex.resize((128,192),Image.Resampling.NEAREST)
    x=40+i*205;image.paste(tex,(x,415));draw.text((x,627),name,font=SMALL,fill='#eee4cb')
draw.text((35,705),'Volumes JSON et textures sources · éclairage simplifié, hors moteur Minecraft',font=SMALL,fill='#a6b6af')
image.save(OUT/'board-models.png')
