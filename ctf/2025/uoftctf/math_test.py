from pwnlib.tubes.remote import remote

conn = remote("34.66.235.106", 5000)
asd = conn.recvuntil(b"Good Luck\n")

for i in range(1000):
    question = conn.recvuntil(b"Answer: ")
    question = next(
        x for x in question.decode().split("\n") if "Question" in x
    ).replace("Question: ", "")
    answer = eval(question)
    conn.sendline(f"{answer}")

print(conn.recvline())
print(conn.recvline())
