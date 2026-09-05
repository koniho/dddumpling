#!/data/data/com.termux/files/usr/bin/python
import wave, math, statistics, argparse
from array import array

parser = argparse.ArgumentParser(description='Estimate BPM and transcribe a monophonic WAV melody.')
parser.add_argument('wav', help='16-bit PCM WAV input (use ffmpeg to convert m4a first)')
args = parser.parse_args()
path = args.wav
with wave.open(path, 'rb') as w:
    rate = w.getframerate()
    samples = array('h', w.readframes(w.getnframes()))

# Spectral-flux-like onset envelope and tempo autocorrelation. Report the driving
# pulse plus its half-time interpretation, since unaccompanied melodies can imply both.
env_hop = int(rate * .01)
env_win = int(rate * .04)
energy = [math.sqrt(sum(v*v for v in samples[i:i+env_win]) / env_win)
          for i in range(0, len(samples) - env_win, env_hop)]
onset = [0.0] + [max(0.0, energy[i] - sum(energy[max(0, i-10):i]) / min(10, i))
                 for i in range(1, len(energy))]
tempo_scores = []
for bpm in range(70, 241):
    lag = round(60.0 / (bpm * .01))
    tempo_scores.append((sum(onset[i] * onset[i-lag] for i in range(lag, len(onset))), bpm))
# Prefer the energetic upper pulse when it is close to the strongest slow correlation.
best_score = max(score for score, bpm in tempo_scores)
fast = [(score, bpm) for score, bpm in tempo_scores if bpm >= 120 and score >= best_score * .88]
bpm = max(fast)[1] if fast else max(tempo_scores)[1]
print(f"estimated_bpm {bpm} half_time {bpm / 2:.1f}")
win, hop = int(rate * .05), int(rate * .025)
lo, hi = int(rate / 700), int(rate / 75)
frames = []
for start in range(0, len(samples) - win, hop):
    x0 = samples[start:start+win]
    mean = sum(x0) / win
    x = [v - mean for v in x0]
    rms = math.sqrt(sum(v*v for v in x) / win)
    if rms < 260:
        frames.append((start/rate, None, rms, 0)); continue
    best_lag, best = 0, 0.0
    for lag in range(lo, hi + 1):
        a, b = x[:-lag], x[lag:]
        cross = sum(u*v for u,v in zip(a,b))
        den = math.sqrt(sum(u*u for u in a) * sum(v*v for v in b))
        score = cross / den if den else 0
        if score > best: best_lag, best = lag, score
    midi = 69 + 12 * math.log2((rate / best_lag) / 440) if best >= .55 else None
    frames.append((start/rate, midi, rms, best))
notes = []
for i,(t,m,r,c) in enumerate(frames):
    nearby = [frames[j][1] for j in range(max(0,i-2),min(len(frames),i+3)) if frames[j][1] is not None]
    q = round(statistics.median(nearby)) if nearby else None
    if q is not None and not 35 <= q <= 90: q = None
    if notes and q == notes[-1][2] and t - notes[-1][1] <= .08:
        notes[-1] = (notes[-1][0], t+.025, q, max(notes[-1][3],r))
    else: notes.append((t,t+.025,q,r))
notes = [n for n in notes if n[2] is not None and n[1]-n[0] >= .075]
names=['C','C#','D','D#','E','F','F#','G','G#','A','A#','B']
print('duration', len(samples)/rate, 'rate', rate)
for a,b,m,r in notes:
    print(f'{a:5.2f}-{b:5.2f} {names[m%12]}{m//12-1} midi={m:2d} dur={b-a:.2f} rms={r:.0f}')
hist=[0.0]*12
for a,b,m,r in notes: hist[m%12] += b-a
print('pitch classes', sorted(((round(v,2),names[i]) for i,v in enumerate(hist)), reverse=True))
