"""Small structure snapshots shown (turning, in 3D) in the field guide: assets/spacebreaker/guide/structures/."""
import os

from template import Template

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..'))
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'spacebreaker', 'guide', 'structures')


def launch_pad() -> Template:
    """The complete launch site the console checks (rocketry/pad/LaunchPad): a 5x5 pad, the 3x3 flame trench in the
    middle, a 7-high service tower just outside the ring with the crew access arm and the fuel umbilical facing the pad,
    and the launch control console a little way off."""
    t = Template(9, 9, 7)
    cx, cz = 3, 3
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            inner = abs(dx) <= 1 and abs(dz) <= 1
            t.set(cx + dx, 0, cz + dz, 'spacebreaker:flame_trench' if inner else 'spacebreaker:launch_pad')
    tx = cx + 3                       # tower on the east ring
    for y in range(0, 7):
        t.set(tx, y, cz, 'spacebreaker:service_tower')
    t.set(tx - 1, 5, cz, 'spacebreaker:crew_access_arm', facing='west', extended='true')
    t.set(tx - 1, 2, cz, 'spacebreaker:fuel_umbilical', facing='west')
    t.set(cx + 5, 0, cz + 3, 'spacebreaker:launch_control_console', facing='west')
    return t


def generate(out):
    n, p = launch_pad().save(os.path.join(OUT, 'launch_pad.nbt'))
    print(f'  guide/launch_pad: {n} blocks, {p} states')
