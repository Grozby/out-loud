import base64
import os
import secrets
import subprocess as sp
import demjson3 as json
from flask import Flask, render_template, request
from io import BytesIO
from PIL import Image

PROGRAMMING_LANGUAGES = "Python|Java|C|C++|C#|HTML5|CSS3|JavaScript|Ruby|PHP|Swift|Go|Kotlin|Rust|TypeScript|Perl|Dart|Scala|Haskell|Lua|Objective-C|Elixir".lower()

app = Flask(__name__)
app.secret_key = secrets.token_hex(32)

UPLOAD_FOLDER = 'uploads'
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

FLAG = os.environ.get("FLAG", "flag{test}")

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/apply')
def apply():
    return render_template('apply.html')

@app.route('/api/submit', methods=['POST'])
def submit():
    if 'file' not in request.form:
        return 'No file part', 400
    file_data = request.form['file']
    if file_data == '':
        return 'No selected file', 400
    if (len(file_data) > 20000):
        return 'Too big', 400
    file_data = file_data.split(",")[1]
    
    image = Image.open(BytesIO(base64.b64decode(file_data)))
    img_filename = secrets.token_hex(16) + '.png'
    img_filepath = os.path.join(app.config['UPLOAD_FOLDER'], img_filename)
    image.save(img_filepath)
    decode(img_filepath)

    result_filepath = img_filepath + '.json'
    decode_result = json.decode_file(result_filepath, encoding='utf-8')
    if (not decode_result['success']):
        raise Exception("Error")
    
    code_matrix = str(decode_result['codeMatrix'])
    code_result = str(decode_result['codeResult'])
    check_qrcode(code_matrix)
    check_result(code_result)
    
    return FLAG, 200

def check_result(decode_result):
    data = json.decode(decode_result)

    # Confirmation of identity
    # On Mars, we use a novel writing system.
    # Fortunately, these characters are all included in the Unicode system on Earth.
    assert data['name'] == "滌漾火珈豆谷欝寵齦棧"
    assert data['email'] == "ĄąŁęțÿżÀÁćæçöśű@POD-ASIA.RECRUITMENT.MARS.TECH"
    assert data['phone'] == "껪ㇴ㍂舘덃駱縷긭ㇼ蘭㍑糧곀뇂㍅㋗懲궒"

    # Language check
    # Python is important and should not be listed at the last place.
    for lang in data['languages']:
        assert len(str(lang)) > 0
        assert str(lang).lower() in PROGRAMMING_LANGUAGES
    assert data['languages'][3] != "Python"

def check_qrcode(code_matrix):
    bit_matrix = [
        [
            int(pixel) for pixel in line
        ]
        for line in code_matrix.strip('\n').split('\n')
    ]

    # Compliance check
    dimension = len(bit_matrix)
    for line in bit_matrix:
        assert len(line) == dimension
    
    # Our printer's precision is terrible.
    # So, the QR code version cannot be too high.
    assert dimension <= (7 * 4 + 17)

    # Our printer is experiencing issues with ink flow.
    # Must use the highest error correction level.
    assert bit_matrix[8][0] == 0
    assert bit_matrix[8][1] == 0
    assert bit_matrix[dimension - 1][8] == 0
    assert bit_matrix[dimension - 2][8] == 0

def decode(filepath):
    try:
        cmd = ['java', '-jar', 'zxing-test-1.0-SNAPSHOT.jar'] + [filepath]
        p = sp.Popen(cmd, stdout=sp.PIPE, stderr=sp.STDOUT, universal_newlines=False)
        p.wait()
        if (p.returncode != 0):
            raise Exception("Error")
    except OSError as e:
        raise Exception("Could not execute specified Java binary") from e

if __name__ == '__main__':
    app.run(debug=False, port=3049)


