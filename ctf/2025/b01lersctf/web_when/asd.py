from datetime import datetime
from hashlib import sha256

import requests

found = False
timestamp = 0
while not found:
    hash = sha256(str(timestamp).encode()).digest()
    if hash[0] == hash[1] == 255:
        found = True
    else:
        timestamp += 1000

timestamp_str = datetime.utcfromtimestamp(timestamp).isoformat()

print(
    requests.post(
        url="https://when.atreides.b01lersc.tf/gamble",
        headers={
            "date": timestamp_str,
        },
    ).json()
)
