"""Turn a reference photo into a 3D prop, via Tencent Hunyuan3D.

THIS IS NOT EVIDENCE. Props make the simulator readable. They prove nothing
about the real cabinet. Rule A5, and [ADR 0008](../../docs/adr/0008-cabinet-simulator.md).

Run it from inside Blender, with the BlenderMCP addon loaded and its
SecretId / SecretKey filled in. Credentials are read from the scene and are
never written to this repo.

    submit("C:/path/parcel.jpg")        -> job id
    fetch(job_id, "Parcel_Foo", 0.25)   -> imports and scales to 250 mm

## What the addon gets wrong

The shipped addon targets the *standard* Hunyuan 3D product. If the account is
on **3D Global (Profession)** instead, three things must change, and the errors
are unhelpful about all three:

| Symptom | Cause |
| --- | --- |
| `ResourceUnavailable.NotExist` | action must be `SubmitHunyuanTo3DProJob`, not `...To3DJob` |
| `UnknownParameter: Num` | the Pro API rejects `Num` |
| `UnsupportedRegion` | region must be `ap-singapore`, not `ap-guangzhou` |

Also: `create_hunyuan_job` only registers while `blendermcp_use_hunyuan3d` is
True, so a fresh scene reports the command as unknown. And these are *scene*
properties, so restarting Blender resets them.

Only 3 jobs may run at once; a 4th returns `RequestLimitExceeded.JobNumExceed`.
Keep uploads small - a ~350 KB image drops the socket, ~40 KB is fine.
"""
import base64
import json
import os
import sys

import bpy
import requests

REGION = "ap-singapore"
VERSION = "2023-09-01"
SERVICE = "hunyuan"


def _signer():
    """Borrow the addon's Tencent request signer."""
    mod = sys.modules.get("addon")
    if mod is None:
        raise RuntimeError("BlenderMCP addon not loaded")
    for name in dir(mod):
        obj = getattr(mod, name)
        if isinstance(obj, type) and hasattr(obj, "get_tencent_cloud_sign_headers"):
            return obj.__new__(obj)
    raise RuntimeError("signer not found in addon")


def _call(action, data):
    sc = bpy.context.scene
    sid = sc.blendermcp_hunyuan3d_secret_id
    key = sc.blendermcp_hunyuan3d_secret_key
    if not sid or not key:
        raise RuntimeError("Set SecretId and SecretKey in the BlenderMCP panel")

    head = {"Action": action, "Version": VERSION, "Region": REGION}
    headers, endpoint = _signer().get_tencent_cloud_sign_headers(
        "POST", "/", head, data, SERVICE, REGION, sid, key
    )
    resp = requests.post(endpoint, headers=headers, data=json.dumps(data), timeout=120)
    body = resp.json().get("Response", {})
    err = body.get("Error", {}).get("Code")
    if err:
        raise RuntimeError(f"{err}: {body.get('Error', {}).get('Message')}")
    return body


def submit(image_path):
    """Send one image. Returns a job id."""
    if os.path.getsize(image_path) > 200_000:
        print("WARNING: large image, the socket may drop. Downscale to ~40 KB.")
    with open(image_path, "rb") as fh:
        data = {"ImageBase64": base64.b64encode(fh.read()).decode("ascii")}
    job = _call("SubmitHunyuanTo3DProJob", data)["JobId"]
    print("submitted", os.path.basename(image_path), "->", job)
    return job


def status(job_id):
    """RUN, DONE, or a failure. Returns (status, obj_zip_url)."""
    body = _call("QueryHunyuanTo3DProJob", {"JobId": str(job_id)})
    files = body.get("ResultFile3Ds", [])
    url = next((f["Url"] for f in files if f.get("Type") == "OBJ"), None)
    return body.get("Status"), url


def fetch(job_id, name, longest_mm):
    """Import a finished job and scale it so its longest edge is longest_mm."""
    state, url = status(job_id)
    if state != "DONE":
        print(f"{name}: {state}")
        return None

    _signer().import_generated_asset_hunyuan(name=name, zip_file_url=url)

    ob = bpy.data.objects.get(name)
    if ob is None:
        print(f"{name}: import failed")
        return None
    scale = (longest_mm / 1000.0) / max(ob.dimensions)
    ob.scale = [s * scale for s in ob.scale]
    bpy.context.view_layer.update()
    ob.location = (ob.location.x, ob.location.y, ob.dimensions.z / 2)
    print(f"{name}: {[round(d * 1000) for d in ob.dimensions]} mm, "
          f"{len(ob.data.polygons)} polys")
    return ob
