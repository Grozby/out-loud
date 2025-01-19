# Script used to find suitable encodings for the QR Code
import encodings


def find_encodings_for_character(char):
    supported_encodings = []
    # Get all available encodings
    all_encodings = set(encodings.aliases.aliases.values())
    for encoding in sorted(all_encodings):  # Sort for consistent output
        try:
            char.encode(encoding)
            supported_encodings.append(encoding)
        except (UnicodeEncodeError, LookupError):
            pass
    return supported_encodings


# Example usage
encodings_matched = []
for c in "滌漾火珈豆谷欝寵齦棧":
    encodings_supporting_character = find_encodings_for_character(c)
    encodings_matched.append(set(encodings_supporting_character))

intersection = encodings_matched[0]
for e in encodings_matched[1:]:
    intersection &= e

print("Supported Encodings:")
print("\n".join(sorted(intersection)))


print("=" * 80)
for k in {
    # Codecs name (``codecs.lookup(some-charset).name``) -> ECI designator
    "cp437": 1,
    "iso8859-1": 3,
    "iso8859-2": 4,
    "iso8859-3": 5,
    "iso8859-4": 6,
    "iso8859-5": 7,
    "iso8859-6": 8,
    "iso8859-7": 9,
    "iso8859-8": 10,
    "iso8859-9": 11,
    "iso8859-10": 12,
    "iso8859-11": 13,
    "iso8859-13": 15,
    "iso8859-14": 16,
    "iso8859-15": 17,
    "iso8859-16": 18,
    "shift_jis": 20,
    "cp1250": 21,
    "cp1251": 22,
    "cp1252": 23,
    "cp1256": 24,
    "utf-16-be": 25,
    "utf-8": 26,
    "ascii": 27,
    "big5": 28,
    "gb18030": 29,
    "gbk": 29,  # GBK is treated as GB-18030
    "euc_kr": 30,
}.keys():
    try:
        "껪ㇴ㍂舘덃駱縷긭ㇼ蘭㍑糧곀뇂㍅㋗懲궒".encode(k)
        print(k)
    except:
        pass
