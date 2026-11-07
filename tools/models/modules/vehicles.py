"""Vehicle item models (the rover itself is an entity model in Java)."""
from sbmodels.common import *  # noqa: F401,F403


def generate():
    generated_item('rover')
    generated_item('sprint_rover')
    generated_item('pressurized_rover')
