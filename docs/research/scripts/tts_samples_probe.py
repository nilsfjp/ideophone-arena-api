#!/usr/bin/env python3
"""TTS feasibility probe for Ideophone Arena expansion (research session).
Synthesizes <=10 clips/language + phoneme-route tests, transcodes WAV->m4a, ffprobes.
Token read from /tmp/gcp_tok (never written to the planning folder).
NOT the production pipeline - that is emitted in SPEC-tts-synthesis.md. Sample-only.
"""
import json, base64, subprocess, os, time, urllib.request, urllib.error

TOK = open("/tmp/gcp_tok").read().strip()
PROJECT = "enduring-sign-501717-f4"
URL = "https://texttospeech.googleapis.com/v1/text:synthesize"
OUT = "/sessions/fervent-affectionate-knuth/mnt/Ideophone Arena/research/tts-samples"

def dur(p):
    r = subprocess.run(["ffprobe","-v","error","-show_entries","format=duration",
                        "-of","default=nk=1:nw=1",p], capture_output=True, text=True)
    return r.stdout.strip()

def synth(input_obj, voice_name, lang, outname, subdir):
    body = {"input": input_obj,
            "voice": {"languageCode": lang, "name": voice_name},
            "audioConfig": {"audioEncoding": "LINEAR16"}}
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(URL, data=data, headers={
        "Authorization": "Bearer " + TOK,
        "x-goog-user-project": PROJECT,
        "Content-Type": "application/json; charset=utf-8"})
    try:
        r = urllib.request.urlopen(req, timeout=45)
        resp = json.loads(r.read())
        audio = base64.b64decode(resp["audioContent"])
        os.makedirs(f"{OUT}/{subdir}", exist_ok=True)
        wavp = f"{OUT}/{subdir}/{outname}.wav"
        m4ap = f"{OUT}/{subdir}/{outname}.m4a"
        open(wavp, "wb").write(audio)
        subprocess.run(["ffmpeg","-y","-loglevel","error","-i",wavp,
                        "-c:a","aac","-b:a","96k",m4ap], check=True)
        return ("OK", len(audio), dur(wavp), dur(m4ap), os.path.getsize(m4ap))
    except urllib.error.HTTPError as e:
        return ("HTTP"+str(e.code), e.read().decode()[:300], "", "", 0)
    except Exception as e:
        return ("ERR", str(e)[:300], "", "", 0)

rows = []  # (subdir, outname, form, gloss, lang, voice, input_kind, result...)

# ---------- Japanese: 1 word x 4 voice types (comparison) + 5 singles ----------
JA_VOICES = ["ja-JP-Chirp3-HD-Aoede","ja-JP-Neural2-B","ja-JP-Wavenet-B","ja-JP-Standard-A"]
for vn in JA_VOICES:
    tag = vn.replace("ja-JP-","")
    res = synth({"text":"いらいら"}, vn, "ja-JP", f"ja-iraira-{tag}", "ja-JP")
    rows.append(("ja-JP", f"ja-iraira-{tag}", "いらいら", "irritated/nervous", "ja-JP", vn, "text", *res))
JA_SINGLES = [("うきうき","ukiuki","cheerful/buoyant"),
              ("いそいそ","isoiso","eagerly/briskly"),
              ("あっさり","assari","plain/light (taste)"),
              ("あたふた","atafuta","in a flustered hurry"),
              ("あくせく","akuseku","toiling restlessly")]
for kana, roma, gloss in JA_SINGLES:
    vn = "ja-JP-Chirp3-HD-Aoede"
    res = synth({"text":kana}, vn, "ja-JP", f"ja-{roma}-Chirp3HD-Aoede", "ja-JP")
    rows.append(("ja-JP", f"ja-{roma}-Chirp3HD-Aoede", kana, gloss, "ja-JP", vn, "text", *res))

# ---------- Korean: Hangul ideophones (hand-sourced; dataset ships romanization) ----------
# 1 word x 3 voice types + 5 singles, all in Hangul
KO_CMP = ("반짝반짝","banjjakbanjjak","sparkling/twinkling")
for vn in ["ko-KR-Chirp3-HD-Aoede","ko-KR-Neural2-A","ko-KR-Wavenet-A"]:
    tag = vn.replace("ko-KR-","")
    res = synth({"text":KO_CMP[0]}, vn, "ko-KR", f"ko-{KO_CMP[1]}-{tag}", "ko-KR")
    rows.append(("ko-KR", f"ko-{KO_CMP[1]}-{tag}", KO_CMP[0], KO_CMP[2], "ko-KR", vn, "text-hangul", *res))
