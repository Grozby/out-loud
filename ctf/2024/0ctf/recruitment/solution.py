import base64
import demjson3 as json3
import os
import requests
import segno
import subprocess

PROGRAMMING_LANGUAGES = "Python|Java|C|C++|C#|HTML5|CSS3|JavaScript|Ruby|PHP|Swift|Go|Kotlin|Rust|TypeScript|Perl|Dart|Scala|Haskell|Lua|Objective-C|Elixir".lower()

OPTIMIZE = False

IMG_FILE_INPUT = "tmp.png"
IMG_OPTIMIZED = IMG_FILE_INPUT if not OPTIMIZE else "tmp_optimized.png"
IMG_JSON_FILE_INPUT = IMG_OPTIMIZED.replace(".png", ".png.json").replace(
    ".jpg", ".jpg.json"
)


def check_result(decode_result):
    data = json3.decode(decode_result)

    # Confirmation of identity
    # On Mars, we use a novel writing system.
    # Fortunately, these characters are all included in the Unicode system on Earth.
    assert data["name"] == "滌漾火珈豆谷欝寵齦棧"
    assert data["email"] == "ĄąŁęțÿżÀÁćæçöśű@POD-ASIA.RECRUITMENT.MARS.TECH"
    assert data["phone"] == "껪ㇴ㍂舘덃駱縷긭ㇼ蘭㍑糧곀뇂㍅㋗懲궒"

    # Language check
    # Python is important and should not be listed at the last place.
    for lang in data["languages"]:
        assert len(str(lang)) > 0
        assert str(lang).lower() in PROGRAMMING_LANGUAGES
    assert data["languages"][3] != "Python"

    print("BELLA")


def check_qrcode(code_matrix):
    bit_matrix = [
        [int(pixel) for pixel in line] for line in code_matrix.strip("\n").split("\n")
    ]

    # Compliance check
    dimension = len(bit_matrix)
    for line in bit_matrix:
        assert len(line) == dimension

    # Our printer's precision is terrible.
    # So, the QR code version cannot be too high.
    print("DIMENSION", dimension)
    assert dimension <= (7 * 4 + 17)

    # Our printer is experiencing issues with ink flow.
    # Must use the highest error correction level.
    assert bit_matrix[8][0] == 0
    assert bit_matrix[8][1] == 0
    assert bit_matrix[dimension - 1][8] == 0
    assert bit_matrix[dimension - 2][8] == 0


def create_qrcode(str_json: str):
    qr = segno.make(str_json, error="L", boost_error=False, eci=True)
    print(qr.version)
    qr.matrix[8][0] = 0
    qr.matrix[8][1] = 0
    qr.matrix[45 - 1][8] = 0
    qr.matrix[45 - 2][8] = 0
    qr.save(IMG_FILE_INPUT, border=2, scale=2)


if __name__ == "__main__":
    try:
        os.remove(IMG_JSON_FILE_INPUT)
        print("removed")
    except:
        print("Already removed")

    strs_json = [
        ('{"phone":"', 0x4),
        ("껪ㇴ㍂舘덃駱縷긭ㇼ蘭㍑糧곀뇂㍅㋗懲궒", 0x4, "utf-16-be"),  # Best encoding for these chars
        (
            '","email":"ĄąŁęțÿżÀÁćæçöśű@',
            0x4,
            "iso8859_16",
        ),  # Best encoding for these chars
        ("POD-ASIA.RECRUITMENT.MARS.TECH", 0x2),  # Alphanumeric
        ('","languages":"3333","name":"', 0x4),
        ("滌漾火珈豆谷欝寵齦棧", 0x8),  # Kanji
        ('"}', 0x4),  # 28
    ]  #

    print("".join([x[0] for x in strs_json]))
    create_qrcode(strs_json)
    fp = IMG_OPTIMIZED

    cmd = ["java", "-jar", "./src/zxing-test-1.0-SNAPSHOT.jar"] + [fp]

    print(" ".join(cmd))

    p = subprocess.Popen(
        cmd,
        universal_newlines=False,
    )
    p.wait()

    print(p)

    decode_result = json3.decode_file(IMG_JSON_FILE_INPUT, encoding="utf-8")

    if not decode_result["success"]:
        raise Exception("Error")

    code_matrix = str(decode_result["codeMatrix"])
    code_result = str(decode_result["codeResult"])
    check_qrcode(code_matrix)
    check_result(code_result)

    with open(IMG_FILE_INPUT, "rb") as image_file:
        encoded_string = base64.b64encode(image_file.read())
    response = requests.post(
        "http://9jpbmg2rbphygcer.instance.penguin.0ops.sjtu.cn:18080/api/submit",
        data={"file": b"," + encoded_string},
    )
    print(response.text)
