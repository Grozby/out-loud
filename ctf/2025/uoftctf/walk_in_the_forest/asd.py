import pickle
import string
from collections import defaultdict

if __name__ == "__main__":
    with open("model.pkl", "rb") as f:
        model = pickle.load(f)

    output = defaultdict(list)
    for c in string.printable:
        bits = list(map(int, f"{ord(c):b}".zfill(8)))
        output_model = model.predict([bits])[0]
        probabilities = model.predict_proba([bits])[0]
        if any(p == 1 for p in probabilities):
            print(model.predict_proba([bits]))
            print(f"{c=} | {output_model=}")
            output[output_model].append(c)

    print(output)