KO_SINGLES = [("두근두근","dugeundugeun","heart pounding"),
              ("데굴데굴","deguldegul","rolling over and over"),
              ("폭신폭신","poksinpoksin","soft and fluffy"),
              ("미끌미끌","mikkeulmikkeul","slippery/slimy"),  # dataset: mikkŭlmikkŭl
              ("보들보들","bodeulbodeul","soft to the touch")]  # dataset: podŭlbodŭl
for hangul, roma, gloss in KO_SINGLES:
    vn = "ko-KR-Chirp3-HD-Aoede"
    res = synth({"text":hangul}, vn, "ko-KR", f"ko-{roma}-Chirp3HD-Aoede", "ko-KR")
    rows.append(("ko-KR", f"ko-{roma}-Chirp3HD-Aoede", hangul, gloss, "ko-KR", vn, "text-hangul", *res))

# ---------- Phoneme route: Ewe/Siwu (NO native voice) via <phoneme> on a foreign voice ----------
def ssml_phoneme(ipa, fallback):
    return {"ssml": f'<speak><phoneme alphabet="ipa" ph="{ipa}">{fallback}</phoneme></speak>'}
PHON = [
  # (dataset form, language, ipa_ph, fallback_text, voice, note)
  ("gulugulu","Siwu","gulugulu","gulugulu","it-IT-Wavenet-A","clean phonemes (g,u,l)"),
  ("fiɛfiɛ","Siwu","fiɛfiɛ","fiefie","it-IT-Wavenet-A","has ɛ"),
  ("bɔyibɔyi","Ewe","bɔjibɔji","boyiboyi","es-ES-Wavenet-B","ɔ + glide j"),
  ("kpraɖii","Ewe","kpraɖiː","kpradii","en-US-Wavenet-F","HARD: kp labial-velar + ɖ retroflex"),
]
for form, lang, ipa, fb, vn, note in PHON:
    vlang = "-".join(vn.split("-")[:2])
    res = synth(ssml_phoneme(ipa, fb), vn, vlang, f"phon-{lang}-{fb}-{vn.split('-')[0]}{vn.split('-')[1]}", "phoneme-route")
    rows.append(("phoneme-route", f"phon-{lang}-{fb}", form, note, vlang, vn, "ssml-phoneme", *res))
    time.sleep(0.2)

# ---------- report ----------
print(f"{'status':7s} {'lang':6s} {'voice':26s} {'kind':12s} {'wav_s':6s} {'m4a_s':6s} form")
ok = 0
for r in rows:
    subdir,outname,form,gloss,lang,voice,kind,status,info,wavd,m4ad,sz = r
    if status == "OK": ok += 1
    ws = (wavd[:5] if wavd else "").ljust(6)
    ms = (m4ad[:5] if m4ad else "").ljust(6)
    print(f"{status:7s} {lang:6s} {voice:26s} {kind:12s} {ws} {ms} {form}  {('' if status=='OK' else '<<'+str(info)[:120])}")
print(f"\nOK {ok}/{len(rows)}")

# ---------- listening sheet ----------
ls = ["# TTS sample listening sheet - for Nils's voice-quality ratification\n",
      "_Generated by `research/scripts/tts_samples_probe.py` this session. m4a files under `research/tts-samples/{lang}/`._",
      "_Korean forms are hand-sourced Hangul (the triangulating dataset ships romanization only - a real finding, see manifest)._\n",
      "| # | file (.m4a) | form (verbatim) | gloss | lang | voice | input | m4a dur (s) |",
      "|---|---|---|---|---|---|---|---|"]
i = 0
for r in rows:
    subdir,outname,form,gloss,lang,voice,kind,status,info,wavd,m4ad,sz = r
    if status != "OK": continue
    i += 1
    ls.append(f"| {i} | `{subdir}/{outname}.m4a` | {form} | {gloss} | {lang} | {voice} | {kind} | {m4ad[:5]} |")
ls.append("\n## Failed / phoneme-route notes\n")
for r in rows:
    subdir,outname,form,gloss,lang,voice,kind,status,info,wavd,m4ad,sz = r
    if status == "OK": continue
    ls.append(f"- **{status}** - {form} via {voice} ({gloss}): `{str(info)[:200]}`")
open(f"{OUT}/listening-sheet.md","w").write("\n".join(ls))
print("wrote listening-sheet.md")
