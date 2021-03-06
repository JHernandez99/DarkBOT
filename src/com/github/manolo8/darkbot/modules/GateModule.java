package com.github.manolo8.darkbot.modules;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.itf.Module;
import com.github.manolo8.darkbot.core.entities.Npc;
import com.github.manolo8.darkbot.core.manager.GuiManager;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.core.objects.Location;

import java.util.List;

import static com.github.manolo8.darkbot.Main.API;
import static java.lang.Math.cos;
import static java.lang.StrictMath.sin;


public class GateModule implements Module {

    private HeroManager hero;
    private GuiManager gui;

    private List<Npc> npcs;

    private long clickDelay;
    private long laserTime;
    private long locker;

    private Npc current;

    private boolean direction;
    private boolean repairing;

    private Location safe;

    public GateModule() {
        safe = new Location(8000, 5500);
    }

    @Override
    public void install(Main main) {
        npcs = main.mapManager.npcs;
        hero = main.hero;
        gui = main.guiManager;
    }

    @Override
    public void tick() {

        if (isGGMap()) {
            Npc closest = closest(hero.location);

            if (closest != null) {

                Location locationHero = hero.location;
                Location locationNpc = closest.location;

                if (hero.health.hpPercent() < 0.5 || repairing) {

                    if (!repairing) repairing = true;
                    else repairing = (hero.health.hpPercent() < 0.8);


                    hero.runMode();
                    move(safe, 500, 1000);

                } else {

                    hero.attackMode();

                    if (closest.health.hpPercent() < 0.25 && System.currentTimeMillis() - locker > 5000) {
                        direction = !direction;
                        locker = System.currentTimeMillis();
                    }

                    if (locationNpc.isMoving() || locationNpc.distance(locationHero) > 800) {
                        move(locationNpc, hero.shipInfo.speed * 0.625, closest.npcInfo.radius + locationNpc.measureSpeed() * 0.625);
                    }

                    if (current == null
                            || current.isInvalid()
                            || locationHero.distance(current) - locationHero.distance(locationNpc) > 150) {
                        current = closest;
                    }

                    tickNormalMode();
                }

            } else {
                hero.move(9000, 6500);
            }
        }
    }

    private boolean isGGMap() {
        int id = hero.map.id;
        return id == 51 || id == 52 || id == 53;
    }

    private void tickNormalMode() {
        if (hero.isTarget(current)) {

            if (!hero.isAttacking(current) && System.currentTimeMillis() - laserTime > 1000) {
                API.keyboardClick('Z');

                laserTime = System.currentTimeMillis();
            }

        } else if (hero.location.distance(current) < 800 && System.currentTimeMillis() - clickDelay > 500) {
            hero.setTarget(current);

            current.clickable.setRadius(800);
            hero.clickCenter();
            current.clickable.setRadius(0);
            API.keyboardClick('Z');

            clickDelay = System.currentTimeMillis();
        }
    }

    private void move(Location location, double distance, double radius) {

        Location current = hero.location;

        double angle = location.angle(current);


        Location target = new Location(
                location.x - cos(angle) * radius,
                location.y - sin(angle) * radius
        );

        if (distance - target.distance(current) > 0) {
            double move = distance / radius;
            angle += direction ? move : -move;
            target.x = location.x - cos(angle) * radius;
            target.y = location.y - sin(angle) * radius;
        }

        hero.move(target);
    }

    private boolean hasNPC() {
        return !npcs.isEmpty();
    }

    private Npc closest(Location location) {
        double distance = -1;
        Npc closest = null;

        for (Npc npc : npcs) {
            double distanceCurrent = location.distance(npc.location);
            if (distance == -1 || distanceCurrent < distance) {
                distance = distanceCurrent;
                closest = npc;
            }
        }

        return closest;
    }
}
