"""Check exported production coordinates, rounded native tiles and fixed altar anchors."""
from pathlib import Path
import json, math
OUT=Path(__file__).resolve().parent
DATA=json.loads((OUT/'gpt-layout-export-20261114.json').read_text(encoding='utf-8'))
N=DATA['nodes']; R=DATA['routes']
def clip(a,b,box):
    l,t,r,bot=box; dx=b[0]-a[0];dy=b[1]-a[1];u=0;v=1
    for p,q in [(-dx,a[0]-l),(dx,r-a[0]),(-dy,a[1]-t),(dy,bot-a[1])]:
        if abs(p)<1e-9:
            if q<0:return None
        else:
            h=q/p
            if p<0:u=max(u,h)
            else:v=min(v,h)
            if u>v:return None
    return u,v

def inside(g,p):
    a=math.degrees(math.atan2(p[1],p[0]))
    while a<DATA['sectorStart'][g]:a+=360
    return a<=DATA['sectorEnd'][g]+1e-7

def cross(a,b,c):return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])
def intersects(a,b,c,d):return cross(a,b,c)*cross(a,b,d)<-1e-5 and cross(c,d,a)*cross(c,d,b)<-1e-5
byid={n['id']:n for n in N}
backward=sum(math.hypot(n['x'],n['y'])<=math.hypot(byid[p]['x'],byid[p]['y']) for n in N for ps in n['parents'] for p in ps)
sector_nodes=sum(not inside(n['group'],(n['x']+dx,n['y']+dy)) for n in N for dx in [-max(17,n['width']/2+4)/.75,max(17,n['width']/2+4)/.75] for dy in [-18/.75,25/.75])
sector_routes=sum(not inside(r['group'],p) for r in R for p in list(zip(r['x'],r['y']))[(1 if r['parent']==-1 else 0):])
checks=[]
for step in range(26):
    z=.75+step*.05
    centers=[(math.floor(n['x']*z+.5),math.floor(n['y']*z+.5)) for n in N]
    boxes=[(x-max(17,n['width']/2+4),y-18,x+max(17,n['width']/2+4),y+25) for n,(x,y) in zip(N,centers)]
    def overlap(a,b):return a[0]<b[2] and a[2]>b[0] and a[1]<b[3] and a[3]>b[1]
    overlaps=sum(overlap(a,b) for i,a in enumerate(boxes) for b in boxes[:i])
    altar=sum(overlap(a,(-98,-100,98,100)) for a in boxes)
    segments=[]; hits=[]
    for ri,r in enumerate(R):
        pts=[(x*z,y*z) for x,y in zip(r['x'],r['y'])]
        if r['parent']==-1:pts[0]=tuple(DATA['anchors'][r['group']])
        for a,b in zip(pts,pts[1:]):
            # Root stems inside the fixed altar are not visible.
            cut=clip(a,b,(-88,-92,88,92))
            if cut:
                if cut[0]<=0 and cut[1]>=1:continue
                if cut[0]<=0:a=(a[0]+(b[0]-a[0])*cut[1],a[1]+(b[1]-a[1])*cut[1])
            segments.append((ri,a,b))
            for i,box in enumerate(boxes):
                if i in [r['parent'],r['child']]:continue
                if clip(a,b,box):hits.append((ri,N[i]['id']))
    crossings=sum(intersects(a,b,c,d) for i,(ri,a,b) in enumerate(segments) for rj,c,d in segments[:i] if ri!=rj)
    checks.append(dict(zoom=round(z,2),nodeOverlaps=overlaps,altarHits=altar,nodeHits=len(hits),properCrossings=crossings))
    assert not overlaps+altar+len(hits)+crossings,(checks[-1],hits)
assert backward==sector_nodes==sector_routes==0
report=dict(nodes=len(N),learningEdges=sum(len(ps) for n in N for ps in n['parents']),backwardEdges=backward,sectorNodeCornerViolations=sector_nodes,sectorRouteViolations=sector_routes,zooms=checks,geometryClass=DATA['geometryClass'])
(OUT/'gpt-layout-verify-20261114.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print('85 nodes / 48 prerequisites / 26 zoom steps .75..2 / rounded bounds and fixed altar: PASS; reserved sector intrusions=0')
