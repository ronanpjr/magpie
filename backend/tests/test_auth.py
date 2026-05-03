from __future__ import annotations


def test_register_valid_data(client):
    response = client.post(
        "/auth/register",
        json={
            "username": "bob",
            "email": "bob@example.com",
            "password": "password123",
            "display_name": "Bob",
        },
    )
    assert response.status_code == 201
    body = response.json()
    assert body["access_token"]
    assert body["user"]["username"] == "bob"


def test_register_duplicate_username(client):
    payload = {
        "username": "bob",
        "email": "bob@example.com",
        "password": "password123",
        "display_name": "Bob",
    }
    assert client.post("/auth/register", json=payload).status_code == 201
    duplicate = client.post(
        "/auth/register",
        json={**payload, "email": "other@example.com"},
    )
    assert duplicate.status_code == 409


def test_login_success_and_failure(client):
    client.post(
        "/auth/register",
        json={
            "username": "bob",
            "email": "bob@example.com",
            "password": "password123",
            "display_name": "Bob",
        },
    )
    ok = client.post("/auth/login", json={"username": "bob", "password": "password123"})
    assert ok.status_code == 200
    assert ok.json()["access_token"]

    bad = client.post("/auth/login", json={"username": "bob", "password": "wrong"})
    assert bad.status_code == 401


def test_protected_route_requires_token(client):
    response = client.get("/users/me")
    assert response.status_code == 401
