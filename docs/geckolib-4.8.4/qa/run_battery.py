from qa_runtime import *
import re
r=Rcon()
def c(x): return r.command(x)
def health():
    ans=c(f'data get entity {G} Health')
    m=re.search(r'following entity data: ([0-9.]+)f',ans)
    if not m: raise RuntimeError(ans)
    return float(m.group(1))
results={}
def new_guard(ai=False):
    c('kill @e[type=struggler:midland_guard]')
    c('kill @e[type=minecraft:zombie]')
    c('summon struggler:midland_guard 0 100 5 {Tags:["qa"],NoAI:'+('0b' if ai else '1b')+',Rotation:[180f,0f],PersistenceRequired:1b}')
    time.sleep(.7)
# Idle, sustained block, actual hurt with return to shield, and preview attack.
film('02-poses',12,[(1,f'strugglerguard block {G}'),(1.4,f'strugglerguard block {G}'),(1.8,f'strugglerguard block {G}'),(2.2,f'strugglerguard block {G}'),(2.3,f'damage {G} 2 minecraft:magic'),(2.6,f'strugglerguard block {G}'),(3,f'strugglerguard block {G}'),(3.4,f'strugglerguard block {G}'),(4.5,f'strugglerguard status {G}'),(5,f'strugglerguard attack {G}'),(7,f'strugglerguard attack {G}'),(9,f'strugglerguard defend {G}')])
# True damage measurements, no AI or combat competing with the block window.
new_guard()
before=health(); c(f'strugglerguard block {G}'); c(f'damage {G} 10 minecraft:player_attack by MigrationA');after=health()
results['frontal']={'before':before,'after':after,'damage':before-after,'expected':6.0};assert abs(before-after-6)<.01
c(f'strugglerguard status {G}')
time.sleep(.7); c(f'data merge entity {G} {{Health:24f,Rotation:[0f,0f]}}');before=health();c(f'strugglerguard block {G}');c(f'damage {G} 10 minecraft:player_attack by MigrationA');after=health()
results['rear']={'before':before,'after':after,'damage':before-after,'expected':10.0};assert abs(before-after-10)<.01
c(f'strugglerguard status {G}')
# Real server-targeted melee combat against a passive hostile dummy.
new_guard(True)
c('summon minecraft:zombie 0 100 6.5 {Tags:["dummy"],NoAI:1b,PersistenceRequired:1b,Silent:1b,Attributes:[{Name:"minecraft:generic.max_health",Base:100d}],Health:100f}')
film('03-real-combat',10,[(1,f'strugglerguard status {G}'),(3,f'strugglerguard status {G}'),(6,f'strugglerguard status {G}'),(9,'data get entity @e[type=minecraft:zombie,tag=dummy,limit=1] Health')])
results['real_combat_status']=c(f'strugglerguard status {G}')
# Side views: existing previews drive real navigation; record before/after.
for action in ['walk','run']:
    new_guard(True)
    c('tp MigrationA -4 100 1 -90 0'); c('tp MigrationB -4 100 3 -90 0')
    start=c(f'data get entity {G} Pos')
    film('04-'+action,5,[(.6,f'strugglerguard {action} {G}'),(4.5,f'data get entity {G} Pos')])
    end=c(f'data get entity {G} Pos')
    results[action]={'start':start,'end':end}
# Actual lethal damage, capture begins BEFORE the death rather than after.
new_guard()
c('tp MigrationA 0 100 2 0 0');c('tp MigrationB 2 100 2 25 0')
film('05-death',4,[(1,f'damage {G} 100 minecraft:magic'),(2.8,'execute if entity @e[type=struggler:midland_guard,tag=qa] run say QA_GUARD_STILL_EXISTS')])
results['death_entity_check']=c('execute unless entity @e[type=struggler:midland_guard,tag=qa] run say QA_GUARD_REMOVED')
new_guard();shot('06-final-idle'); c('save-all flush')
(OUT/'runtime-measurements.json').write_text(json.dumps(results,indent=2,ensure_ascii=False)+'\n')
r.close()
print('Bateria concluída',flush=True)
