"""Assemble the preview page.

Each script is wrapped in its own IIFE. They are separate <script> blocks but
share one global scope, so a `const` declared in both is a SyntaxError that
kills the *whole* second block - which is exactly what happened: `const NS`
was in both files and the liquid-glass script silently never ran for four
rounds of "why does it still look like blur".

A syntax check that parses each block alone cannot see this. The check at the
bottom concatenates them, which can.
"""
import base64, json, pathlib, subprocess, sys

HERE = pathlib.Path(__file__).parent
HERO = pathlib.Path(r"C:/Users/admin/Project Postman/scripts/cabinet-sim/hero")

b64 = lambda p: base64.b64encode(p.read_bytes()).decode()

css = ((HERE / "screens.css").read_text(encoding="utf-8") + "\n" +
       (HERE / "glass.css").read_text(encoding="utf-8"))
css = (css.replace("__GEISTMONO__", b64(HERE / "geistmono.woff2"))
          .replace("__GEIST__", b64(HERE / "geist.woff2")))
body = (HERE / "body.html").read_text(encoding="utf-8").replace(
    "__PNG__", b64(HERO / "cabinet-1600.png"))

doors = json.load(open(HERO / "cabinet.json"))["doors"]

# GSAP is a UMD bundle: it detects `exports`/`module` and otherwise assigns to
# the global. Wrapping it in the IIFE below would still work, but it is left
# raw so `window.gsap` is unambiguous and a stack trace points at the library
# rather than at an anonymous closure. Inlined at build time - the artifact
# CSP blocks every runtime request, so there is nothing to fetch.
gsap = (HERE / "gsap.min.js").read_text(encoding="utf-8")

scripts = [
    (HERE / "motion.js").read_text(encoding="utf-8"),
    (HERE / "screens.js").read_text(encoding="utf-8").replace("__DOORS__", json.dumps(doors)),
    (HERE / "liquid.js").read_text(encoding="utf-8"),
    (HERE / "orb.js").read_text(encoding="utf-8"),
]

parts = ["<style>\n", css, "\n</style>\n\n", body, "\n",
         "\n<script>\n", gsap, "\n</script>\n"]
for src in scripts:
    parts += ["\n<script>\n(() => {\n", src, "\n})();\n</script>\n"]
out = "".join(parts)

for marker in ("__GEIST__", "__GEISTMONO__", "__PNG__", "__DOORS__"):
    assert marker not in out, marker

(HERE / "vgu-liquid.html").write_text(out, encoding="utf-8")
(HERE / "preview.html").write_text(
    '<!doctype html><html><head><meta charset="utf-8">'
    '<meta name="viewport" content="width=device-width,initial-scale=1">'
    "</head><body>" + out + "</body></html>", encoding="utf-8")

# Parse the scripts TOGETHER, the way the browser will.
check = "\n".join(f"(() => {{\n{s}\n}})();" for s in scripts)
(HERE / ".check.js").write_text(check, encoding="utf-8")
r = subprocess.run(["node", "--check", str(HERE / ".check.js")],
                   capture_output=True, text=True)
if r.returncode:
    print("SCRIPT ERROR:\n" + r.stderr)
    sys.exit(1)

print(f"built {len(out)//1024} KB - scripts parse together")
