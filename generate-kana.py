"""Flatten only the kana used by the study pages. FontTools 4.60.1; Klee One, SIL OFL 1.1.
Input font SHA-256 is pinned. Outputs are committed; runtime needs no font or FontTools.
Usage: python generate-kana.py /path/to/KleeOne-Regular.ttf
"""
import sys,json,hashlib,math
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.pens.basePen import BasePen
root=Path(__file__).resolve().parent
font_path=Path(sys.argv[1]);font_bytes=font_path.read_bytes()
assert hashlib.sha256(font_bytes).hexdigest()=='bf4063f030cc2ae6adf0a11424a1888e5c0eb4438f1f6d02f52294af868e9b3a'
font=TTFont(font_path);glyphs=font.getGlyphSet();cmap=font.getBestCmap();em=font['head'].unitsPerEm
class Flat(BasePen):
 def __init__(self):super().__init__(glyphs);self.contours=[];self.points=[]
 def _moveTo(self,p):self.points=[p]
 def _lineTo(self,p):self.points.append(p)
 def _closePath(self):
  if len(self.points)>2:self.contours.append(self.points)
  self.points=[]
 def _endPath(self):self._closePath()
 def _curveToOne(self,p1,p2,p3):
  p0=self.points[-1];n=max(8,int(sum(math.dist(a,b) for a,b in [(p0,p1),(p1,p2),(p2,p3)])/em*90))
  for j in range(1,n+1):
   t=j/n;u=1-t;self.points.append(tuple(u**3*p0[k]+3*u*u*t*p1[k]+3*u*t*t*p2[k]+t**3*p3[k] for k in (0,1)))
 def _qCurveToOne(self,p1,p2):
  p0=self.points[-1];n=max(8,int((math.dist(p0,p1)+math.dist(p1,p2))/em*90))
  for j in range(1,n+1):
   t=j/n;u=1-t;self.points.append(tuple(u*u*p0[k]+2*u*t*p1[k]+t*t*p2[k] for k in (0,1)))
data={}
for char in json.loads((root/'kana-curriculum.json').read_text(encoding='utf-8'))['characters']:
 assert ord(char) in cmap,char
 pen=Flat();glyphs[cmap[ord(char)]].draw(pen);contours=[]
 for contour in pen.contours:
  points=[]
  for x,y in contour:
   q=(round(x/em*1000),round(880-y/em*1000));assert all(0<=v<=1000 for v in q),(char,q)
   if not points or q!=points[-1]:points.append(q)
  if len(points)>2:contours.append(' '.join(f'{x},{y}' for x,y in points))
 data[char]=';'.join(contours);assert data[char]
out={'source':'https://github.com/google/fonts/tree/main/ofl/kleeone','fontSha256':hashlib.sha256(font_bytes).hexdigest(),'license':'SIL Open Font License 1.1; Copyright 2020 The Klee Project Authors','glyphs':data}
(root/'kana-glyphs.json').write_text(json.dumps(out,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
print(f'{len(data)} portable kana contours; {sum(len(s) for s in data.values())} characters of path data.')
