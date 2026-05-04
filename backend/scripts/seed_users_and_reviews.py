"""
Seeding script to create users with reviews and follower relationships.
Includes a default test user with a known password for testing purposes.
"""

from __future__ import annotations

import random
from datetime import datetime, timedelta

from sqlmodel import Session, select

from app.database import create_db_and_tables, engine
from app.models.catalog import Album, Track
from app.models.follow import Follow
from app.models.review import Review, ReviewTarget
from app.models.user import User
from app.services.auth_service import get_password_hash


# Default test user - use these credentials for testing
DEFAULT_TEST_USER = {
    "username": "testuser",
    "email": "testuser@magpie.dev",
    "password": "testpassword123",
    "display_name": "Test User",
}


def seed_users() -> list[User]:
    """Create test users including the default test user."""
    users: list[User] = []

    # Create the default test user
    test_user = User(
        username=DEFAULT_TEST_USER["username"],
        email=DEFAULT_TEST_USER["email"],
        hashed_password=get_password_hash(DEFAULT_TEST_USER["password"]),
        display_name=DEFAULT_TEST_USER["display_name"],
        bio="Default test user for development and testing",
    )
    users.append(test_user)

    # Create additional test users
    user_count = 5
    for idx in range(1, user_count + 1):
        user = User(
            username=f"user{idx}",
            email=f"user{idx}@magpie.dev",
            hashed_password=get_password_hash(f"password{idx}"),
            display_name=f"User {idx}",
            bio=f"Bio for user {idx}",
        )
        users.append(user)

    return users


def seed_follows(session: Session, users: list[User]) -> None:
    """Create follower relationships between users."""
    if len(users) < 2:
        return

    # Create bidirectional follows (testuser follows others, others follow testuser)
    test_user = users[0]

    for user in users[1:]:
        # Test user follows others
        existing = session.exec(
            select(Follow).where(
                Follow.follower_id == test_user.id,
                Follow.followed_id == user.id,
            )
        ).first()
        if existing is None:
            session.add(Follow(follower_id=test_user.id or 0, followed_id=user.id or 0))

        # Others follow test user
        existing = session.exec(
            select(Follow).where(
                Follow.follower_id == user.id,
                Follow.followed_id == test_user.id,
            )
        ).first()
        if existing is None:
            session.add(Follow(follower_id=user.id or 0, followed_id=test_user.id or 0))

    # Create random follows between other users
    for user in users[1:]:
        followable_users = [u for u in users[1:] if u.id != user.id]
        num_follows = random.randint(1, min(2, len(followable_users)))

        for followee in random.sample(followable_users, k=num_follows):
            existing = session.exec(
                select(Follow).where(
                    Follow.follower_id == user.id,
                    Follow.followed_id == followee.id,
                )
            ).first()
            if existing is None:
                session.add(Follow(follower_id=user.id or 0, followed_id=followee.id or 0))


def seed_reviews(session: Session, users: list[User]) -> None:
    """Create reviews for albums and tracks by users."""
    albums = session.exec(select(Album)).all()
    tracks = session.exec(select(Track)).all()

    if not albums and not tracks:
        print("Warning: No albums or tracks found in database. Skipping reviews.")
        return

    review_bodies = [
        "Amazing album! A must-listen.",
        "Really enjoyed this one. Great production quality.",
        "Not my cup of tea, but well-made.",
        "Iconic work. Timeless classics.",
        "Outstanding composition and performance.",
        "Fresh and innovative sound.",
        "A solid collection of tracks.",
        "Exceeded my expectations.",
        "Great for background listening.",
        "Masterpiece. Every track is gold.",
    ]

    # Create album reviews from all users
    if albums:
        for user in users:
            for album in random.sample(albums, k=min(random.randint(1, 3), len(albums))):
                existing = session.exec(
                    select(Review).where(
                        Review.author_id == user.id,
                        Review.target_type == ReviewTarget.album,
                        Review.target_id == album.id,
                    )
                ).first()
                if existing is None:
                    session.add(
                        Review(
                            author_id=user.id or 0,
                            target_type=ReviewTarget.album,
                            target_id=album.id or 0,
                            rating=round(random.uniform(2.5, 5.0), 1),
                            body=random.choice(review_bodies),
                            created_at=datetime.utcnow()
                            - timedelta(days=random.randint(1, 30)),
                        )
                    )

    # Create track reviews from all users
    if tracks:
        for user in users:
            for track in random.sample(tracks, k=min(random.randint(0, 2), len(tracks))):
                existing = session.exec(
                    select(Review).where(
                        Review.author_id == user.id,
                        Review.target_type == ReviewTarget.track,
                        Review.target_id == track.id,
                    )
                ).first()
                if existing is None:
                    session.add(
                        Review(
                            author_id=user.id or 0,
                            target_type=ReviewTarget.track,
                            target_id=track.id or 0,
                            rating=round(random.uniform(2.5, 5.0), 1),
                            body=random.choice(review_bodies),
                            created_at=datetime.utcnow()
                            - timedelta(days=random.randint(1, 30)),
                        )
                    )


def seed() -> None:
    """Main seeding function. Creates database and populates with test data."""
    create_db_and_tables()

    with Session(engine) as session:
        # Check if reviews already exist - if so, skip seeding
        existing_reviews = session.exec(select(Review)).all()
        if existing_reviews:
            print("Reviews already exist in database. Skipping seeding.")
            print(f"\nDefault test user credentials:")
            print(f"  Username: {DEFAULT_TEST_USER['username']}")
            print(f"  Password: {DEFAULT_TEST_USER['password']}")
            print(f"  Email: {DEFAULT_TEST_USER['email']}")
            return

        # Check if users already exist
        existing_users = session.exec(select(User)).all()
        if existing_users:
            print("Users already exist in database. Skipping user creation.")
            users = existing_users
        else:
            print("Creating test users...")
            users = seed_users()
            for user in users:
                session.add(user)
            session.flush()
            print(f"Created {len(users)} users")

        # Seed follows
        print("Creating follower relationships...")
        seed_follows(session, users)

        # Seed reviews
        print("Creating reviews...")
        seed_reviews(session, users)

        session.commit()
        print("Seeding completed successfully!")
        print(f"\nDefault test user credentials:")
        print(f"  Username: {DEFAULT_TEST_USER['username']}")
        print(f"  Password: {DEFAULT_TEST_USER['password']}")
        print(f"  Email: {DEFAULT_TEST_USER['email']}")


if __name__ == "__main__":
    seed()
