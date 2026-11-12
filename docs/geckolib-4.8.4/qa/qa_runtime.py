"""Local-only RCON and genuine X11 captures for the GeckoLib migration QA."""
import socket,struct,time,json,subprocess,sys
from pathlib import Path
from PIL import ImageGrab
ROOT=Path('/home/ubuntu/gecko-migration')
OUT=ROOT/'evidence'
G='@e[type=struggler:midland_guard,tag=qa,limit=1]'
class Rcon:
    def __init__(self):
        self.s=socket.create_connection(('127.0.0.1',25576),timeout=10)
        self.s.settimeout(10); self.id=0
        self.send(3,'migration-qa-local-only')
    def receive(self,n):
        data=b''
        while len(data)<n:
            chunk=self.s.recv(n-len(data))
            if not chunk: raise ConnectionError('RCON closed')
            data+=chunk
        return data
    def send(self,typ,text):
        self.id+=1
        b=struct.pack('<ii',self.id,typ)+text.encode()+b'\0\0'
        self.s.sendall(struct.pack('<i',len(b))+b)
        while True:
            size=struct.unpack('<i',self.receive(4))[0]
            resp=self.receive(size)
            rid,rtyp=struct.unpack('<ii',resp[:8])
            if rid==-1: raise PermissionError('RCON auth failed')
            if rid==self.id and (typ!=3 or rtyp==2):
                return resp[8:-2].decode(errors='replace')
    def command(self,text):
        ans=self.send(2,text)
        print(text+' -> '+ans,flush=True)
        with (OUT/'rcon-transcript.log').open('a') as f:
            f.write(time.strftime('%Y-%m-%dT%H:%M:%S')+' '+text+' -> '+ans+'\n')
        return ans
    def close(self): self.s.close()
def shot(name):
    for label,display in [('a',':106'),('b',':107')]:
        ImageGrab.grab(xdisplay=display).save(OUT/(name+'-'+label+'.png'))
def film(name,duration,commands):
    processes=[]
    for label,display in [('a',':106'),('b',':107')]:
        p=subprocess.Popen(['ffmpeg','-y','-hide_banner','-loglevel','error','-f','x11grab','-framerate','20','-video_size','1280x720','-i',display,'-t',str(duration),'-c:v','libx264','-preset','ultrafast','-crf','24','-pix_fmt','yuv420p',str(OUT/(name+'-'+label+'.mp4'))],stdout=subprocess.DEVNULL,stderr=(OUT/(name+'-'+label+'-ffmpeg.log')).open('w'))
        processes.append(p)
    r=Rcon(); started=time.monotonic()
    for delay,cmd in commands:
        time.sleep(max(0,started+delay-time.monotonic())); r.command(cmd)
    r.close()
    for p in processes:
        if p.wait(timeout=duration+15)!=0: raise RuntimeError('capture failed')
if __name__=='__main__':
    if sys.argv[1]=='cmd':
        r=Rcon()
        for c in sys.argv[2:]: r.command(c)
        r.close()
    elif sys.argv[1]=='shot': shot(sys.argv[2])
