from Crypto.Util.Padding import pad
from pwn import *
from tqdm import tqdm

BLOCK_SIZE = 16


def padding_oracle_attack(plaintext: bytes):
    plaintext = pad(plaintext, BLOCK_SIZE)
    plaintext_blocks = reversed(
        [plaintext[i : i + BLOCK_SIZE] for i in range(0, len(plaintext), BLOCK_SIZE)]
    )
    current_block = bytearray(BLOCK_SIZE)
    ciphertext = bytearray(BLOCK_SIZE)

    for plaintext_block in tqdm(plaintext_blocks, desc="Plaintext block"):
        decrypted_block = decrypt_block_with_oracle(oracle, block=current_block)
        current_block = xor(decrypted_block, plaintext_block)
        ciphertext = current_block + ciphertext

    oracle.recvuntil(b"Your choice: ")
    oracle.sendlines([b"2", base64.b64encode(ciphertext).decode()])
    print(oracle.recvline().decode())
    print(oracle.recvline().decode())


def decrypt_block_with_oracle(oracle: remote, block: bytearray):
    decrypted_bytes = bytearray(BLOCK_SIZE)
    attack_bytes = bytearray(BLOCK_SIZE) + block

    for byte_position in (pbar := tqdm(reversed(range(BLOCK_SIZE)), leave=False)):
        for byte in reversed(range(256)):
            pbar.set_description(
                f"{decrypted_bytes=} | {byte_position=}, trying {byte=}"
            )
            attack_bytes[byte_position] = byte
            if not check_oracle_correct_decryption(oracle, attack_bytes[:]):
                continue

            decrypted_bytes[byte_position] = attack_bytes[byte_position] ^ (
                BLOCK_SIZE - byte_position
            )
            for k in range(byte_position, BLOCK_SIZE):
                attack_bytes[k] ^= (BLOCK_SIZE - byte_position) ^ (
                    BLOCK_SIZE - byte_position + 1
                )

            break

    return decrypted_bytes


def check_oracle_correct_decryption(oracle: remote, ciphertext: bytes):
    oracle.recvuntil(b"Your choice: ")
    oracle.sendlines([b"2", base64.b64encode(ciphertext).decode()])
    response = oracle.recvline()
    return b"Error!" not in response


if __name__ == "__main__":
    oracle = process(["python", "aes-cbc.py"])
    # oracle = remote("34.162.82.42", 5000)
    oracle.recvuntil(b"oracle!")
    padding_oracle_attack(
        plaintext=b"I am an authenticated admin, please give me the flag",
    )
