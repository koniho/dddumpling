'use strict';
window.ddd = window.ddd || {};
(() => {
  const colors=new Map();
  const color=n=>{let c=colors.get(n);if(c)return c;c=`rgba(${n>>>16&255},${n>>>8&255},${n&255},${(n>>>24)/255})`;if(colors.size>4096)colors.clear();colors.set(n,c);return c;};
  const stroke=(c,n,w,path)=>{c.strokeStyle=color(n);c.lineWidth=w;c.lineJoin='round';c.lineCap='round';if(path)c.stroke(path);else c.stroke();};
  // Java passes its actual coordinate buffer. Reuse static paths; scratch/body arrays may mutate.
  const paths=new WeakMap();
  function pathFor(p,closed){
    let entry=paths.get(p),same=entry?.closed===closed && entry.points.length===p.length;
    if(same)for(let i=0;i<p.length;i++)if(entry.points[i]!==p[i]){same=false;break;}
    if(same)return entry.path;
    const path=new Path2D();path.moveTo(p[0],p[1]);for(let i=2;i<p.length;i+=2)path.lineTo(p[i],p[i+1]);if(closed)path.closePath();
    if(!entry || entry.points.length!==p.length)entry={points:new Float32Array(p.length)};
    entry.points.set(p);entry.path=path;entry.closed=closed;paths.set(p,entry);return path;
  }
  const contour=(c,p)=>{if(p.length<2)return;c.moveTo(p[0],p[1]);for(let i=2;i<p.length;i+=2)c.lineTo(p[i],p[i+1]);c.closePath();};
  ddd.paint={color,contour,
    poly(c,p,n,w,closed){if(p.length<2 || n>>>24===0)return;const path=pathFor(p,closed);if(w>0)stroke(c,n,w,path);else{c.fillStyle=color(n);c.fill(path);}},
    ellipse(c,x,y,rx,ry,n,w){if(rx<=0||ry<=0)return;c.beginPath();c.ellipse(x,y,rx,ry,0,0,Math.PI*2);if(w>0)stroke(c,n,w);else{c.fillStyle=color(n);c.fill();}},
    arc(c,x,y,rx,ry,a,s,n,w){if(rx<=0||ry<=0||w<=0)return;c.beginPath();c.ellipse(x,y,rx,ry,0,a*Math.PI/180,(a+s)*Math.PI/180,s<0);stroke(c,n,w);},
    line(c,x,y,a,b,n,w){if(w<=0)return;c.beginPath();c.moveTo(x,y);c.lineTo(a,b);stroke(c,n,w);}
  };
  const entries=new Map(),limit=64*1024*1024;let bytes=0,serial=0;
  function get(key){const v=entries.get(key);if(!v)return null;entries.delete(key);entries.set(key,v);return v;}
  function remove(key){const v=entries.get(key);if(v){bytes-=v.width*v.height*4;entries.delete(key);v.close?.();}}
  function put(key,img){const old=entries.get(key);if(old===img)return;remove(key);entries.set(key,img);bytes+=img.width*img.height*4;img.cacheID??=++serial;while(bytes>limit&&entries.size>1){const [k,v]=entries.entries().next().value;remove(k);}}
  // Recolour cached artwork once per quantized palette, on all browsers (no Canvas.filter dependency).
  function filtered(img,m){
    if(m.every((v,i)=>Math.abs(v-([0,6,12,18].includes(i)?1:0))<.0001))return img;
    const q=m.map((v,i)=>Math.round(v*(i%5===4?1/8:16))/(i%5===4?1/8:16));
    const key=`palette:${img.cacheID}:${q.join(',')}`;let out=get(key);if(out)return out;
    out=document.createElement('canvas');out.width=img.width;out.height=img.height;const ctx=out.getContext('2d',{willReadFrequently:true});ctx.drawImage(img,0,0);
    const pixels=ctx.getImageData(0,0,out.width,out.height),d=pixels.data;
    for(let i=0;i<d.length;i+=4){const r=d[i],g=d[i+1],b=d[i+2];for(let j=0;j<3;j++)d[i+j]=q[j*5]*r+q[j*5+1]*g+q[j*5+2]*b+q[j*5+4];}
    ctx.putImageData(pixels,0,0);put(key,out);return out;
  }
  function matrixFor(mono,shade,water){const weights=[54/256,183/256,19/256],m=new Array(20).fill(0);for(let r=0;r<3;r++){for(let c=0;c<3;c++)m[r*5+c]=(weights[c]*mono+(r===c?1-mono:0))*shade;m[r*5+4]=((water>>>(16-r*8))&255)*(1-shade);}m[18]=1;return m;}
  function triangle(c,img,s,d){
    const [x0,y0,x1,y1,x2,y2]=s,[u0,v0,u1,v1,u2,v2]=d;
    const det=(x1-x0)*(y2-y0)-(x2-x0)*(y1-y0);if(!det)return;
    const a=((u1-u0)*(y2-y0)-(u2-u0)*(y1-y0))/det,b=((v1-v0)*(y2-y0)-(v2-v0)*(y1-y0))/det;
    const cc=((u2-u0)*(x1-x0)-(u1-u0)*(x2-x0))/det,dd=((v2-v0)*(x1-x0)-(v1-v0)*(x2-x0))/det;
    c.save();c.beginPath();c.moveTo(u0,v0);c.lineTo(u1,v1);c.lineTo(u2,v2);c.closePath();c.clip();c.transform(a,b,cc,dd,u0-a*x0-cc*y0,v0-b*x0-dd*y0);c.drawImage(img,0,0);c.restore();
  }
  ddd.cache={get,put,remove,
    // Cached art is recolored on the CPU. Avoid repeated GPU readback during palette changes.
    create(w,h){const canvas=document.createElement('canvas');canvas.width=w;canvas.height=h;canvas.getContext('2d',{willReadFrequently:true});return canvas;},
    pixels(c,pixels,w,h){const d=c.createImageData(w,h);for(let i=0;i<pixels.length;i++){const p=pixels[i];d.data[i*4]=p>>>16&255;d.data[i*4+1]=p>>>8&255;d.data[i*4+2]=p&255;d.data[i*4+3]=p>>>24;}c.putImageData(d,0,0);},get bytes(){return bytes;},get count(){return entries.size;},
    draw(c,img,x,y,w,h,alpha,mono,shade,water){if(!img||w<=0||h<=0||alpha<=0)return;const tex=mono||shade!==1?filtered(img,matrixFor(mono,shade,water)):img;c.save();c.globalAlpha*=Math.min(255,alpha)/255;c.drawImage(tex,x,y,w,h);c.restore();},
    matrix(c,img,x,y,l,t,w,h,angle,alpha,m){if(!img)return;c.save();c.globalAlpha*=alpha/255;c.translate(x,y);c.rotate(angle);c.drawImage(filtered(img,Array.from(m)),l,t,w,h);c.restore();},
    band(c,img,xy,r,a,mono){if(!img)return;const tex=mono?filtered(img,matrixFor(mono,1,0)):img;c.save();c.globalAlpha*=a/255;for(let i=2;i<xy.length;i+=2){const x0=xy[i-2],y0=xy[i-1],x1=xy[i],y1=xy[i+1];triangle(c,tex,[0,0,tex.width,0,0,tex.height],[x0,y0-r,x1,y1-r,x0,y0+r]);triangle(c,tex,[tex.width,0,tex.width,tex.height,0,tex.height],[x1,y1-r,x1,y1+r,x0,y0+r]);}c.restore();},
    mesh(c,img,x,y,w,h,offsets,left,alpha){if(!img)return;c.save();c.globalAlpha*=alpha/255;const n=offsets.length-1;for(let i=0;i<n;i++){
      const y0=y+h*i/n,y1=y+h*(i+1)/n,s0=img.height*i/n,s1=img.height*(i+1)/n;
      const l0=x-(left?0:offsets[i]),l1=x-(left?0:offsets[i+1]),r0=x+w+(left?offsets[i]:0),r1=x+w+(left?offsets[i+1]:0);
      triangle(c,img,[0,s0,img.width,s0,0,s1],[l0,y0,r0,y0,l1,y1]);triangle(c,img,[img.width,s0,img.width,s1,0,s1],[r0,y0,r1,y1,l1,y1]);
    }c.restore();}
  };
})();
